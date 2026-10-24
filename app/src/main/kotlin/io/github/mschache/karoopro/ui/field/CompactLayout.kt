package io.github.mschache.karoopro.ui.field

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * How many values the Panels field fits: columns by the slot's shape, a second row only when the
 * slot is tall enough for two rows of readable numbers. Sizes are in dp.
 */
data class CompactLayout(val cols: Int, val rows: Int) {
    companion object {
        /** A value needs about this much height to stay readable on the bike. */
        private const val MIN_ROW_DP = 34f
        /** ...and about this much width. */
        private const val MIN_CELL_DP = 56f

        fun of(cells: Int, widthDp: Float, heightDp: Float): CompactLayout {
            if (cells <= 0) return CompactLayout(1, 1)
            val maxCols = (widthDp / MIN_CELL_DP).toInt().coerceIn(1, 4)
            val rows = if (cells > maxCols && heightDp >= 2 * MIN_ROW_DP + 6) 2 else 1
            val cols = min(maxCols, max(1, ceil(cells / rows.toFloat()).toInt()))
            return CompactLayout(cols, rows)
        }
    }
}
