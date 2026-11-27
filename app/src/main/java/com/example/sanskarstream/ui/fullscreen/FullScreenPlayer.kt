package com.example.sanskarstream.ui.fullscreen

import android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
import android.view.KeyEvent.KEYCODE_CHANNEL_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_REWIND
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sanskarstream.domain.Stream
import com.example.sanskarstream.ui.common.RequestInitialFocus
import com.example.sanskarstream.ui.grid.StreamTile
import kotlinx.coroutines.delay

/** Ek channel full screen, full quality, audio ON. Up/Down = channel badlo, Back = grid. */
@Composable
fun FullScreenPlayer(
    stream: Stream,
    position: Int,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val requester = remember { FocusRequester() }
    var showInfo by remember(stream.id) { mutableStateOf(true) }
    var paused by remember(stream.id) { mutableStateOf(false) }
    LaunchedEffect(stream.id, showInfo) {
        if (showInfo) {
            delay(4_000)
            showInfo = false
        }
    }
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(requester)
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.nativeKeyEvent.keyCode) {
                    KEYCODE_DPAD_UP, KEYCODE_CHANNEL_UP, KEYCODE_MEDIA_REWIND -> { onPrev(); true }
                    KEYCODE_DPAD_DOWN, KEYCODE_CHANNEL_DOWN, KEYCODE_MEDIA_FAST_FORWARD -> { onNext(); true }
                    KEYCODE_DPAD_CENTER, KEYCODE_ENTER, KEYCODE_NUMPAD_ENTER -> { showInfo = !showInfo; true }
                    KEYCODE_MEDIA_PLAY_PAUSE -> { paused = !paused; true }
                    KEYCODE_MEDIA_PLAY -> { paused = false; true }
                    KEYCODE_MEDIA_PAUSE -> { paused = true; true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        key(stream.id) {
            StreamTile(
                stream = stream,
                live = true,
                maxVideo = null,
                audioOn = true,
                focused = false,
                showChrome = false,
                paused = paused,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (paused) {
            Text(
                "⏸  Paused",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 26.dp, vertical = 12.dp),
            )
        }
        if (showInfo) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    "${position + 1}/$total   ${stream.title}",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    "▲▼ channel badlo  ·  ⏯ pause  ·  BACK grid par wapas",
                    color = Color(0xFFBDBDBD), fontSize = 14.sp,
                )
            }
        }
    }
    RequestInitialFocus(requester)
}
