package io.github.mschache.karoopro.calc

/** Elevation against distance, both in meters. */
data class ProfilePoint(val distance: Double, val elevation: Double)

/**
 * Elevation trace of the ride so far, sampled every [stepMeters]. When it grows past [maxPoints]
 * every other point is dropped, so a long ride keeps its shape with bounded memory.
 */
class TerrainHistory(private val stepMeters: Double = 25.0, private val maxPoints: Int = 2000) {
    private val points = ArrayList<ProfilePoint>()
    private var step = stepMeters

    fun add(distance: Double?, elevation: Double?) {
        if (distance == null || elevation == null || !distance.isFinite() || !elevation.isFinite()) return
        val last = points.lastOrNull()
        if (last != null && distance - last.distance < step) return
        points += ProfilePoint(distance, elevation)
        if (points.size > maxPoints) {
            val kept = points.filterIndexed { i, _ -> i % 2 == 0 }
            points.clear()
            points.addAll(kept)
            step *= 2
        }
    }

    /** Points within the last [meters] of the ride. */
    fun window(meters: Double): List<ProfilePoint> {
        val end = points.lastOrNull()?.distance ?: return emptyList()
        return points.filter { it.distance >= end - meters }
    }

    fun all(): List<ProfilePoint> = points.toList()

    fun reset() {
        points.clear()
        step = stepMeters
    }

    fun restore(saved: List<ProfilePoint>) {
        reset()
        points.addAll(saved)
    }
}

object Polyline {
    /**
     * Decode a Google encoded polyline. Karoo's route elevation profile uses precision 1 with
     * (distance, elevation) pairs in place of (lat, lng).
     */
    fun decode(encoded: String, precision: Int): List<Pair<Double, Double>> {
        val factor = Math.pow(10.0, precision.toDouble())
        val out = ArrayList<Pair<Double, Double>>()
        var index = 0
        var a = 0
        var b = 0
        fun next(): Int {
            var result = 0
            var shift = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20 && index < encoded.length)
            return if (result and 1 != 0) (result shr 1).inv() else result shr 1
        }
        while (index < encoded.length) {
            a += next()
            if (index >= encoded.length) break
            b += next()
            out += (a / factor) to (b / factor)
        }
        return out
    }
}
