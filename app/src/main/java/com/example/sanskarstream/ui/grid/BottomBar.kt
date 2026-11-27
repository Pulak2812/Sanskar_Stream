package com.example.sanskarstream.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.sanskarstream.ui.theme.MutedText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sanskarstream.domain.GridLayout
import com.example.sanskarstream.ui.common.TvButton

/** Niche ka control bar: 1 / 4 / 8 / All / Custom, pages, refresh. Selected button par [selectedRequester]. */
@Composable
fun BottomBar(
    state: GridUiState,
    selectedRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onLayout: (GridLayout) -> Unit,
    onCustom: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
            .padding(horizontal = 32.dp, vertical = 18.dp)
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val presets = listOf(
            "1" to GridLayout.One,
            "4" to GridLayout.Four,
            "8" to GridLayout.Eight,
            "All" to GridLayout.All,
        )
        presets.forEach { (label, layout) ->
            val selected = state.layout == layout
            TvButton(
                text = label,
                selected = selected,
                modifier = if (selected) Modifier.focusRequester(selectedRequester) else Modifier,
                onClick = { onLayout(layout) },
            )
        }
        val custom = state.layout as? GridLayout.Custom
        TvButton(
            text = if (custom != null) "Custom ${custom.cols}×${custom.rows}" else "Custom",
            selected = custom != null,
            modifier = if (custom != null) Modifier.focusRequester(selectedRequester) else Modifier,
            onClick = onCustom,
        )

        Spacer(Modifier.weight(1f))

        state.streams.firstOrNull { it.id == state.audioId }?.let { listening ->
            Text(
                "🔊 ${listening.title}",
                color = Color.White, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
            Spacer(Modifier.width(8.dp))
        }

        if (state.totalPages > 1) {
            TvButton("◀", onClick = onPrev)
            Text(
                "Page ${state.page + 1}/${state.totalPages}",
                color = Color.White, fontSize = 16.sp,
            )
            TvButton("▶", onClick = onNext)
            Spacer(Modifier.width(12.dp))
        }
        Text("${state.streams.size} channels", color = MutedText, fontSize = 14.sp)
        TvButton(if (state.refreshing) "⟳ Refreshing…" else "⟳ Refresh", onClick = onRefresh)
        Clock()
    }
}

/** Chhoti ghadi — 24x7 monitor screen par time dikhna kaam ka hai. */
@Composable
private fun Clock() {
    val format = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000) // agle minute ki shuruaat par update
        }
    }
    Text(
        format.format(Date(now)), color = Color.White, fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
    )
}
