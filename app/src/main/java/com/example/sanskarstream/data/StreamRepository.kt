package com.example.sanskarstream.data

import android.content.Context
import com.example.sanskarstream.Config
import com.example.sanskarstream.domain.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class StreamRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "streams_cache.json")

    /** API se latest list. Success par response cache me save hota hai. */
    suspend fun fetch(): Result<List<Stream>> = withContext(Dispatchers.IO) {
        runCatching {
            if (Config.API_URL.isBlank()) return@runCatching SampleStreams.list
            val body = httpGet(Config.API_URL)
            val list = StreamMapper.parse(body)
            if (list.isEmpty()) throw IOException("API ne koi stream nahi bheja")
            cacheFile.writeText(body)
            list
        }
    }

    /** Last successful API response (network fail hone par). */
    suspend fun cached(): List<Stream> = withContext(Dispatchers.IO) {
        runCatching {
            if (cacheFile.exists()) StreamMapper.parse(cacheFile.readText()) else emptyList()
        }.getOrDefault(emptyList())
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            Config.API_HEADERS.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Server error: HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
