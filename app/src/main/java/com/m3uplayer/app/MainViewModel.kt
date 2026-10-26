package com.m3uplayer.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.m3uplayer.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val repo = PlaylistRepo(app)

    var user by mutableStateOf(store.load()); private set
    var library by mutableStateOf<ParsedPlaylist?>(null); private set
    var loading by mutableStateOf(false); private set
    var loadMessage by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null)

    var tab by mutableStateOf(Tab.HOME)
    var openSeries by mutableStateOf<String?>(null)
    var playing by mutableStateOf<PlayRequest?>(null); private set
    var addingPlaylist by mutableStateOf(false)

    val showAdd: Boolean get() = addingPlaylist || user.playlists.isEmpty()

    init {
        if (user.favLists.isEmpty()) {
            update { it.copy(favLists = listOf(FavList(DEFAULT_LIST, "المفضلة", emptyList()))) }
        }
        activePlaylist()?.let { entry -> viewModelScope.launch { load(entry, force = false, isNew = false) } }
    }

    private fun update(f: (UserData) -> UserData) {
        user = f(user)
        store.save(user)
    }

    // ---------------------------------------------------------------- قوائم التشغيل

    fun activePlaylist(): PlaylistEntry? =
        user.playlists.firstOrNull { it.id == user.activeId } ?: user.playlists.firstOrNull()

    fun addPlaylist(name: String, url: String) {
        val u = url.trim()
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) {
            error = "أدخل رابطاً صحيحاً يبدأ بـ http:// أو https://"
            return
        }
        val entry = PlaylistEntry(
            id = UUID.randomUUID().toString().take(8),
            name = name.trim().ifEmpty { "قائمة ${user.playlists.size + 1}" },
            url = u,
        )
        viewModelScope.launch {
            if (load(entry, force = true, isNew = true)) {
                update { it.copy(playlists = it.playlists + entry, activeId = entry.id) }
                addingPlaylist = false
                tab = Tab.HOME
            }
        }
    }

    fun selectPlaylist(id: String) {
        val entry = user.playlists.firstOrNull { it.id == id } ?: return
        update { it.copy(activeId = id) }
        viewModelScope.launch { load(entry, force = false, isNew = false) }
    }

    fun refreshActive() {
        val entry = activePlaylist() ?: return
        viewModelScope.launch { load(entry, force = true, isNew = false) }
    }

    fun deletePlaylist(id: String) {
        val wasActive = activePlaylist()?.id == id
        repo.file(id).delete()
        update { u ->
            val rest = u.playlists.filter { it.id != id }
            u.copy(playlists = rest, activeId = if (wasActive) rest.firstOrNull()?.id else u.activeId)
        }
        if (wasActive) {
            library = null
            activePlaylist()?.let { entry -> viewModelScope.launch { load(entry, force = false, isNew = false) } }
        }
    }

    private suspend fun load(entry: PlaylistEntry, force: Boolean, isNew: Boolean): Boolean {
        loading = true
        error = null
        val file = repo.file(entry.id)
        return try {
            if (force || !file.exists()) {
                loadMessage = "جاري تحميل القائمة…"
                repo.download(entry.url, file)
            }
            loadMessage = "جاري قراءة القنوات والأفلام والمسلسلات…"
            val parsed = repo.parse(file)
            if (parsed.isEmpty) throw IOException("لم يتم العثور على أي عناصر في القائمة")
            library = parsed
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isNew) file.delete()
            error = friendly(e)
            false
        } finally {
            loading = false
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is UnknownHostException -> "تعذر الوصول إلى الخادم. تحقق من الرابط ومن اتصال الإنترنت."
        is SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم."
        is IllegalArgumentException -> "الرابط غير صالح."
        else -> e.message ?: "تعذر تحميل القائمة."
    }

    // ---------------------------------------------------------------- التشغيل والمتابعة

    fun fraction(p: Progress): Float =
        if (p.duration > 0) (p.position.toFloat() / p.duration).coerceIn(0f, 1f) else 0f

    fun progressOf(id: String): Float = user.progress[id]?.let { fraction(it) } ?: 0f

    fun play(item: Item) {
        val queue = if (item.kind == Kind.SERIES) {
            library?.seriesMap?.get(item.seriesName)?.episodes.orEmpty()
        } else emptyList()
        playing = PlayRequest(item, queue)
    }

    fun closePlayer() {
        playing = null
    }

    fun onProgress(item: Item, position: Long, duration: Long) {
        if (item.kind == Kind.LIVE || duration <= 0 || position < 0) return
        val p = Progress(item.id, item.name, item.logo, position, duration, item.seriesName, System.currentTimeMillis())
        update { it.copy(progress = it.progress + (item.id to p)) }
    }

    fun resumePosition(item: Item): Long {
        val p = user.progress[item.id] ?: return 0L
        return if (fraction(p) < 0.95f) (p.position - 2_000).coerceAtLeast(0) else 0L
    }

    fun clearProgress() = update { it.copy(progress = emptyMap()) }

    /** أفلام وحلقات لم تكتمل، وللمسلسل المكتمل تظهر الحلقة التالية. */
    fun continueWatching(): List<ContinueEntry> {
        val lib = library ?: return emptyList()
        val out = ArrayList<ContinueEntry>()
        val seen = HashSet<String>()
        for (p in user.progress.values.sortedByDescending { it.updated }) {
            val item = lib.byId[p.itemId] ?: continue
            val f = fraction(p)
            when (item.kind) {
                Kind.MOVIE -> if (f < 0.95f && p.position >= 5_000) out += ContinueEntry(item, f)
                Kind.SERIES -> {
                    val sn = item.seriesName ?: continue
                    if (!seen.add(sn)) continue
                    if (f < 0.95f) {
                        if (p.position >= 5_000) out += ContinueEntry(item, f)
                    } else {
                        val eps = lib.seriesMap[sn]?.episodes ?: continue
                        val next = eps.getOrNull(eps.indexOfFirst { it.id == item.id } + 1)
                        if (next != null) out += ContinueEntry(next, 0f)
                    }
                }
                Kind.LIVE -> {}
            }
            if (out.size >= 20) break
        }
        return out
    }

    /** الحلقة التي يجب متابعتها لمسلسل معين. */
    fun continueTarget(s: Series): Item {
        val last = s.episodes
            .mapNotNull { e -> user.progress[e.id]?.let { e to it } }
            .maxByOrNull { it.second.updated }
            ?: return s.episodes.first()
        val (episode, progress) = last
        return if (fraction(progress) >= 0.95f) {
            s.episodes.getOrNull(s.episodes.indexOf(episode) + 1) ?: episode
        } else episode
    }

    // ---------------------------------------------------------------- المفضلة

    fun seriesKey(name: String) = "s:$name"

    fun isFav(key: String) = user.favLists.any { key in it.keys }

    /** إضافة/إزالة سريعة إلى القائمة الافتراضية. */
    fun toggleFav(key: String) = update { u ->
        if (u.favLists.any { key in it.keys }) {
            u.copy(favLists = u.favLists.map { it.copy(keys = it.keys - key) })
        } else {
            val lists = if (u.favLists.any { it.id == DEFAULT_LIST }) u.favLists
            else listOf(FavList(DEFAULT_LIST, "المفضلة", emptyList())) + u.favLists
            u.copy(favLists = lists.map { if (it.id == DEFAULT_LIST) it.copy(keys = it.keys + key) else it })
        }
    }

    fun createList(name: String): String {
        val id = UUID.randomUUID().toString().take(8)
        update { it.copy(favLists = it.favLists + FavList(id, name.trim(), emptyList())) }
        return id
    }

    fun renameList(id: String, name: String) =
        update { u -> u.copy(favLists = u.favLists.map { if (it.id == id) it.copy(name = name.trim()) else it }) }

    fun deleteList(id: String) {
        if (id == DEFAULT_LIST) return
        update { u -> u.copy(favLists = u.favLists.filter { it.id != id }) }
    }

    fun addToList(listId: String, keys: List<String>) =
        update { u -> u.copy(favLists = u.favLists.map { if (it.id == listId) it.copy(keys = (it.keys + keys).distinct()) else it }) }

    fun removeFromList(listId: String, key: String) =
        update { u -> u.copy(favLists = u.favLists.map { if (it.id == listId) it.copy(keys = it.keys - key) else it }) }

    fun moveBetween(fromId: String, toId: String, key: String) = update { u ->
        u.copy(favLists = u.favLists.map {
            when (it.id) {
                fromId -> it.copy(keys = it.keys - key)
                toId -> it.copy(keys = (it.keys + key).distinct())
                else -> it
            }
        })
    }

    fun shift(listId: String, key: String, delta: Int) = update { u ->
        u.copy(favLists = u.favLists.map { l ->
            if (l.id != listId) return@map l
            val i = l.keys.indexOf(key)
            val j = i + delta
            if (i < 0 || j !in l.keys.indices) return@map l
            val m = l.keys.toMutableList()
            m.removeAt(i)
            m.add(j, key)
            l.copy(keys = m)
        })
    }

    // ---------------------------------------------------------------- تثبيت القنوات في الرئيسية

    fun isPinned(itemId: String) = user.pinned.any { it.itemId == itemId }

    fun pin(itemId: String, label: String) = update { u ->
        u.copy(pinned = u.pinned.filter { it.itemId != itemId } + Pinned(itemId, label.trim()))
    }

    fun unpin(itemId: String) = update { u -> u.copy(pinned = u.pinned.filter { it.itemId != itemId }) }

    // ---------------------------------------------------------------- مساعدات للواجهة

    fun tileFor(key: String): Tile? {
        val lib = library ?: return null
        if (key.startsWith("s:")) {
            val s = lib.seriesMap[key.removePrefix("s:")] ?: return null
            return Tile(key, s.name, s.logo, "مسلسل", Kind.SERIES)
        }
        val item = lib.byId[key] ?: return null
        val label = when (item.kind) {
            Kind.LIVE -> "قناة"
            Kind.MOVIE -> "فيلم"
            Kind.SERIES -> "حلقة"
        }
        return Tile(key, item.name, item.logo, label, item.kind)
    }

    fun open(key: String) {
        val lib = library ?: return
        if (key.startsWith("s:")) {
            openSeries = key.removePrefix("s:")
        } else {
            lib.byId[key]?.let { play(it) }
        }
    }
}
