package com.example.sanskarstream.data

import android.content.Context
import androidx.core.content.edit
import com.example.sanskarstream.domain.GridLayout

/** User ki choices yaad rakhta hai: layout, kis channel ki audio, channels ka order (swap). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var layout: GridLayout
        get() = GridLayout.fromKey(
            prefs.getString(KEY_LAYOUT, null),
            prefs.getInt(KEY_COLS, 3),
            prefs.getInt(KEY_ROWS, 3),
        )
        set(value) = prefs.edit {
            putString(KEY_LAYOUT, value.key)
            if (value is GridLayout.Custom) {
                putInt(KEY_COLS, value.cols)
                putInt(KEY_ROWS, value.rows)
            }
        }

    var audioId: String?
        get() = prefs.getString(KEY_AUDIO, null)
        set(value) = prefs.edit { putString(KEY_AUDIO, value) }

    var order: List<String>
        get() = prefs.getString(KEY_ORDER, "").orEmpty().split('\n').filter { it.isNotEmpty() }
        set(value) = prefs.edit { putString(KEY_ORDER, value.joinToString("\n")) }

    private companion object {
        const val KEY_LAYOUT = "layout"
        const val KEY_COLS = "custom_cols"
        const val KEY_ROWS = "custom_rows"
        const val KEY_AUDIO = "audio_id"
        const val KEY_ORDER = "order"
    }
}
