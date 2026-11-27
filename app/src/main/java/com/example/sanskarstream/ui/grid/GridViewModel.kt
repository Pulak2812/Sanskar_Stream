package com.example.sanskarstream.ui.grid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanskarstream.Config
import com.example.sanskarstream.data.SettingsStore
import com.example.sanskarstream.data.StreamRepository
import com.example.sanskarstream.domain.GridLayout
import com.example.sanskarstream.domain.Stream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.sanskarstream.player.NetworkMonitor
import kotlin.math.max

data class GridUiState(
    val streams: List<Stream> = emptyList(),
    val layout: GridLayout = GridLayout.All,
    val page: Int = 0,
    val focusedIndex: Int = 0,
    /** Jis stream ki awaaz chal rahi hai (null = sab mute). */
    val audioId: String? = null,
    val barVisible: Boolean = true,
    val showAudioHint: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    /** API fail hua, purani list dikh rahi hai. */
    val offline: Boolean = false,
    val fullscreenIndex: Int? = null,
    /** Refresh button dabane par list aa rahi hai. */
    val refreshing: Boolean = false,
    /** Chhota message (e.g. "List updated") — kuch second baad gayab. */
    val toast: String? = null,
) {
    val dims: Pair<Int, Int> get() = layout.dims(streams.size)
    val pageSize: Int get() = dims.first * dims.second
    val totalPages: Int get() = max(1, (streams.size + pageSize - 1) / pageSize)
}

class GridViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StreamRepository(app)
    private val settings = SettingsStore(app)

    private val _state = MutableStateFlow(
        GridUiState(
            layout = settings.layout,
            audioId = settings.audioId,
            showAudioHint = settings.audioId == null,
        )
    )
    val state: StateFlow<GridUiState> = _state.asStateFlow()

    private var barHideJob: Job? = null
    private var toastJob: Job? = null
    private val loadLock = Mutex()

    init {
        viewModelScope.launch {
            // Pichhli baar ki list turant dikhao — API ka wait karte hue black screen nahi.
            val cached = repo.cached()
            if (cached.isNotEmpty()) {
                _state.update {
                    if (it.streams.isNotEmpty()) it
                    else it.copy(streams = applyOrder(cached), loading = false).clamped()
                }
            }
            load()
            while (isActive) {
                delay(Config.REFRESH_INTERVAL_MS)
                load()
            }
        }
        // Network wapas aaya aur list purani/fail thi -> turant dobara laao.
        viewModelScope.launch {
            NetworkMonitor.reconnects.collect {
                val s = _state.value
                if (s.offline || s.error != null) load()
            }
        }
        scheduleBarHide()
        if (_state.value.showAudioHint) {
            viewModelScope.launch {
                delay(Config.AUDIO_HINT_MS)
                _state.update { it.copy(showAudioHint = false) }
            }
        }
    }

    // ---------- Data ----------

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update {
                if (it.streams.isEmpty()) it.copy(loading = true, error = null) else it.copy(refreshing = true)
            }
            val ok = load()
            val count = _state.value.streams.size
            _state.update { it.copy(refreshing = false) }
            if (count > 0) showToast(if (ok) "✓  List updated · $count channels" else "⚠  Network issue — purani list chal rahi hai")
        }
    }

    /** @return true agar API se nayi list aayi. */
    private suspend fun load(): Boolean = loadLock.withLock { loadLocked() }

    private suspend fun loadLocked(): Boolean {
        val result = repo.fetch()
        val list = result.getOrNull()
        if (list != null) {
            _state.update { it.copy(streams = applyOrder(list), loading = false, error = null, offline = false).clamped() }
            return true
        }
        val current = _state.value
        if (current.streams.isNotEmpty()) {
            _state.update { it.copy(offline = true, loading = false) }
            return false
        }
        val cached = repo.cached()
        _state.update {
            if (cached.isNotEmpty()) {
                it.copy(streams = applyOrder(cached), offline = true, loading = false).clamped()
            } else {
                it.copy(loading = false, error = result.exceptionOrNull()?.message ?: "Streams load nahi hue")
            }
        }
        return false
    }

    private fun showToast(text: String) {
        toastJob?.cancel()
        _state.update { it.copy(toast = text) }
        toastJob = viewModelScope.launch {
            delay(Config.TOAST_MS)
            _state.update { it.copy(toast = null) }
        }
    }

    /** User ka swap kiya hua order pehle, naye channels end me. */
    private fun applyOrder(list: List<Stream>): List<Stream> {
        val order = settings.order
        if (order.isEmpty()) return list
        val rank = order.withIndex().associate { (i, id) -> id to i }
        return list.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    }

    private fun GridUiState.clamped(): GridUiState {
        val focus = focusedIndex.coerceIn(0, max(0, streams.size - 1))
        return copy(focusedIndex = focus, page = (focus / pageSize).coerceIn(0, totalPages - 1))
    }

    // ---------- Layout & pages ----------

    fun setLayout(layout: GridLayout) {
        settings.layout = layout
        _state.update { it.copy(layout = layout).clamped() }
        onUserActivity()
    }

    fun nextPage() = changePage(+1)
    fun prevPage() = changePage(-1)

    private fun changePage(delta: Int) {
        _state.update {
            val pages = it.totalPages
            if (pages <= 1) return@update it
            val page = (it.page + delta + pages) % pages
            it.copy(page = page, focusedIndex = page * it.pageSize)
        }
    }

    // ---------- Tiles ----------

    fun onTileFocused(index: Int) {
        _state.update { if (it.focusedIndex == index) it else it.copy(focusedIndex = index) }
    }

    /** OK press: pehli baar = is tile ki audio; audio wale tile par dobara = full screen. */
    fun onTileClicked(index: Int, live: Boolean) {
        val stream = _state.value.streams.getOrNull(index) ?: return
        if (!live || _state.value.audioId == stream.id) openFullscreen(index) else setAudio(stream.id)
    }

    fun setAudio(id: String?) {
        settings.audioId = id
        _state.update { it.copy(audioId = id, showAudioHint = false) }
    }

    fun swap(a: Int, b: Int) {
        _state.update {
            if (a !in it.streams.indices || b !in it.streams.indices) return@update it
            val list = it.streams.toMutableList()
            list[a] = list[b].also { list[b] = list[a] }
            settings.order = list.map(Stream::id)
            it.copy(streams = list, focusedIndex = a).clamped()
        }
    }

    // ---------- Full screen ----------

    fun openFullscreen(index: Int) {
        if (index in _state.value.streams.indices) _state.update { it.copy(fullscreenIndex = index) }
    }

    fun fullscreenStep(delta: Int) {
        _state.update {
            val i = it.fullscreenIndex ?: return@update it
            val n = it.streams.size
            it.copy(fullscreenIndex = (i + delta + n) % n)
        }
    }

    fun closeFullscreen() {
        val s = _state.value
        val i = s.fullscreenIndex ?: return
        val id = s.streams.getOrNull(i)?.id
        if (id != null) settings.audioId = id
        _state.update { it.copy(fullscreenIndex = null, focusedIndex = i, audioId = id ?: it.audioId).clamped() }
        showBar()
    }

    // ---------- Bottom bar ----------

    fun showBar() {
        _state.update { it.copy(barVisible = true) }
        scheduleBarHide()
    }

    fun hideBar() {
        barHideJob?.cancel()
        _state.update { it.copy(barVisible = false) }
    }

    /** Remote ka koi bhi button — bar ka auto-hide timer reset. */
    fun onUserActivity() {
        if (_state.value.barVisible) scheduleBarHide()
    }

    private fun scheduleBarHide() {
        barHideJob?.cancel()
        barHideJob = viewModelScope.launch {
            delay(Config.BAR_AUTO_HIDE_MS)
            _state.update { it.copy(barVisible = false) }
        }
    }
}
