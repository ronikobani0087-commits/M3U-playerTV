package com.m3uplayer.app.data

import java.io.File
import java.security.MessageDigest

/**
 * يقرأ ملف M3U ويصنّف كل عنصر:
 *  - مسارات Xtream (/series/ و /movie/) تُعرف مباشرة.
 *  - ثم امتداد الملف (m3u8/ts = بث حي، mp4/mkv... = فيلم أو حلقة).
 *  - ثم كلمات اسم الباقة (مسلسلات، أفلام، series, movies ...).
 */
object M3uParser {

    private val attrRegex = Regex("([\\w-]+)=\"([^\"]*)\"")

    private val sxe = Regex("""^(.*?)[\s._\-\[(]*[Ss](\d{1,3})\s*[._\-]?\s*[Ee](\d{1,4})""")
    private val nxm = Regex("""^(.*?)[\s._\-]*(?<!\d)(\d{1,2})[xX](\d{1,3})\b""")
    private val arSeason = Regex("""^(.*?)[\s._\-]*(?:الموسم|موسم)\s*(\d{1,3})\s*[-–]?\s*(?:الحلقة|حلقة)\s*(\d{1,4})""")
    private val epOnly = Regex("""^(.*?)[\s._\-]*(?:الحلقة|حلقة|(?<![A-Za-z])[Ee][Pp]?(?:isode)?)\s*[._\-]?\s*(\d{1,4})""")

    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "mpg", "mpeg", "webm", "m4v")
    private val liveExt = setOf("m3u8", "ts", "m3u")
    private val seriesWords = listOf("series", "serie", "مسلسل", "dizi", "episode", "حلقات")
    private val movieWords = listOf("movie", "film", "vod", "فيلم", "افلام", "أفلام", "cinema", "سينما")

    fun parse(file: File): ParsedPlaylist {
        val live = ArrayList<Item>()
        val movies = ArrayList<Item>()
        val episodes = LinkedHashMap<String, MutableList<Item>>()
        val displayNames = HashMap<String, String>()
        val seen = HashSet<String>()

        var title = ""
        var logo: String? = null
        var group = ""
        var extGroup = ""
        var pending = false

        file.bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF", ignoreCase = true)) {
                    val lastQuote = line.lastIndexOf('"')
                    val comma = line.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                    val attrs = HashMap<String, String>()
                    val attrPart = if (comma >= 0) line.substring(0, comma) else line
                    attrRegex.findAll(attrPart).forEach { attrs[it.groupValues[1].lowercase()] = it.groupValues[2] }
                    val rawTitle = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    title = rawTitle.ifEmpty { attrs["tvg-name"].orEmpty() }.ifEmpty { "بدون اسم" }
                    logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                    group = attrs["group-title"].orEmpty().trim()
                    pending = true
                } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                    extGroup = line.substringAfter(':').trim()
                } else if (line.startsWith("#")) {
                    continue
                } else {
                    if (!pending) {
                        title = line.substringAfterLast('/').ifEmpty { line }
                        logo = null
                        group = ""
                    }
                    val g = group.ifBlank { extGroup }.ifBlank { "غير مصنف" }
                    val id = md5(line)
                    if (seen.add(id)) {
                        when (classify(line, g, title)) {
                            Kind.LIVE -> live += Item(id, title, line, logo, g, Kind.LIVE)
                            Kind.MOVIE -> movies += Item(id, title, line, logo, g, Kind.MOVIE)
                            Kind.SERIES -> {
                                val (parsedName, season, ep) = parseEpisode(title)
                                val seriesName = parsedName.ifBlank { g }
                                val key = seriesName.lowercase()
                                displayNames.putIfAbsent(key, seriesName)
                                episodes.getOrPut(key) { ArrayList() } +=
                                    Item(id, title, line, logo, g, Kind.SERIES, displayNames[key], season, ep)
                            }
                        }
                    }
                    pending = false
                    extGroup = ""
                }
            }
        }

        val series = episodes.map { (key, list) ->
            val sorted = list.sortedWith(compareBy<Item>({ it.season }, { it.episode }))
            Series(
                name = displayNames[key] ?: key,
                logo = sorted.firstNotNullOfOrNull { it.logo },
                group = sorted.first().group,
                episodes = sorted,
            )
        }
        return ParsedPlaylist(live, movies, series)
    }

    private fun classify(url: String, group: String, name: String): Kind {
        val u = url.lowercase()
        if ("/series/" in u) return Kind.SERIES
        if ("/movie/" in u) return Kind.MOVIE
        val ext = extOf(u)
        val g = group.lowercase()
        val looksLikeEpisode = sxe.containsMatchIn(name) || arSeason.containsMatchIn(name)
        val groupSaysSeries = seriesWords.any { g.contains(it) }
        return when {
            ext in liveExt -> Kind.LIVE
            ext in videoExt -> if (looksLikeEpisode || groupSaysSeries) Kind.SERIES else Kind.MOVIE
            groupSaysSeries -> Kind.SERIES
            movieWords.any { g.contains(it) } -> Kind.MOVIE
            else -> Kind.LIVE
        }
    }

    private fun parseEpisode(name: String): Triple<String, Int, Int> {
        for (r in listOf(sxe, arSeason, nxm)) {
            val m = r.find(name) ?: continue
            return Triple(clean(m.groupValues[1]), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        epOnly.find(name)?.let { m ->
            return Triple(clean(m.groupValues[1]), 1, m.groupValues[2].toInt())
        }
        return Triple(clean(name), 1, 0)
    }

    private fun clean(s: String) = s.trim { it.isWhitespace() || it in "-_.|:[]()" }

    private fun extOf(url: String): String {
        val last = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        return if ('.' in last) last.substringAfterLast('.').lowercase() else ""
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)
}
