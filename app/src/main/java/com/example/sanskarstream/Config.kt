package com.example.sanskarstream

/**
 * App-wide settings. Real API milne par sirf yahan API_URL / API_HEADERS badlein.
 */
object Config {
    /** Channel list API. Khaali ("") rakha to app [com.example.sanskarstream.data.SampleStreams] chalayega. */
    const val API_URL = ""

    /** Agar API ko token chahiye, e.g. mapOf("Authorization" to "Bearer xyz"). */
    val API_HEADERS: Map<String, String> = emptyMap()

    /**
     * Ek page par max kitne tiles live chalenge. Isse zyada tiles "OK to play" placeholder dikhayenge.
     * Website 16 chalati hai; sasta TV box hang ho to 8 ya 9 kar dein.
     */
    const val MAX_ACTIVE_PLAYERS = 16

    /** Grid ke players itne ms ke gap se start honge (tile 0, 1, 2 ...). */
    const val PLAYER_START_STAGGER_MS = 250L

    const val REFRESH_INTERVAL_MS = 5 * 60_000L
    const val BAR_AUTO_HIDE_MS = 5_000L
    const val AUDIO_HINT_MS = 4_000L
    const val TOAST_MS = 3_000L
    const val BUFFERING_TIMEOUT_MS = 20_000L
    val RETRY_DELAYS_MS = longArrayOf(5_000, 10_000, 30_000, 60_000)
}
