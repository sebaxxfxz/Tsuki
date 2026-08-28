package com.example.tsuki.lyrics.providers

import android.util.Log
import com.example.tsuki.lyrics.LyricsProvider
import com.example.tsuki.network.INNERTUBE_CLIENT_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import java.util.concurrent.TimeUnit

object YouTubeSubtitleLyricsProvider : LyricsProvider {

    override val name: String = "YouTube Subtitles"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext Result.failure(Exception("Empty videoId"))


        try {
            val paramBytes = byteArrayOf(0x0a, id.length.toByte()) + id.toByteArray(Charsets.UTF_8)
            val paramsBase64 = android.util.Base64.encodeToString(paramBytes, android.util.Base64.NO_WRAP)

            val bodyJson = """{"context":{"client":{"clientName":"WEB","clientVersion":"$INNERTUBE_CLIENT_VERSION","hl":"es","gl":"US"}},"params":"$paramsBase64"}"""
            val reqBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/get_transcript?prettyPrint=false")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0")
                .post(reqBody)
                .build()

            val respStr = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use ""
                resp.body?.string() ?: ""
            }
            if (respStr.isNotEmpty()) {
                val root = org.json.JSONObject(respStr)
                val actions = root.optJSONArray("actions")
                val action = actions?.optJSONObject(0)
                val updateAction = action?.optJSONObject("updateEngagementPanelAction")
                val content = updateAction?.optJSONObject("content")
                val transcriptRenderer = content?.optJSONObject("transcriptRenderer")
                val body = transcriptRenderer?.optJSONObject("body")
                val transcriptBodyRenderer = body?.optJSONObject("transcriptBodyRenderer")
                val cueGroups = transcriptBodyRenderer?.optJSONArray("cueGroups")

                if (cueGroups != null && cueGroups.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until cueGroups.length()) {
                        val group = cueGroups.getJSONObject(i)
                        val groupRenderer = group.optJSONObject("transcriptCueGroupRenderer")
                        val cues = groupRenderer?.optJSONArray("cues") ?: continue
                        for (j in 0 until cues.length()) {
                            val cue = cues.optJSONObject(j)?.optJSONObject("transcriptCueRenderer") ?: continue
                            val startMs = cue.optLong("startOffsetMs", 0L)
                            val text = cue.optJSONObject("cue")?.optString("simpleText", "")
                                ?.replace("♪", "")?.trim() ?: ""

                            if (text.isNotBlank()) {
                                val min = startMs / 60000
                                val sec = (startMs / 1000) % 60
                                val cs = (startMs % 1000) / 10
                                sb.append(String.format("[%02d:%02d.%02d] %s\n", min, sec, cs, text))
                            }
                        }
                    }
                    val lrc = sb.toString().trim()
                    if (lrc.isNotBlank()) {
                        return@withContext Result.success(lrc)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("YouTubeSubtitleLyrics", "get_transcript error: ${e.message}")
        }


        try {
            val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$id")
            val subtitleStreams = info.subtitles ?: emptyList()
            if (subtitleStreams.isEmpty()) return@withContext Result.failure(Exception("No YouTube subtitles available"))


            val preferredSub = subtitleStreams.firstOrNull { it.languageTag.startsWith("es", ignoreCase = true) }
                ?: subtitleStreams.firstOrNull { it.languageTag.startsWith("en", ignoreCase = true) }
                ?: subtitleStreams.first()

            val subUrl = preferredSub.content ?: preferredSub.getUrl() ?: return@withContext Result.failure(Exception("No subtitle URL"))

            val req = Request.Builder().url(subUrl).build()
            val body = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.string()
            } ?: return@withContext Result.failure(Exception("Empty subtitles body"))
            val lrc = convertVttOrSrtToLrc(body)
            if (lrc.isNotBlank()) {
                Result.success(lrc)
            } else {
                Result.failure(Exception("Failed to convert subtitles to LRC"))
            }
        } catch (e: Exception) {
            Log.w("YouTubeSubtitleLyrics", "Subtitles fetch failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun convertVttOrSrtToLrc(raw: String): String {
        val lines = raw.lines()
        val lrcBuilder = StringBuilder()
        val timeRegex = Regex("""(\d{2}:)?(\d{2}):(\d{2})[.,](\d{2,3})""")

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val match = timeRegex.find(line)
                if (match != null) {
                    val mm = match.groupValues[2].ifEmpty { "00" }
                    val ss = match.groupValues[3]
                    val xx = match.groupValues[4].take(2).padEnd(2, '0')
                    val timeTag = "[$mm:$ss.$xx]"


                    i++
                    val textList = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() && !lines[i].contains("-->") && !lines[i].all { it.isDigit() }) {
                        val cleaned = lines[i].replace(Regex("<[^>]*>"), "").trim()
                        if (cleaned.isNotEmpty()) textList.add(cleaned)
                        i++
                    }
                    if (textList.isNotEmpty()) {
                        lrcBuilder.append(timeTag).append(" ").append(textList.joinToString(" ")).append("\n")
                    }
                    continue
                }
            }
            i++
        }
        return lrcBuilder.toString().trim()
    }
}
