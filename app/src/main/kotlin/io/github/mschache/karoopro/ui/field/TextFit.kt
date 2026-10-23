package io.github.mschache.karoopro.ui.field

import kotlin.math.floor
import kotlin.math.max

/**
 * Lays out free text (coach messages) as large as it fits. Sizes are cap heights; [measure] gives a
 * string's width at cap height 1, which scales linearly with the size.
 */
object TextFit {
    /** Distance between baselines, in cap heights. */
    const val LINE_PITCH = 1.7f
    /** Room below the last baseline for descenders, in cap heights. */
    const val DESCENT = 0.4f

    data class Fitted(val capHeight: Float, val lines: List<String>)

    /** Greedy word wrap at [maxWidth]; a word wider than a line is split. Blank lines are dropped. */
    fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ', '\t').filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measure(candidate) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) lines += line
                var rest = word
                while (rest.length > 1 && measure(rest) > maxWidth) {
                    var n = rest.length - 1
                    while (n > 1 && measure(rest.take(n)) > maxWidth) n--
                    lines += rest.take(n)
                    rest = rest.drop(n)
                }
                line = rest
            }
            if (line.isNotEmpty()) lines += line
        }
        return lines
    }

    fun height(lines: Int, capHeight: Float): Float = if (lines <= 0) 0f else capHeight * (1 + DESCENT + (lines - 1) * LINE_PITCH)

    /**
     * The largest cap height up to [maxCap] at which [text] fits [width] x [height]. Below [minCap]
     * it stops shrinking and cuts the text with an ellipsis instead.
     */
    fun fit(text: String, width: Float, height: Float, maxCap: Float, minCap: Float, measure: (String) -> Float): Fitted {
        var cap = max(maxCap, minCap)
        while (true) {
            val lines = wrap(text, width / cap, measure)
            if (height(lines.size, cap) <= height) return Fitted(cap, lines)
            if (cap <= minCap) {
                val room = max(1, floor((height / cap - 1 - DESCENT) / LINE_PITCH).toInt() + 1)
                return Fitted(cap, ellipsize(lines, room, width / cap, measure))
            }
            cap = max(minCap, cap * 0.92f)
        }
    }

    private fun ellipsize(lines: List<String>, count: Int, maxWidth: Float, measure: (String) -> Float): List<String> {
        if (lines.size <= count) return lines
        var last = lines[count - 1]
        while (last.isNotEmpty() && measure("$last…") > maxWidth) last = last.dropLast(1)
        return lines.take(count - 1) + (last.trimEnd() + "…")
    }
}
