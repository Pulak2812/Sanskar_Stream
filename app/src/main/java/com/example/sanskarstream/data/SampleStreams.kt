package com.example.sanskarstream.data

import com.example.sanskarstream.domain.Stream

/**
 * Built-in channel list (website `stream.sanskargroup.in` wale 16 devotional channels).
 * [com.example.sanskarstream.Config.API_URL] khaali ho to app yahi chalata hai.
 * API mil jaane par wahi list server se aayegi aur ye sirf fallback rahegi.
 */
object SampleStreams {
    val list: List<Stream> = listOf(
        Stream("sanskar-tv", "SANSKAR TV", "https://d26idhjf0y1p2g.cloudfront.net/out/v1/cd66dd25b9774cb29943bab54bbf3e2f/index.m3u8"),
        Stream("satsang-tv", "SATSANG TV", "https://d2vfwvjxwtwq1t.cloudfront.net/out/v1/6b24239d5517495b986e7705490c6e65/index.m3u8"),
        Stream("sanskar-uk", "SANSKAR UK", "https://d34z4embz0hjf6.cloudfront.net/out/v1/7ac2789ff9a544a49337d1ffc54ce61c/index.m3u8"),
        Stream("sanskar-usa", "SANSKAR USA", "https://d2netiedy8cz3x.cloudfront.net/out/v1/9bf6fa4ac8d6432cb98da13b121ba3c2/index.m3u8"),
        Stream("shubh-tv", "SHUBH TV", "https://d2g1vdc6ozl2o8.cloudfront.net/out/v1/0a0dc7d7911b4fddbb4dfc963fdd4b9e/index.m3u8"),
        Stream("shubh-cinema", "SHUBH CINEMA", "https://d393sxaxig6bax.cloudfront.net/out/v1/589cf2cf44bf42bb941e817a2240d62e/index.m3u8"),
        Stream("aastha-tv", "AASTHA TV", "https://mumt05.tangotv.in/87NeALx2AASTHA/index.m3u8"),
        Stream("aastha-bhajan", "AASTHA BHAJAN", "https://aasthaott.akamaized.net/110923/smil:bhajan.smil/playlist.m3u8"),
        Stream("aastha-uk", "AASTHA UK", "https://aasthaott.akamaized.net/110923/smil:aasthauk.smil/playlist.m3u8"),
        Stream("aastha-usa", "AASTHA USA", "https://aasthaott.akamaized.net/110923/smil:aasthausa.smil/playlist.m3u8"),
        Stream("dharm-sandesh", "DHARM SANDESH", "https://aasthaott.akamaized.net/110923/smil:arihant.smil/playlist.m3u8"),
        Stream("sadhana-tv", "SADHANA TV", "https://6n3yow8pl9ok-hls-live.5centscdn.com/sadhanalivetv/live.stream/playlist.m3u8"),
        Stream("ishwar-tv", "ISHWAR TV", "https://6n3yow8pl9ok-hls-live.5centscdn.com/ishwartvlive/tv.stream/playlist.m3u8"),
        Stream("shraddha", "SHRADDHA", "https://livestream.jswk.online/mhoneshraddha/live/playlist.m3u8"),
        Stream("santvani", "SANTVANI", "https://d1msejlow1t3l4.cloudfront.net/fta/santwani/playlist.m3u8"),
        Stream("aaj-tak-devotional", "AAJ TAK", "https://aajtaklive-amd.akamaized.net/hls/live/2014416/aajtak/aajtaklive/live_404p/chunks.m3u8"),
    )
}
