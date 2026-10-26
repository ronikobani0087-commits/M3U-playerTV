package com.m3uplayer.app.data

enum class Tab { HOME, LIVE, MOVIES, SERIES, FAVORITES, SETTINGS }

enum class Kind { LIVE, MOVIE, SERIES }

const val DEFAULT_LIST = "default"

/** عنصر واحد من ملف M3U: قناة، فيلم أو حلقة. */
data class Item(
    val id: String,
    val name: String,
    val url: String,
    val logo: String?,
    val group: String,
    val kind: Kind,
    val seriesName: String? = null,
    val season: Int = 0,
    val episode: Int = 0,
)

data class Series(
    val name: String,
    val logo: String?,
    val group: String,
    val episodes: List<Item>,
) {
    val seasons: List<Int> get() = episodes.map { it.season }.distinct().sorted()
}

/** نتيجة قراءة ملف M3U بعد تصنيفه إلى بث حي وأفلام ومسلسلات. */
class ParsedPlaylist(
    val live: List<Item>,
    val movies: List<Item>,
    val series: List<Series>,
) {
    val isEmpty: Boolean get() = live.isEmpty() && movies.isEmpty() && series.isEmpty()

    val liveGroups: List<String> = live.map { it.group }.distinct()
    val movieGroups: List<String> = movies.map { it.group }.distinct()
    val seriesGroups: List<String> = series.map { it.group }.distinct()

    val liveGroupCounts: Map<String, Int> = live.groupingBy { it.group }.eachCount()
    val movieGroupCounts: Map<String, Int> = movies.groupingBy { it.group }.eachCount()
    val seriesGroupCounts: Map<String, Int> = series.groupingBy { it.group }.eachCount()

    val seriesMap: Map<String, Series> = series.associateBy { it.name }

    val byId: Map<String, Item> = HashMap<String, Item>(live.size + movies.size + 16).also { m ->
        live.forEach { m[it.id] = it }
        movies.forEach { m[it.id] = it }
        series.forEach { s -> s.episodes.forEach { m[it.id] = it } }
    }
}

// ---------- بيانات المستخدم (تُحفظ على الجهاز) ----------

data class PlaylistEntry(val id: String, val name: String, val url: String)

/** قائمة مفضلة. المفاتيح: معرّف القناة/الفيلم، أو "s:اسم المسلسل" للمسلسلات. */
data class FavList(val id: String, val name: String, val keys: List<String>)

/** قناة مثبتة في الشاشة الرئيسية (مثلاً قناة مباراة مهمة). */
data class Pinned(val itemId: String, val label: String)

data class Progress(
    val itemId: String,
    val title: String,
    val logo: String?,
    val position: Long,
    val duration: Long,
    val seriesName: String?,
    val updated: Long,
)

data class UserData(
    val playlists: List<PlaylistEntry> = emptyList(),
    val activeId: String? = null,
    val favLists: List<FavList> = emptyList(),
    val pinned: List<Pinned> = emptyList(),
    val progress: Map<String, Progress> = emptyMap(),
)

// ---------- نماذج للواجهة ----------

data class PlayRequest(val item: Item, val queue: List<Item>)

data class Tile(val key: String, val title: String, val logo: String?, val subtitle: String, val kind: Kind)

data class ContinueEntry(val item: Item, val fraction: Float)
