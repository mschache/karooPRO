package io.github.mschache.karoopro.map

import java.io.File

/**
 * Remembers which map lines and symbols KarooPRO has drawn, in a small file that survives process
 * restarts and app updates. The Karoo keeps an extension's map items after its process dies, so a new
 * map layer first removes these leftovers.
 */
class MapLayerMemory(private val file: File) {

    data class Items(val lines: List<String>, val symbols: List<String>) {
        val isEmpty: Boolean get() = lines.isEmpty() && symbols.isEmpty()
    }

    fun remember(lines: Collection<String>, symbols: Collection<String>) {
        runCatching {
            if (lines.isEmpty() && symbols.isEmpty()) {
                file.delete()
            } else {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText((lines.map { "L:$it" } + symbols.map { "S:$it" }).joinToString("\n"))
                tmp.renameTo(file)
            }
        }
    }

    /** What the previous map layer left on the map; forgets it so it is only cleared once. */
    fun takeLeftovers(): Items {
        val rows = runCatching { if (file.exists()) file.readLines() else emptyList() }.getOrDefault(emptyList())
        file.delete()
        return Items(
            lines = rows.filter { it.startsWith("L:") }.map { it.removePrefix("L:") },
            symbols = rows.filter { it.startsWith("S:") }.map { it.removePrefix("S:") },
        )
    }
}
