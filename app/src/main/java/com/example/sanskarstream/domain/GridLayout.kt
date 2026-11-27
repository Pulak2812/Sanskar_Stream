package com.example.sanskarstream.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** Grid layout options shown in the bottom bar: 1 / 4 / 8 / All / Custom. */
sealed interface GridLayout {
    val key: String

    data object One : GridLayout { override val key = "1" }
    data object Four : GridLayout { override val key = "4" }
    data object Eight : GridLayout { override val key = "8" }
    data object All : GridLayout { override val key = "all" }
    data class Custom(val cols: Int, val rows: Int) : GridLayout { override val key = "custom" }

    /** (columns, rows) for this layout when [total] streams are available. */
    fun dims(total: Int): Pair<Int, Int> = when (this) {
        One -> 1 to 1
        Four -> 2 to 2
        Eight -> 4 to 2
        All -> auto(total)
        is Custom -> cols.coerceIn(1, MAX_SIDE) to rows.coerceIn(1, MAX_SIDE)
    }

    companion object {
        const val MAX_SIDE = 6

        /** Square-ish grid: 16 -> 4x4 (website jaisa), 8 -> 3x3, 5 -> 3x2. */
        fun auto(total: Int): Pair<Int, Int> {
            val n = max(total, 1)
            val cols = ceil(sqrt(n.toDouble())).toInt()
            val rows = ceil(n / cols.toDouble()).toInt()
            return cols to rows
        }

        fun fromKey(key: String?, cols: Int, rows: Int): GridLayout = when (key) {
            "1" -> One
            "4" -> Four
            "8" -> Eight
            "custom" -> Custom(cols, rows)
            else -> All
        }
    }
}
