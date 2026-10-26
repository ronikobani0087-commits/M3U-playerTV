package com.m3uplayer.app.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

const val USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** حفظ بيانات المستخدم (القوائم، المفضلة، التقدم) على الجهاز. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("m3u_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): UserData = try {
        prefs.getString("data", null)?.let { gson.fromJson(it, UserData::class.java) } ?: UserData()
    } catch (e: Exception) {
        UserData()
    }

    fun save(data: UserData) {
        prefs.edit().putString("data", gson.toJson(data)).apply()
    }
}

/** تحميل ملف M3U وقراءته. */
class PlaylistRepo(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun file(id: String) = File(context.filesDir, "playlist_$id.m3u")

    suspend fun download(url: String, target: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("رد الخادم: ${response.code}")
            val body = response.body ?: throw IOException("رد فارغ من الخادم")
            val tmp = File(target.parentFile, target.name + ".tmp")
            body.byteStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
            val valid = tmp.bufferedReader().use { r ->
                r.lineSequence().take(30).any { it.contains("#EXTM3U") || it.contains("#EXTINF") }
            }
            if (!valid) {
                tmp.delete()
                throw IOException("الرابط لا يحتوي على قائمة M3U صالحة")
            }
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("تعذر حفظ القائمة")
        }
    }

    suspend fun parse(file: File): ParsedPlaylist = withContext(Dispatchers.Default) { M3uParser.parse(file) }
}
