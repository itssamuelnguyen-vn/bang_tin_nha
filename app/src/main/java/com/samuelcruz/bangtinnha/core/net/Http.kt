package com.samuelcruz.bangtinnha.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * One shared HTTP client (like a Faraday connection in Rails): clear User-Agent, timeouts, no automatic retries.
 * The last raw response of each source is kept in cacheDir/raw for debugging (never in the database).
 */
class Http(private val cacheDir: File) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /** GETs [url] and returns the body as text; throws on network errors and non-2xx answers. */
    suspend fun get(url: String, sourceId: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} từ ${request.url.host}")
                keepRaw(sourceId, url, body)
                body
            }
        }

    /** Overwrites one debug file per source and drops anything older than a week. */
    private fun keepRaw(sourceId: String, url: String, body: String) {
        runCatching {
            val dir = File(cacheDir, "raw").apply { mkdirs() }
            File(dir, "$sourceId.txt").writeText("$url\n\n${body.take(MAX_RAW_CHARS)}")
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            dir.listFiles()?.filter { it.lastModified() < weekAgo }?.forEach { it.delete() }
        }
    }

    companion object {
        const val USER_AGENT = "BangTinNha/1.0 (Android; family dashboard; +https://github.com/itssamuelnguyen-vn/bang_tin_nha)"
        private const val MAX_RAW_CHARS = 200_000
    }
}
