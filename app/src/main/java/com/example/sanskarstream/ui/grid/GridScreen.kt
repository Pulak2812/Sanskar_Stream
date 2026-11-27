package com.example.sanskarstream.ui.grid

import android.view.KeyEvent.KEYCODE_0
import android.view.KeyEvent.KEYCODE_1
import android.view.KeyEvent.KEYCODE_4
import android.view.KeyEvent.KEYCODE_8
import android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
import android.view.KeyEvent.KEYCODE_CHANNEL_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_REWIND
import android.view.KeyEvent.KEYCODE_MENU
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import android.view.KeyEvent.KEYCODE_PAGE_DOWN
import android.view.KeyEvent.KEYCODE_PAGE_UP
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sanskarstream.Config
import com.example.sanskarstream.domain.GridLayout
import com.example.sanskarstream.ui.common.ErrorScreen
import com.example.sanskarstream.ui.common.LoadingScreen
import com.example.sanskarstream.ui.fullscreen.FullScreenPlayer

@Composable
fun GridScreen(vm: GridViewModel, onExit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state
    val error = s.error
    val fullscreen = s.fullscreenIndex
    when {
        s.streams.isEmpty() && error != null -> ErrorScreen(error, onRetry = vm::refresh)
        s.streams.isEmpty() -> LoadingScreen()
        fullscreen != null -> FullScreenPlayer(
            stream = s.streams[fullscreen],
            position = fullscreen,
            total = s.streams.size,
            onPrev = { vm.fullscreenStep(-1) },
            onNext = { vm.fullscreenStep(+1) },
            onBack = vm::closeFullscreen,
        )
        else -> StreamGrid(s, vm, onExit)
    }
}

private fun KeyEvent.code() = nativeKeyEvent.keyCode
private fun KeyEvent.isCenter() = code() == KEYCODE_DPAD_CENTER || code() == KEYCODE_ENTER || code() == KEYCODE_NUMPAD_ENTER

@Composable
private fun StreamGrid(s: GridUiState, vm: GridViewModel, onExit: () -> Unit) {
    val (cols, rows) = s.dims
    val pageSize = cols * rows
    val pageStart = s.page * pageSize
    val maxVideo = maxVideoFor(pageSize)

    val tileRequesters = remember(pageSize) { List(pageSize) { FocusRequester() } }
    val barRequester = remember { FocusRequester() }
    var barFocused by remember { mutableStateOf(false) }
    var barFocusTick by remember { mutableIntStateOf(0) }
    var optionsFor by remember { mutableStateOf<Int?>(null) }
    var swapFor by remember { mutableStateOf<Int?>(null) }
    var showCustom by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }

    suspend fun focusTile() {
        val count = minOf(pageSize, s.streams.size - pageStart)
        if (count <= 0) return
        val local = (s.focusedIndex - pageStart).coerceIn(0, count - 1)
        withFrameNanos { }
        runCatching { tileRequesters[local].requestFocus() }
    }

    // Page/layout/list badle -> focused tile par focus (agar user bar me nahi hai).
    LaunchedEffect(s.page, pageSize, s.streams) { if (!barFocused) focusTile() }
    // Bar chhupa -> focus wapas tile par.
    LaunchedEffect(s.barVisible) { if (!s.barVisible) focusTile() }
    // Bar kholne ki request (DOWN last row / MENU).
    LaunchedEffect(barFocusTick) {
        if (barFocusTick > 0) {
            withFrameNanos { }
            runCatching { barRequester.requestFocus() }
        }
    }

    // Koi dialog band hua -> focus wapas usi tile par (warna remote "kahin nahi" atak jata hai).
    val dialogOpen = optionsFor != null || swapFor != null || showCustom || showExit
    LaunchedEffect(dialogOpen) { if (!dialogOpen && !barFocused) focusTile() }

    BackHandler {
        if (s.barVisible) vm.hideBar() else showExit = true
    }

    fun openBar() {
        vm.showBar()
        barFocusTick++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { ev ->
                vm.onUserActivity()
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ev.code()) {
                    KEYCODE_CHANNEL_UP, KEYCODE_PAGE_DOWN, KEYCODE_MEDIA_FAST_FORWARD -> { vm.nextPage(); true }
                    KEYCODE_CHANNEL_DOWN, KEYCODE_PAGE_UP, KEYCODE_MEDIA_REWIND -> { vm.prevPage(); true }
                    KEYCODE_1 -> { vm.setLayout(GridLayout.One); true }
                    KEYCODE_4 -> { vm.setLayout(GridLayout.Four); true }
                    KEYCODE_8 -> { vm.setLayout(GridLayout.Eight); true }
                    KEYCODE_0 -> { vm.setLayout(GridLayout.All); true }
                    KEYCODE_MENU -> { if (s.barVisible) vm.hideBar() else openBar(); true }
                    else -> false
                }
            },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (c in 0 until cols) {
                        val local = r * cols + c
                        val index = pageStart + local
                        val stream = s.streams.getOrNull(index)
                        if (stream == null) {
                            Spacer(Modifier.weight(1f).fillMaxHeight())
                        } else key(stream.id) {
                            val live = local < Config.MAX_ACTIVE_PLAYERS
                            val lastRow = r == rows - 1 || index + cols >= s.streams.size
                            var focused by remember { mutableStateOf(false) }
                            var longPress by remember { mutableStateOf(false) }
                            StreamTile(
                                stream = stream,
                                live = live,
                                maxVideo = maxVideo,
                                audioOn = s.audioId == stream.id,
                                focused = focused,
                                startDelayMs = local * Config.PLAYER_START_STAGGER_MS,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(tileRequesters[local])
                                    .onFocusChanged {
                                        focused = it.isFocused
                                        if (it.isFocused) vm.onTileFocused(index)
                                    }
                                    .onKeyEvent { ev ->
                                        when {
                                            // OK: chhota press = listen / full screen, long-press = options.
                                            // Options turant (button chhodne ka wait nahi); uska KeyUp ignore.
                                            ev.isCenter() && ev.type == KeyEventType.KeyDown -> {
                                                if (ev.nativeKeyEvent.repeatCount == 0) {
                                                    longPress = false
                                                } else if (!longPress) {
                                                    longPress = true
                                                    optionsFor = index
                                                }
                                                true
                                            }
                                            ev.isCenter() && ev.type == KeyEventType.KeyUp -> {
                                                if (!longPress) vm.onTileClicked(index, live)
                                                longPress = false
                                                true
                                            }
                                            ev.type != KeyEventType.KeyDown -> false
                                            ev.code() == KEYCODE_MEDIA_PLAY_PAUSE || ev.code() == KEYCODE_MEDIA_PLAY -> {
                                                vm.openFullscreen(index); true
                                            }
                                            ev.code() == KEYCODE_DPAD_DOWN && lastRow -> { openBar(); true }
                                            else -> false
                                        }
                                    }
                                    .focusable(),
                            )
                        }
                    }
                }
            }
        }

        if (s.showAudioHint) {
            Text(
                "🔊  Press OK on any screen to hear its audio",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6202020))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }

        s.toast?.let { msg ->
            Text(
                msg,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (s.offline) 48.dp else 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6202020))
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }

        if (s.offline) {
            Text(
                "Network issue — purani list chal rahi hai, retrying…",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6B91C1C))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }

        AnimatedVisibility(
            visible = s.barVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            BottomBar(
                state = s,
                selectedRequester = barRequester,
                onFocusChanged = { barFocused = it },
                onLayout = vm::setLayout,
                onCustom = { showCustom = true },
                onPrev = vm::prevPage,
                onNext = vm::nextPage,
                onRefresh = vm::refresh,
            )
        }
    }

    optionsFor?.let { index ->
        val stream = s.streams.getOrNull(index)
        if (stream == null) optionsFor = null else TileOptionsDialog(
            stream = stream,
            onListen = { vm.setAudio(stream.id); optionsFor = null },
            onFullscreen = { optionsFor = null; vm.openFullscreen(index) },
            onSwap = { optionsFor = null; swapFor = index },
            onDismiss = { optionsFor = null },
        )
    }

    swapFor?.let { index ->
        if (index !in s.streams.indices || s.streams.size < 2) swapFor = null else SwapDialog(
            streams = s.streams,
            current = index,
            onPick = { other -> vm.swap(index, other); swapFor = null },
            onDismiss = { swapFor = null },
        )
    }

    if (showCustom) {
        CustomLayoutDialog(
            initial = s.layout as? GridLayout.Custom,
            onApply = { vm.setLayout(it); showCustom = false },
            onDismiss = { showCustom = false },
        )
    }

    if (showExit) {
        ExitDialog(onExit = onExit, onDismiss = { showExit = false })
    }
}
