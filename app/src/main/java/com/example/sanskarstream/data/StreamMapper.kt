package com.example.sanskarstream.data

import com.example.sanskarstream.domain.Stream
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * API JSON -> [Stream] list. Kaafi formats samajhta hai:
 *  - `["url1", "url2"]`
 *  - `[{ "id": .., "title": .., "url": .. }]`
 *  - `{ "streams" | "channels" | "data" | ...: [ ... ] }` (nested bhi chalega)
 * Real API ka format alag nikle to sirf yahi file badalni hai.
 */
object StreamMapper {
    private val LIST_KEYS = listOf("streams", "channels", "data", "items", "results", "result", "list")
    private val URL_KEYS = listOf(
        "url", "stream_url", "streamUrl", "stream", "link", "hls", "hls_url", "hlsUrl",
        "src", "m3u8", "live_url", "liveUrl",
    )
    private val TITLE_KEYS = listOf("title", "name", "channel", "channel_name", "channelName")
    private val ID_KEYS = listOf("id", "_id", "slug", "channel_id", "channelId")
    private val LOGO_KEYS = listOf("logo", "logo_url", "image", "thumbnail", "icon")
    private val ORDER_KEYS = listOf("number", "order", "position", "sort", "channel_number", "channelNumber")
    private val ENABLED_KEYS = listOf("enabled", "active", "is_active", "isActive")

    fun parse(json: String): List<Stream> {
        val array = findArray(JSONTokener(json).nextValue(), depth = 0) ?: return emptyList()
        // (number, stream) — "number"/"order" ho to usi kram me, warna API ka kram.
        val out = ArrayList<Pair<Int, Stream>>(array.length())
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> if (item.isNotBlank()) {
                    out += i to Stream(id = item.trim(), title = "Stream ${i + 1}", url = item.trim())
                }
                is JSONObject -> {
                    val url = item.firstString(URL_KEYS) ?: continue
                    if (ENABLED_KEYS.any { item.has(it) && !item.optBoolean(it, true) }) continue
                    val number = ORDER_KEYS.firstNotNullOfOrNull { key ->
                        item.opt(key)?.toString()?.trim()?.toIntOrNull()
                    } ?: (i + 10_000)
                    out += number to Stream(
                        id = item.firstString(ID_KEYS) ?: url,
                        title = item.firstString(TITLE_KEYS) ?: "Stream ${i + 1}",
                        url = url,
                        logo = item.firstString(LOGO_KEYS),
                    )
                }
            }
        }
        return out.sortedBy { it.first }.map { it.second }.distinctBy { it.id }
    }

    private fun findArray(node: Any?, depth: Int): JSONArray? {
        if (depth > 3) return null
        return when (node) {
            is JSONArray -> node
            is JSONObject -> LIST_KEYS.firstNotNullOfOrNull { key -> findArray(node.opt(key), depth + 1) }
            else -> null
        }
    }

    private fun JSONObject.firstString(keys: List<String>): String? =
        keys.firstNotNullOfOrNull { key ->
            optString(key, "").trim().takeIf { it.isNotEmpty() && it != "null" }
        }
}
