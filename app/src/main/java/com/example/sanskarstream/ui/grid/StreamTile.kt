package com.example.sanskarstream.ui.grid

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.sanskarstream.domain.Stream
import com.example.sanskarstream.player.NetworkMonitor
import com.example.sanskarstream.player.TilePlayer
import com.example.sanskarstream.player.TileStatus

/** Grid me tile chhota ho to stream ki chhoti quality lo (decoder aur bandwidth bachane ke liye). */
fun maxVideoFor(tiles: Int): IntSize? = when {
    tiles <= 1 -> null
    tiles <= 4 -> IntSize(1280, 720)
    tiles <= 9 -> IntSize(854, 480)
    else -> IntSize(640, 360)
}

/**
 * Ek video tile (website jaisa): normal me sirf video; focus par border + naam ki patti;
 * loading par spinner + "Loading NAME..."; error par "offline · retrying".
 *
 * @param live false ho to player nahi banta (MAX_ACTIVE_PLAYERS se bahar) — placeholder dikhta hai.
 * @param showChrome grid ke focus/audio overlays (full screen me false).
 */
@Composable
fun StreamTile(
    stream: Stream,
    live: Boolean,
    maxVideo: IntSize?,
    audioOn: Boolean,
    focused: Boolean,
    modifier: Modifier = Modifier,
    showChrome: Boolean = true,
    startDelayMs: Long = 0,
    paused: Boolean = false,
) {
    val tilePlayer = if (live) rememberTilePlayer(stream, maxVideo, startDelayMs) else null
    if (tilePlayer != null) {
        LaunchedEffect(tilePlayer, audioOn) { tilePlayer.setAudio(audioOn) }
        LaunchedEffect(tilePlayer, paused) { tilePlayer.setPaused(paused) }
        LaunchedEffect(tilePlayer) { NetworkMonitor.reconnects.collect { tilePlayer.retryNow() } }
    }

    val shape = RoundedCornerShape(0.dp)
    Box(
        modifier = modifier
            .background(Color.Black)
            .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier),
    ) {
        if (tilePlayer != null) {
            PlayerSurface(tilePlayer, Modifier.fillMaxSize())
            when (tilePlayer.status) {
                TileStatus.Loading -> CenterMessage(loading = true, text = "Loading ${stream.title}...")
                TileStatus.Error -> CenterMessage(loading = false, text = "⚠  ${stream.title} offline · retrying...")
                TileStatus.Playing -> Unit
            }
        } else {
            CenterMessage(loading = false, text = "${stream.title}\nPress OK to play")
        }

        if (!showChrome) return@Box

        if (focused) {
            // Top-right icons (website jaisa): swap & full screen. Long-press OK = options.
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconChip("⇄")
                IconChip("⛶")
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val label = if (audioOn) "🔊  ${stream.title.uppercase()}  · Listening"
                else "🔇  ${stream.title.uppercase()}  · OK to listen"
                Text(
                    label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (audioOn) {
            Text(
                "🔊",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun rememberTilePlayer(stream: Stream, maxVideo: IntSize?, startDelayMs: Long): TilePlayer {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilePlayer = remember(stream.id, stream.url, maxVideo) {
        TilePlayer(context.applicationContext, stream, maxVideo, startDelayMs)
    }
    DisposableEffect(tilePlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> tilePlayer.pause()
                Lifecycle.Event.ON_START -> tilePlayer.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tilePlayer.release()
        }
    }
    return tilePlayer
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerSurface(tilePlayer: TilePlayer, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setKeepContentOnPlayerReset(true)
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                player = tilePlayer.player
            }
        },
        update = { it.player = tilePlayer.player },
        onRelease = { it.player = null },
        modifier = modifier,
    )
}

@Composable
private fun CenterMessage(loading: Boolean, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xE6000000)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color(0xFFBDBDBD), strokeWidth = 2.dp, modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text,
            color = Color(0xFFBDBDBD),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun IconChip(symbol: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White, fontSize = 16.sp)
    }
}
