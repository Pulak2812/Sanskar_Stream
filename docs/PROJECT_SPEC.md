# SanskarStream — Android TV Multi-Stream Player

**Version:** 0.1 (Draft) · **Date:** 26 Nov 2026 · **Status:** v0.1 code ban gaya (M1–M7), real API aur TV device test baaki

---

## 1. Overview (App kya karega)

Ek Android TV app jo ek **API se stream URLs ki list** laata hai aur un sabko **ek saath grid me play** karta hai (jaise multi-view / CCTV monitor screen).
Screen ke **niche ek control bar** hoga jisme user choose kar sake ki ek baar me kitne streams dikhne chahiye:

| Option   | Layout (16:9 TV par)          | Kab use ho                           |
|----------|-------------------------------|--------------------------------------|
| **1**    | Full screen                   | Ek channel dhyaan se dekhna          |
| **4**    | 2 × 2 grid                    | Default                              |
| **8**    | 4 columns × 2 rows            | Zyada channels monitor karna         |
| **All**  | Auto grid (sab URLs)          | Pura overview                        |
| **Custom** | User khud number/cols×rows choose kare (jaise 6, 9, 3×3) | Apne hisaab se |

**Reference:** website `stream.sanskargroup.in` (Live TV | SANSKAR · SHUBH) — wahan 16 channels 4×4 grid me ek saath chalte hain (Sanskar, Satsang, Shubh, Shubh Cinema, Aastha, Aastha Bhajan, Dharm Sandesh, Sadhna, Shraddha, Santvani, Ishwar TV, Aaj Tak, etc.). TV app ka look & feel usi jaisa rakhna hai (detail section 5.2 me).

Agar streams ki sankhya layout se zyada hai (jaise 20 URLs, layout = 4), to **pages** banenge — user Left/Right (ya Page buttons) se agle/pichhle page par ja sakta hai.

---

## 2. Goals / Non-Goals

**Goals**
- API se URL list fetch karna aur auto-refresh karna.
- 1 / 4 / 8 / All / Custom layouts me live play.
- Pura app **TV remote (D-pad)** se chalna chahiye — touch ki zaroorat nahi.
- Focused tile ki hi audio chale, baaki mute (warna shor hoga).
- Kisi stream ke fail hone par sirf wahi tile "Offline" dikhaye, baaki chalte rahein.

**Non-Goals (v1 me nahi)**
- Login / user accounts
- Recording / DVR
- Phone/tablet UI (sirf TV)
- EPG / program guide

---

## 3. Tech Stack

Project already **Kotlin + Jetpack Compose** template par bana hai (`com.example.sanskarstream`, minSdk 24, targetSdk 37). Usi par build karenge.

| Kaam            | Library                                                         |
|-----------------|-----------------------------------------------------------------|
| UI              | Jetpack Compose + `androidx.tv:tv-material` (TV focus/components) |
| Video player    | **AndroidX Media3 ExoPlayer** (`media3-exoplayer`, `media3-exoplayer-hls`, `media3-exoplayer-dash`, `media3-ui-compose`) |
| Networking      | Retrofit + OkHttp (ya Ktor) + `kotlinx.serialization`           |
| State           | ViewModel + `StateFlow` (`lifecycle-viewmodel-compose`)         |
| Settings save   | SharedPreferences (`SettingsStore`: layout, custom grid, audio channel, swap order) |
| Images (optional) | Coil (channel logos)                                          |

Supported stream formats (Media3 se): **HLS (.m3u8)**, DASH (.mpd), progressive MP4, RTSP (extra module `media3-exoplayer-rtsp` agar chahiye).

---

## 4. API Contract

> ⚠️ **Asli API URL aur response format abhi pending hai** — mil jaane par ye section update karenge. Tab tak app is format ko expect karega; agar real API alag hai to sirf `StreamMapper` change hoga.

**Request**
```
GET {BASE_URL}/streams
Headers: (agar token chahiye) Authorization: Bearer <token>
```

**Expected Response (example)**
```json
{
  "streams": [
    { "id": "1", "title": "Channel 1", "url": "https://example.com/live/ch1/index.m3u8", "logo": "https://.../ch1.png" },
    { "id": "2", "title": "Channel 2", "url": "https://example.com/live/ch2/index.m3u8" }
  ]
}
```

- Minimum zaroori field: **`url`**. `id` na ho to index use hoga, `title` na ho to "Stream N".
- Agar API sirf plain array `["url1","url2"]` deti hai, to woh bhi mapper handle karega.

**Refresh policy**
- App start par fetch.
- Har **5 minute** me background refresh (configurable). Naya list aane par sirf jo URL badle hain unke player recreate honge — chal rahe streams disturb nahi honge.
- Network fail → last cached list (DataStore/JSON file) dikhao + "Offline, retrying…" banner.

---

## 5. Screens & UX

### 5.1 Splash / Loading
- Logo + loader jab tak API response aaye.
- Error par: message + **Retry** button (focused by default).

### 5.2 Main Grid Screen (core screen) — website jaisa design

Website (`stream.sanskargroup.in`) se liye gaye design rules:
- **Edge-to-edge grid:** koi margin/padding nahi, poori screen video se bhari. Tiles ke beech sirf **~2px black gap**.
- **Clean tiles:** normal halat me koi title/label nahi — channel ka logo video me hi hota hai.
- **Background:** pure black (`#000000`).
- **Sab muted start:** app khulte hi saare tiles muted; screen ke beech ek **toast/pill** aata hai:
  `🔊 Press OK on any screen to hear its audio` (dark rounded pill, white text, 4s baad fade out).

```
┌───────────────┬───────────────┬───────────────┬───────────────┐
│               │               │               │               │
│   (video)     │   (video)     │   (video)     │   (video)     │
├───────────────┼═══════════════╪───────────────┼───────────────┤
│               ║        ⇆  ⛶  ║               │               │
│   (video)     ║   (video)     ║   (video)     │   (video)     │
│               ║🔇 SHUBH CINEMA║               │               │
│               ║ · OK to listen║               │               │
├───────────────┼═══════════════╪───────────────┼───────────────┤
│               │   ( 🔊 Press OK on any screen to hear audio )  │
│   (video)     │   (video)     │   (video)     │   (video)     │
├───────────────┼───────────────┼───────────────┼───────────────┤
│      ◌        │               │               │               │
│ Loading       │   (video)     │   (video)     │   (video)     │
│ ISHWAR TV...  │               │               │               │
├───────────────┴───────────────┴───────────────┴───────────────┤
│ [1] [4] [8] [■All] [Custom]        ◀ 1/1 ▶      ⟳   (auto-hide)│
└───────────────────────────────────────────────────────────────┘
```

**Tile states**
| State | Kya dikhega |
|-------|-------------|
| Playing (normal) | Sirf video, koi overlay nahi |
| Loading / Buffering | Black background + beech me spinner + `Loading <CHANNEL NAME>...` (grey text) |
| **Focused** | Halka white/yellow border (2–3px) + top-right icons `⇆` (swap) & `⛶` (fullscreen) + bottom-left dark gradient strip: `🔇 CHANNEL NAME · OK to listen` |
| Focused + Audio ON | Strip me `🔊 CHANNEL NAME · Listening` |
| Audio ON (focus kahin aur) | Corner me chhota `🔊` badge, taaki pata rahe awaaz kahan se aa rahi hai |
| Offline / Error | Black + `⚠ CHANNEL NAME offline · retrying...` |

**Audio rule (website jaisa):** ek time par **sirf ek tile ki audio**. Focus move karne se audio nahi badlegi — OK dabane par hi badlegi (website par bhi "click to listen" hai). Ye choice DataStore me save, agli baar wahi channel ki audio.

**Bottom Control Bar** (website me nahi hai — TV ke liye extra)
- Video ke upar **overlay** (semi-transparent black gradient), grid ki jagah nahi khaata.
- Buttons: `1`, `4`, `8`, `All`, `Custom`, Page `◀ ▶`, Refresh `⟳`.
- Selected layout highlighted.
- **Auto-hide:** 5 second inactivity ke baad hide; last row se DOWN ya MENU dabane par wapas dikhe.

**Default layout:** website 16 channels **4×4** dikhati hai, isliye default **All** (16 channels → 4×4 auto).

**Swap (⇆):** focused tile par swap choose karo → channel list khule → jo channel chuno woh is position par aa jaye (dono ki jagah exchange). User ka custom order DataStore me save.

**Custom dialog**
- Option A: number choose karo (2–16) → app khud best cols×rows banaye.
- Option B: Columns aur Rows alag choose karo (jaise 3×3).
- Choice DataStore me save — agli baar app khulne par wahi layout.

### 5.3 Full Screen Mode
- Kisi tile par **OK/Center** dabao → wo stream full screen.
- Full screen me **Up/Down** = agla/pichhla channel.
- **Back** → wapas grid, same tile par focus.

---

## 6. Remote (D-pad) Key Mapping

| Key                   | Grid screen                              | Full screen          |
|-----------------------|------------------------------------------|----------------------|
| Arrows                | Tile se tile focus move (audio nahi badlegi) | Up/Down: channel change |
| OK / Center           | Focused tile ki audio ON (baaki mute). Jis tile ki audio already ON hai us par dobara OK → full screen | Controls show/hide   |
| OK long-press         | Tile options: Full screen / Swap         | —                    |
| Play/Pause key        | Focused tile full screen                 | Pause / resume       |
| Down (last row par)   | Bottom bar par focus                     | —                    |
| Back                  | Bar khula ho to band; warna "Exit?" confirm | Grid par wapas    |
| Channel+/− , FF/RW    | Next / Prev page                         | Next / Prev channel  |
| Menu                  | Bottom bar toggle                        | —                    |
| Number keys 1/4/8     | Shortcut: seedha woh layout              | —                    |

---

## 7. Grid Layout Logic

```
cols, rows by mode:
  1      → 1 × 1
  4      → 2 × 2
  8      → 4 × 2
  Custom → user ke cols × rows, ya count se auto
  All    → cols = ceil(sqrt(N)), rows = ceil(N / cols)
           (16 → 4×4: 16:9 screen par har tile bhi exact 16:9 banta hai)

pageSize   = cols * rows
totalPages = ceil(N / pageSize)
visible    = streams[page*pageSize  until  min((page+1)*pageSize, N)]
```

Tiles hamesha **16:9 aspect** me rahenge; bachi jagah black letterbox.

---

## 8. Player Management (sabse important part ⚠️)

TV boxes ki **hardware limit** hoti hai — zyada tar sasti Android TV devices ek saath sirf **2–6 video decoders** chala paati hain. Isliye 8 ya "All" me seedha 8+ HD streams chalana crash/black screen de sakta hai. Plan:

1. **Player Pool:** sirf **visible tiles** ke liye ExoPlayer instance banenge. Page change par purane release (`player.release()`), naye create. Max concurrent players ek config value (`MAX_ACTIVE_PLAYERS`, default 9).
2. **Quality cap per tile:** grid me har tile chhoti hai, isliye HLS ki low variant choose karo:
   - 1 tile → no limit
   - 4 tiles → max 720p
   - 8+ tiles → max 480p / 360p
   (`TrackSelectionParameters.setMaxVideoSize(...)` se)
3. **Audio:** sirf "audio tile" (jis par OK dabaya) ka `volume = 1f`, baaki `0f`.
   ⚠️ Website 16 streams ek saath chalati hai — TV box par 16 decoders lagbhag kabhi available nahi hote. Isliye "All"/16 mode me 360p cap + player pool limit zaroori hai; real device test ke baad decide hoga ki kitne tiles live chalein aur baaki thumbnail/placeholder rahein.
4. **Decoder fallback:** `DefaultRenderersFactory.setEnableDecoderFallback(true)` — hardware decoder khatam ho to software try kare.
5. **"All" mode me limit se zyada tiles:** jo tiles limit se bahar hain unme play nahi, sirf **logo/title + "Press OK to play"** placeholder (ya last frame thumbnail). Focus aane par play, purana sabse door wala release.
6. **Lifecycle:** app background (Home button) me → sab players pause/release; wapas aane par resume.
7. **Rendering:** har tile `SurfaceView` (Media3 `PlayerSurface`) — TextureView se kam battery/CPU.

### Error handling per tile
- Error aaye → tile "Offline" + auto retry **5s → 10s → 30s → 60s** (exponential backoff, max 60s).
- Buffering 20s se zyada → retry.
- Ek tile ka error baaki tiles ko affect nahi karega.

---

## 9. Architecture & Folder Structure

MVVM, single Activity.

```
app/src/main/java/com/example/sanskarstream/
├── MainActivity.kt                 // TV entry, Compose host, key events
├── data/
│   ├── api/StreamApi.kt            // Retrofit interface
│   ├── api/StreamDto.kt            // JSON models
│   ├── StreamMapper.kt             // DTO → domain (API format badle to sirf ye)
│   ├── StreamRepository.kt         // fetch + cache + refresh
│   └── SettingsStore.kt            // DataStore (layout, custom grid)
├── domain/
│   ├── Stream.kt                   // data class Stream(id, title, url, logo)
│   └── GridLayout.kt               // enum/sealed: One, Four, Eight, All, Custom(cols, rows)
├── player/
│   ├── PlayerPool.kt               // ExoPlayer create/reuse/release
│   └── RetryPolicy.kt
├── ui/
│   ├── grid/GridScreen.kt          // main screen
│   ├── grid/GridViewModel.kt       // state: streams, layout, page, focusedIndex
│   ├── grid/StreamTile.kt          // single video tile
│   ├── grid/BottomBar.kt           // 1 / 4 / 8 / All / Custom / pages
│   ├── grid/CustomLayoutDialog.kt
│   ├── fullscreen/FullScreenPlayer.kt
│   ├── common/LoadingScreen.kt, ErrorScreen.kt
│   └── theme/                      // already exists
└── Config.kt                       // BASE_URL, MAX_ACTIVE_PLAYERS, REFRESH_INTERVAL
```

**UI State (GridViewModel)**
```kotlin
data class GridUiState(
    val streams: List<Stream> = emptyList(),
    val layout: GridLayout = GridLayout.All,   // website jaisa default (16 → 4×4)
    val page: Int = 0,
    val focusedIndex: Int = 0,
    val audioIndex: Int? = null,               // jis tile ki awaaz chal rahi hai (null = sab mute)
    val isBarVisible: Boolean = true,
    val loading: Boolean = true,
    val error: String? = null,
)
```

---

## 10. Android TV Setup (Manifest / Gradle changes)

Current project ek phone template hai — TV ke liye ye changes zaroori hain:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.software.leanback" android:required="true" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />

<application
    android:banner="@drawable/tv_banner"          <!-- 320x180 px banner -->
    android:usesCleartextTraffic="true"            <!-- sirf agar http:// streams hain -->
    ...>
  <activity
      android:name=".MainActivity"
      android:screenOrientation="landscape"
      android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|smallestScreenSize">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
  </activity>
</application>
```

- Screen on rakhna: `window.addFlags(FLAG_KEEP_SCREEN_ON)` jab tak video chal raha ho.
- `android:windowSoftInputMode="adjustResize"` hata sakte hain (TV par zarurat nahi).
- `applicationId` `com.example...` ko final release se pehle apne package name me badalna (Play Store `com.example` accept nahi karta).

---

## 11. Development Milestones

| # | Milestone | Output |
|---|-----------|--------|
| M1 | TV setup | Manifest/Gradle changes, banner, app TV launcher me dikhe |
| M2 | Data layer | API call + mapper + cache, dummy JSON se test |
| M3 | Single player | 1 URL full screen chale (Media3) |
| M4 | Grid | 4 / 8 layouts, D-pad focus, focused audio |
| M5 | Bottom bar | Layout switch, auto-hide, pages |
| M6 | All + Custom | Auto grid, custom dialog, DataStore save |
| M7 | Robustness | Player pool limits, quality cap, retry/offline, lifecycle |
| M8 | Polish & test | Real TV device par 8-stream test, emulator test, release build |

**Test devices:** Android TV emulator (1080p) + kam se kam ek real TV box/Mi Box/Fire TV (decoder limit sirf real device par pata chalegi).

---

## 12. Open Questions (aapse chahiye)

1. **API URL** aur uska **actual JSON response** (ek sample paste kar dena).
2. API ko **token / header** chahiye kya?
3. Streams ka type — **HLS (.m3u8)**, RTSP, MP4, ya YouTube links? (YouTube links ExoPlayer me directly nahi chalte.)
4. Kitne URLs aam taur par aate hain? (5? 20? 100?) — "All" mode ka design isi par depend karega.
5. ~~Screenshot~~ ✅ mil gaya — UI section 5.2 usi ke hisaab se update.
6. Website `stream.sanskargroup.in` jo API use karti hai, kya wahi TV app me use karni hai?
7. App ka final naam, logo, aur package name.

---

## 13. Implementation Status (26 Nov 2026)

| Milestone | Status |
|-----------|--------|
| M1 TV setup (manifest, banner, leanback) | ✅ |
| M2 Data layer (`StreamRepository`, flexible `StreamMapper`, cache) | ✅ — `Config.API_URL` khaali hai, abhi `SampleStreams` me website ke **16 real channels** (Sanskar … Aaj Tak) built-in hain |
| M3 Single player / full screen | ✅ |
| M4 Grid + D-pad focus + OK se audio | ✅ |
| M5 Bottom bar, auto-hide, pages | ✅ |
| M6 All + Custom + swap, settings save | ✅ |
| M7 Quality cap, stagger start, retry/offline, lifecycle pause | ✅ |
| M8 Real TV box test, real API, release signing | ⏳ |

**Tested:** debug build OPPO phone (CPH2467) par — 4×4 grid chala, focus overlay sahi dikha. Phone par 16 decoders ki limit ki wajah se kuch tiles "offline · retrying" me gaye; unhone retry kiya, app crash nahi hua.

**Config knobs** (`Config.kt`): `API_URL`, `API_HEADERS`, `MAX_ACTIVE_PLAYERS` (16), `PLAYER_START_STAGGER_MS` (250), refresh / auto-hide / retry timings.

### v0.2 improvements (27 Nov 2026)

| Area | Kya badla |
|------|-----------|
| Player memory | Grid tiles ka buffer 6–15s (default 50s tha) — 16 players par RAM bachti hai, sasta box hang nahi hoga |
| Live retry | Retry ab seedha **live edge** se (`seekToDefaultPosition`); live stream ENDED ho to frozen frame ki jagah retry |
| Redirects | http ↔ https redirect wale stream URLs ab chalenge (`allowCrossProtocolRedirects`) |
| Network wapas | `NetworkMonitor`: Wi-Fi/LAN wapas aate hi offline tiles + channel list turant retry (60s backoff ka wait nahi) |
| Fast start | Pichhli cached list turant dikhti hai, API background me aati hai |
| Refresh | Button par "Refreshing…" + toast "✓ List updated · N channels" / network issue |
| Bottom bar | 🔊 kaunsa channel sun rahe hain + clock |
| Long-press OK | Options dialog turant khulta hai (button chhodne ka wait nahi) |
| Focus | Koi bhi dialog band ho to focus wapas usi tile par |
| Full screen | Play/Pause key = pause/resume (⏸ overlay), FF/RW = next/prev channel |
| Phone test | System bars hidden (immersive) |

**Emulator test (Medium_Phone, Android 9):** grid, bar, long-press options, focus restore, audio select sab chale; app crash nahi hui. Emulator ke software decoders 16 streams nahi sambhal paate (kuch tiles "offline · retrying") — asli limit real TV box par hi pata chalegi.

**Built-in channels (27 Nov 2026):** website wale 16 real devotional channels `SampleStreams.kt` me daal diye (PC se check: sab HTTP 200 + valid HLS). Mapper ab `number`/`order` se sort karta hai aur `enabled: false` wale channel chhod deta hai.
Aastha Bhajan / Dharm Sandesh ke diye gaye URL single-quality `chunklist_b1928000` the — app me unke master `playlist.m3u8` (144p–396p variants) laga diye, taaki grid me quality cap kaam kare.
