package io.github.mschache.karoopro.ui.field

/**
 * Icons on the Ride tiles and the status line: Google's Material Symbols (Rounded, filled),
 * Apache License 2.0, from github.com/google/material-design-icons. [path] is the SVG path in its
 * 960-unit box (y from -960 to 0), drawn by [SvgPath].
 */
enum class TileIcon(val path: String) {
    /** Material Symbols Rounded "timer", filled. */
    TIMER("M400-840q-17 0-28.5-11.5T360-880q0-17 11.5-28.5T400-920h160q17 0 28.5 11.5T600-880q0 17-11.5 28.5T560-840H400Zm80 440q17 0 28.5-11.5T520-440v-160q0-17-11.5-28.5T480-640q-17 0-28.5 11.5T440-600v160q0 17 11.5 28.5T480-400Zm0 320q-74 0-139.5-28.5T226-186q-49-49-77.5-114.5T120-440q0-74 28.5-139.5T226-694q49-49 114.5-77.5T480-800q62 0 119 20t107 58l28-28q11-11 28-11t28 11q11 11 11 28t-11 28l-28 28q38 50 58 107t20 119q0 74-28.5 139.5T734-186q-49 49-114.5 77.5T480-80Z"),
    /** Material Symbols Rounded "flag", filled. */
    FLAG("M280-400v240q0 17-11.5 28.5T240-120q-17 0-28.5-11.5T200-160v-600q0-17 11.5-28.5T240-800h287q14 0 25 9t14 23l10 48h184q17 0 28.5 11.5T800-680v320q0 17-11.5 28.5T760-320H553q-14 0-25-9t-14-23l-10-48H280Z"),
    /** Material Symbols Rounded "local_fire_department", filled. */
    FIRE("M160-400q0-113 67-217t184-182q22-15 45.5-1.5T480-760v52q0 34 23.5 57t57.5 23q17 0 32.5-7.5T621-657q8-10 20.5-12.5T665-664q63 45 99 115t36 149q0 88-43 160.5T644-125q17-24 26.5-52.5T680-238q0-40-15-75.5T622-377L480-516 339-377q-29 29-44 64t-15 75q0 32 9.5 60.5T316-125q-70-42-113-114.5T160-400Zm320-4 85 83q17 17 26 38t9 45q0 49-35 83.5T480-120q-50 0-85-34.5T360-238q0-23 9-44.5t26-38.5l85-83Z"),
    /** Material Symbols Rounded "autorenew", filled. */
    CADENCE("M240-478q0 16 2 31.5t7 30.5q5 17-1 32.5T227-361q-16 8-31.5 1.5T175-383q-8-23-11.5-47t-3.5-48q0-134 93-228t227-94h7l-36-36q-11-11-11-28t11-28q11-11 28-11t28 11l104 104q12 12 12 28t-12 28L507-628q-11 11-28 11t-28-11q-11-11-11-28t11-28l36-36h-7q-100 0-170 70.5T240-478Zm480-4q0-16-2-31.5t-7-30.5q-5-17 1-32.5t21-22.5q16-8 31.5-1.5T785-577q8 23 11.5 47t3.5 48q0 134-93 228t-227 94h-7l36 36q11 11 11 28t-11 28q-11 11-28 11t-28-11L349-172q-12-12-12-28t12-28l104-104q11-11 28-11t28 11q11 11 11 28t-11 28l-36 36h7q100 0 170-70.5T720-482Z"),
    /** Material Symbols Rounded "bolt", filled. */
    BOLT("M360-360H236q-24 0-35.5-21.5T203-423l299-430q10-14 26-19.5t33 .5q17 6 25 21t6 32l-32 259h155q26 0 36.5 23t-6.5 43L416-100q-11 13-27 17t-31-3q-15-7-23.5-21.5T328-139l32-221Z"),
    /** Material Symbols Rounded "favorite", filled. */
    HEART("M480-147q-14 0-28.5-5T426-168l-69-63q-106-97-191.5-192.5T80-634q0-94 63-157t157-63q53 0 100 22.5t80 61.5q33-39 80-61.5T660-854q94 0 157 63t63 157q0 115-85 211T602-230l-68 62q-11 11-25.5 16t-28.5 5Z"),
    /** Material Symbols Rounded "speed", filled. */
    SPEED("M418-340q25 25 63 23.5t55-27.5l169-253q9-14-2.5-25.5T677-625L424-456q-26 18-28.5 54.5T418-340ZM204-160q-22 0-40.5-9.5T134-198q-26-47-40-97.5T80-400q0-83 31.5-156T197-683q54-54 127-85.5T480-800q82 0 154 31t126 84.5q54 53.5 86 125T879-406q1 55-12.5 107.5T825-198q-11 19-29.5 28.5T755-160H204Z"),
    /** Material Symbols Rounded "landscape", filled. */
    MOUNTAIN("m88-304 160-213q6-8 14.5-12t17.5-4q9 0 17.5 4t14.5 12l136 181q6 8 14 12t18 4q25 0 36-22.5t-4-42.5l-84-111q-8-11-8-24t8-24l100-133q6-8 14.5-12t17.5-4q9 0 17.5 4t14.5 12l280 373q15 20 4 42t-36 22H120q-25 0-36-22t4-42Z"),
    /** Material Symbols Rounded "trending_up", filled. */
    TREND("M108-255q-12-12-11.5-28.5T108-311l211-214q23-23 57-23t57 23l103 104 208-206h-64q-17 0-28.5-11.5T640-667q0-17 11.5-28.5T680-707h160q17 0 28.5 11.5T880-667v160q0 17-11.5 28.5T840-467q-17 0-28.5-11.5T800-507v-64L593-364q-23 23-57 23t-57-23L376-467 164-255q-11 11-28 11t-28-11Z"),
    /** Material Symbols Rounded "device_thermostat", filled. */
    THERMOMETER("M480-80q-83 0-141.5-58.5T280-280q0-48 21-89.5t59-70.5v-320q0-50 35-85t85-35q50 0 85 35t35 85v320q38 29 59 70.5t21 89.5q0 83-58.5 141.5T480-80Zm-40-440h80v-40h-40v-40h40v-80h-40v-40h40v-40q0-17-11.5-28.5T480-800q-17 0-28.5 11.5T440-760v240Z"),
    /** Material Symbols Rounded "air", filled. */
    WIND("M460-160q-32 0-59.5-16T356-220q-11-20-.5-40t32.5-20q13 0 23 8t18 18q5 7 13.5 10.5T460-240q17 0 28.5-11.5T500-280q0-17-11.5-28.5T460-320H120q-17 0-28.5-11.5T80-360q0-17 11.5-28.5T120-400h340q50 0 85 35t35 85q0 50-35 85t-85 35ZM120-560q-17 0-28.5-11.5T80-600q0-17 11.5-28.5T120-640h500q25 0 42.5-17.5T680-700q0-25-17.5-42.5T620-760q-16 0-30 7.5T568-731q-7 12-17 21.5t-24 9.5q-20 0-32.5-15t-6.5-32q14-42 50.5-67.5T620-840q58 0 99 41t41 99q0 58-41 99t-99 41H120Zm678 308q-20 9-39-2.5T740-288q0-14 9.5-23.5T771-328q14-8 21.5-22t7.5-30q0-25-17.5-42.5T740-440H120q-17 0-28.5-11.5T80-480q0-17 11.5-28.5T120-520h620q58 0 99 41t41 99q0 42-22 76.5T798-252Z"),
    /** Material Symbols Rounded "schedule", filled. */
    CLOCK("M520-496v-144q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640v159q0 8 3 15.5t9 13.5l132 132q11 11 28 11t28-11q11-11 11-28t-11-28L520-496ZM480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Z"),
    /** Material Symbols Rounded "wb_twilight", filled. */
    SUNSET("M792-670q11 11 11 28t-11 28l-29 29q-12 12-28.5 12T706-585q-12-12-11.5-28.5T707-642l29-29q12-11 28.5-10.5T792-670ZM120-160q-17 0-28.5-11.5T80-200q0-17 11.5-28.5T120-240h720q17 0 28.5 11.5T880-200q0 17-11.5 28.5T840-160H120Zm360-640q17 0 28.5 11.5T520-760v40q0 17-11.5 28.5T480-680q-17 0-28.5-11.5T440-720v-40q0-17 11.5-28.5T480-800ZM170-672q11-11 28-11t28 11l29 29q12 12 12 28.5T255-586q-12 11-29 11t-28-12l-29-29q-11-12-10.5-28.5T170-672Zm30 352q0-117 81.5-198.5T480-600q117 0 198.5 81.5T760-320H200Z"),
    /** Material Symbols Rounded "restaurant", filled. */
    FOOD("M280-600v-240q0-17 11.5-28.5T320-880q17 0 28.5 11.5T360-840v240h40v-240q0-17 11.5-28.5T440-880q17 0 28.5 11.5T480-840v240q0 56-34.5 98T360-446v326q0 17-11.5 28.5T320-80q-17 0-28.5-11.5T280-120v-326q-51-14-85.5-56T160-600v-240q0-17 11.5-28.5T200-880q17 0 28.5 11.5T240-840v240h40Zm400 200h-80q-17 0-28.5-11.5T560-440v-240q0-70 51.5-135T718-880q18 0 30 14t12 33v713q0 17-11.5 28.5T720-80q-17 0-28.5-11.5T680-120v-280Z"),
    /** Material Symbols Rounded "battery_full", filled. */
    BATTERY("M320-80q-17 0-28.5-11.5T280-120v-640q0-17 11.5-28.5T320-800h80v-40q0-17 11.5-28.5T440-880h80q17 0 28.5 11.5T560-840v40h80q17 0 28.5 11.5T680-760v640q0 17-11.5 28.5T640-80H320Z"),
    /** Material Symbols Rounded "tire_repair", filled. */
    TYRE("M732-652q-5-5-8.5-12.5T720-680q0-11 6-19.5t15-15.5q14-11 31-17t34-11q4-1 8-1t7 3q4 4 2 15-5 17-11 34t-17 31q-7 9-15.5 15t-19.5 6q-8 0-15.5-3.5T732-652ZM160-120q-33 0-56.5-23.5T80-200v-560q0-33 23.5-56.5T160-840h240q33 0 56.5 23.5T480-760v327q9-3 19-5t21-2q50 0 85 35t35 85v80q0 17 11.5 28.5T680-200q17 0 28.5-11.5T720-240v-200h-40v-57q-54-23-87-72t-33-111q0-83 58.5-141.5T760-880q83 0 141.5 58.5T960-680q0 62-33 111t-87 72v57h-40v200q0 50-35 85t-85 35q-50 0-85-35t-35-85v-80q0-17-11.5-28.5T520-360q-17 0-28.5 11.5T480-320v120q0 33-23.5 56.5T400-120H160Zm600-440q50 0 85-35t35-85q0-50-35-85t-85-35q-50 0-85 35t-35 85q0 50 35 85t85 35ZM152-688l40 40q14 14 29.5 12.5T248-648q11-11 12.5-27T248-705l-40-40q-14-14-29.5-12.5T152-745q-11 11-12.5 27t12.5 30Zm256 0q14-14 12.5-29.5T408-744q-11-11-26.5-13T352-745l-40 40q-14 14-12.5 29.5T312-649q11 11 26.5 13t29.5-12l40-40ZM152-488l40 40q14 14 29.5 12.5T248-448q11-11 12.5-27T248-505l-40-40q-14-14-29.5-12.5T152-545q-11 11-12.5 27t12.5 30Zm256 0q14-14 12.5-29.5T408-544q-11-11-26.5-13T352-545l-40 40q-14 14-12.5 29.5T312-449q11 11 26.5 13t29.5-12l40-40ZM152-288l40 40q14 14 29.5 12.5T248-248q11-11 12.5-27T248-305l-40-40q-14-14-29.5-12.5T152-345q-11 11-12.5 27t12.5 30Zm256 0q14-14 12.5-29.5T408-344q-11-11-26.5-13T352-345l-40 40q-14 14-12.5 29.5T312-249q11 11 26.5 13t29.5-12l40-40Z"),
    /** Material Symbols Rounded "balance", filled. */
    BALANCE("M120-120q-17 0-28.5-11.5T80-160q0-17 11.5-28.5T120-200h320v-447q-26-9-45-28t-28-45H240l110 258q5 11 6 22.5t-1 23.5q-9 46-49.5 71T220-320q-45 0-85.5-25T85-416q-2-12-1-23.5t6-22.5l110-258h-40q-17 0-28.5-11.5T120-760q0-17 11.5-28.5T160-800h207q12-35 43-57.5t70-22.5q39 0 70 22.5t43 57.5h207q17 0 28.5 11.5T840-760q0 17-11.5 28.5T800-720h-40l110 258q5 11 6 22.5t-1 23.5q-9 46-49.5 71T740-320q-45 0-85.5-25T605-416q-2-12-1-23.5t6-22.5l110-258H593q-9 26-28 45t-45 28v447h320q17 0 28.5 11.5T880-160q0 17-11.5 28.5T840-120H120Zm545-320h150l-75-174-75 174Zm-520 0h150l-75-174-75 174Zm335-280q17 0 28.5-11.5T520-760q0-17-11.5-28.5T480-800q-17 0-28.5 11.5T440-760q0 17 11.5 28.5T480-720Z"),
}

/** Receives a parsed SVG path; the painter's adapter builds an android.graphics.Path. */
interface PathSink {
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun quadTo(x1: Float, y1: Float, x: Float, y: Float)
    fun close()
}

/**
 * Just enough of SVG path syntax for the Material Symbols: M, L, H, V, Q, T and Z, absolute and
 * relative. Other commands end the path where they appear.
 */
object SvgPath {
    fun parse(d: String, sink: PathSink) {
        val tokens = Regex("[A-Za-z]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").findAll(d).map { it.value }.toList()
        var i = 0
        var cmd = ' '
        var x = 0f
        var y = 0f
        var startX = 0f
        var startY = 0f
        // The last quadratic control point, for T's reflection.
        var qx = 0f
        var qy = 0f
        var lastWasQuad = false
        fun num(): Float = tokens[i++].toFloat()
        fun hasNumber() = i < tokens.size && !tokens[i][0].isLetter()
        while (i < tokens.size) {
            if (tokens[i][0].isLetter()) cmd = tokens[i++][0] else if (cmd == ' ') return
            val rel = cmd.isLowerCase()
            val ox = if (rel) x else 0f
            val oy = if (rel) y else 0f
            when (cmd.uppercaseChar()) {
                'M' -> {
                    x = ox + num(); y = oy + num()
                    startX = x; startY = y
                    sink.moveTo(x, y)
                    lastWasQuad = false
                    // Further pairs after M are implicit L.
                    cmd = if (rel) 'l' else 'L'
                }
                'L' -> { x = ox + num(); y = oy + num(); sink.lineTo(x, y); lastWasQuad = false }
                'H' -> { x = ox + num(); sink.lineTo(x, y); lastWasQuad = false }
                'V' -> { y = oy + num(); sink.lineTo(x, y); lastWasQuad = false }
                'Q' -> {
                    qx = ox + num(); qy = oy + num()
                    x = ox + num(); y = oy + num()
                    sink.quadTo(qx, qy, x, y)
                    lastWasQuad = true
                }
                'T' -> {
                    qx = if (lastWasQuad) 2 * x - qx else x
                    qy = if (lastWasQuad) 2 * y - qy else y
                    x = ox + num(); y = oy + num()
                    sink.quadTo(qx, qy, x, y)
                    lastWasQuad = true
                }
                'Z' -> {
                    sink.close()
                    x = startX; y = startY
                    lastWasQuad = false
                    if (hasNumber()) return
                    continue
                }
                else -> return
            }
        }
    }
}
