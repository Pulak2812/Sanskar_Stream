package com.example.sanskarstream.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sanskarstream.domain.GridLayout
import com.example.sanskarstream.domain.Stream
import com.example.sanskarstream.ui.common.RequestInitialFocus
import com.example.sanskarstream.ui.common.TvButton
import com.example.sanskarstream.ui.theme.PanelColor

@Composable
private fun TvDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .widthIn(min = 380.dp, max = 560.dp)
                .clip(shape)
                .background(PanelColor)
                .border(1.dp, Color(0x33FFFFFF), shape)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun ExitDialog(onExit: () -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    TvDialog("App band karein?", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Nahi", modifier = Modifier.focusRequester(first), onClick = onDismiss)
            TvButton("Haan, Exit", onClick = onExit)
        }
    }
    RequestInitialFocus(first)
}

/** Long-press OK par: Listen / Full screen / Swap. */
@Composable
fun TileOptionsDialog(
    stream: Stream,
    onListen: () -> Unit,
    onFullscreen: () -> Unit,
    onSwap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    TvDialog(stream.title, onDismiss) {
        TvButton("🔊  Is channel ki awaaz suno", Modifier.fillMaxWidth().focusRequester(first), onClick = onListen)
        TvButton("⛶  Full screen", Modifier.fillMaxWidth(), onClick = onFullscreen)
        TvButton("⇄  Swap (jagah badlo)", Modifier.fillMaxWidth(), onClick = onSwap)
    }
    RequestInitialFocus(first)
}

/** Swap: is position par kaunsa channel chahiye — dono ki jagah exchange hogi. */
@Composable
fun SwapDialog(streams: List<Stream>, current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    val firstIndex = if (current == 0) 1 else 0
    TvDialog("\"${streams[current].title}\" ki jagah kaunsa channel?", onDismiss) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(streams, key = { _, s -> s.id }) { i, s ->
                if (i != current) {
                    TvButton(
                        "${i + 1}.  ${s.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .then(if (i == firstIndex) Modifier.focusRequester(first) else Modifier),
                        onClick = { onPick(i) },
                    )
                }
            }
        }
    }
    RequestInitialFocus(first)
}

/** Custom layout: columns x rows khud chuno (1..6). */
@Composable
fun CustomLayoutDialog(initial: GridLayout.Custom?, onApply: (GridLayout.Custom) -> Unit, onDismiss: () -> Unit) {
    var cols by remember { mutableIntStateOf(initial?.cols ?: 3) }
    var rows by remember { mutableIntStateOf(initial?.rows ?: 3) }
    val first = remember { FocusRequester() }
    val max = GridLayout.MAX_SIDE
    TvDialog("Custom layout", onDismiss) {
        Stepper("Columns", cols, { cols = (cols - 1).coerceAtLeast(1) }, { cols = (cols + 1).coerceAtMost(max) }, first)
        Stepper("Rows", rows, { rows = (rows - 1).coerceAtLeast(1) }, { rows = (rows + 1).coerceAtMost(max) })
        Text(
            "$cols × $rows = ${cols * rows} screens",
            color = Color(0xFFBDBDBD), fontSize = 16.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Apply", selected = true, onClick = { onApply(GridLayout.Custom(cols, rows)) })
            TvButton("Cancel", onClick = onDismiss)
        }
    }
    RequestInitialFocus(first)
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    requester: FocusRequester? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, color = Color.White, fontSize = 18.sp, modifier = Modifier.width(110.dp))
        TvButton("−", modifier = if (requester != null) Modifier.focusRequester(requester) else Modifier, onClick = onMinus)
        Text("$value", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        TvButton("+", onClick = onPlus)
    }
}
