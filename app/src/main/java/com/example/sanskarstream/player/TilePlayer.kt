package com.example.sanskarstream.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.sanskarstream.Config
import com.example.sanskarstream.domain.Stream
import kotlin.math.min

enum class TileStatus { Loading, Playing, Error }

/**
 * Ek tile ka ExoPlayer: quality cap, mute/unmute, aur error/buffering par auto-retry
 * (5s -> 10s -> 30s -> 60s). Ek tile ka error baaki tiles ko affect nahi karta.
 *
 * Grid tiles ([maxVideo] != null) chhota buffer rakhte hain — 16 players ke default
 * 50s buffer se sasta TV box RAM khatam karke hang ho jata hai.
 */
@OptIn(UnstableApi::class)
class TilePlayer(
    context: Context,
    stream: Stream,
    maxVideo: IntSize?,
    /** 16 players ek saath start karne se main thread/decoders par load aata hai — thoda aage-peeche start. */
    startDelayMs: Long = 0,
) {

    var status by mutableStateOf(TileStatus.Loading)
        private set

    val player: ExoPlayer = buildPlayer(context, compact = maxVideo != null)

    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private var active = true

    /** (Re)start: live stream ho to seedha live edge par — purani position se BEHIND_LIVE_WINDOW aata hai. */
    private val retry = Runnable {
        if (!active) return@Runnable
        player.seekToDefaultPosition()
        player.prepare()
    }

    private val bufferingWatchdog = Runnable {
        status = TileStatus.Error
        player.stop()
        scheduleRetry()
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            handler.removeCallbacks(bufferingWatchdog)
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (status != TileStatus.Error) status = TileStatus.Loading
                    handler.postDelayed(bufferingWatchdog, Config.BUFFERING_TIMEOUT_MS)
                }
                Player.STATE_READY -> {
                    status = TileStatus.Playing
                    retryCount = 0
                }
                // Live stream server ne band kar diya — frozen frame mat dikhao, retry karo.
                Player.STATE_ENDED -> {
                    status = TileStatus.Error
                    scheduleRetry()
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handler.removeCallbacks(bufferingWatchdog)
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                // Live stream me peeche reh gaye — seedha live edge par wapas.
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            status = TileStatus.Error
            scheduleRetry()
        }
    }

    init {
        player.apply {
            if (maxVideo != null) {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(maxVideo.width, maxVideo.height)
                    .build()
            }
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL // VOD test streams loop; live par koi asar nahi
            addListener(listener)
            setMediaItem(MediaItem.fromUri(stream.url))
            playWhenReady = true
        }
        handler.postDelayed(retry, startDelayMs)
    }

    fun setAudio(on: Boolean) {
        player.volume = if (on) 1f else 0f
    }

    /** Full screen me Play/Pause key. */
    fun setPaused(paused: Boolean) {
        player.playWhenReady = !paused
    }

    /** Network wapas aaya — offline tile backoff ka wait na kare, turant try kare. */
    fun retryNow() {
        if (!active || status != TileStatus.Error) return
        retryCount = 0
        handler.removeCallbacks(retry)
        handler.post(retry)
    }

    /** App background me gaya (Home button) — decoder free karo. */
    fun pause() {
        active = false
        handler.removeCallbacksAndMessages(null)
        player.stop()
    }

    fun resume() {
        if (active) return
        active = true
        retry.run()
    }

    fun release() {
        active = false
        handler.removeCallbacksAndMessages(null)
        player.removeListener(listener)
        player.release()
    }

    private fun scheduleRetry() {
        if (!active) return
        val delays = Config.RETRY_DELAYS_MS
        val delay = delays[min(retryCount, delays.lastIndex)]
        retryCount++
        handler.removeCallbacks(retry)
        handler.postDelayed(retry, delay)
    }

    private companion object {
        fun buildPlayer(context: Context, compact: Boolean): ExoPlayer {
            // Kai live CDN http -> https redirect karte hain; ExoPlayer default me ise block karta hai.
            val http = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(10_000)
            val builder = ExoPlayer.Builder(
                context,
                DefaultRenderersFactory(context).setEnableDecoderFallback(true),
            ).setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(DefaultDataSource.Factory(context, http))
            )
            if (compact) {
                builder.setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            /* minBufferMs = */ 6_000,
                            /* maxBufferMs = */ 15_000,
                            /* bufferForPlaybackMs = */ 1_500,
                            /* bufferForPlaybackAfterRebufferMs = */ 3_000,
                        )
                        .build()
                )
            }
            return builder.build()
        }
    }
}
