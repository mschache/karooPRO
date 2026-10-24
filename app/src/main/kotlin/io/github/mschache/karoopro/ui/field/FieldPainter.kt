package io.github.mschache.karoopro.ui.field

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.github.mschache.karoopro.R
import io.github.mschache.karoopro.calc.Fmt
import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.calc.TargetStatus
import io.github.mschache.karoopro.calc.Trend
import io.github.mschache.karoopro.calc.WindKind
import io.github.mschache.karoopro.calc.ZoneColors
import io.hammerhead.karooext.models.BatteryStatus
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Share of the field height taken by the hero; the tap zones in res/layout/field.xml match it. */
const val HERO_FRACTION = 0.52f
private const val HEADER_FRACTION = 0.075f

/**
 * Paints a whole [FieldModel] into one bitmap. A single image is cheap for the Karoo ride app to
 * show (no view inflation) and lets numbers use the bundled Oswald font, sized by ink bounds so
 * they never clip.
 */
class FieldPainter(context: Context) {
    private val bold: Typeface = ResourcesCompat.getFont(context, R.font.oswald_bold) ?: Typeface.DEFAULT_BOLD
    private val inkBounds = Rect()

    /** Cap height relative to text size for the bundled font, measured once. */
    private val capRatio: Float = Paint().apply { typeface = bold; textSize = 100f }.let { p ->
        Rect().also { p.getTextBounds("H", 0, 1, it) }.height() / 100f
    }

    fun paint(m: FieldModel, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, w), max(1, h), Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        c.drawColor(m.pageBackground)
        val heroH = h * HERO_FRACTION
        val headerH = h * HEADER_FRACTION
        hero(c, m, RectF(0f, 0f, w.toFloat(), heroH))
        header(c, m, RectF(0f, heroH, w.toFloat(), heroH + headerH))
        val content = RectF(0f, heroH + headerH, w.toFloat(), h.toFloat())
        c.drawRect(content, fill(m.palette.background))
        val pad = w * 0.015f
        val inner = RectF(content.left + pad, content.top + pad * 0.5f, content.right - pad, content.bottom - pad)
        panel(c, m, inner)
        m.overlay?.let { o ->
            val r = if (o.full) RectF(pad, pad, w - pad, h - pad) else inner
            overlay(c, m, o, r)
        }
        return bmp
    }

    /**
     * The PRO Strip: cells side by side, label over value; on a tall slot they wrap into two rows
     * so the numbers stay large.
     */
    fun paintStrip(m: StripModel, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        val p = m.palette
        c.drawColor(p.background)
        val n = m.cells.size.coerceAtLeast(1)
        // One row unless the cells would get narrower than about 70 % of the slot's height.
        val rows = if (n > 2 && width / n < height * 0.7f) 2 else 1
        val cols = (n + rows - 1) / rows
        val pad = min(width, height) * 0.06f
        val area = RectF(pad, pad * 0.6f, width - pad, height - pad * 0.6f)
        val gap = width * 0.02f
        val rowRects = if (rows == 1) listOf(area) else splitV(area, 1f, 1f, gap = pad * 0.5f)
        m.cells.forEachIndexed { i, cell ->
            val rr = rowRects[i / cols]
            val r = row(rr, cols, gap)[i % cols]
            stat(c, p, Stat(cell.label, cell.value, cell.unit, cell.color), r)
            if (i % cols != cols - 1) c.drawLine(r.right + gap / 2, r.top + r.height() * 0.1f, r.right + gap / 2, r.bottom - r.height() * 0.1f, stroke(p.faint, max(1f, width * 0.003f)))
        }
        return bmp
    }

    /** The standalone Wind field: compass on the left, head/tail/cross value and gust on the right. */
    fun paintWind(p: Palette, w: WeatherContent, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        c.drawColor(p.background)
        val r = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val pad = min(r.width(), r.height()) * 0.06f
        val size = min(r.height() - 2 * pad, r.width() * 0.45f)
        compass(c, w.relativeToDeg, w.kind, RectF(pad, r.centerY() - size / 2, pad + size, r.centerY() + size / 2), p)
        val info = RectF(pad * 2 + size, pad, r.right - pad, r.bottom - pad)
        if (w.message != null) {
            label(c, "WIND", info.left, info.top, info.height() * 0.14f, p.dim)
            number(c, Fmt.NONE, "", RectF(info.left, info.top + info.height() * 0.3f, info.right, info.bottom), p.text, p.dim, 0.6f)
            return bmp
        }
        val (main, gust) = splitV(info, 0.62f, 0.38f, gap = info.height() * 0.05f)
        number(c, w.component, w.unit, main, windColor(w.kind, p), p.dim, 0.7f, unitScale = 0.3f)
        w.stats.getOrNull(1)?.let { stat(c, p, it, gust) }
        return bmp
    }

    // ---------------------------------------------------------------- text

    private fun textPaint(color: Int, size: Float, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = bold
        textAlign = align
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Text whose cap height is [capHeight] px, its top at [top]. Returns the drawn width. */
    private fun label(c: Canvas, text: String, x: Float, top: Float, capHeight: Float, color: Int, align: Paint.Align = Paint.Align.LEFT, maxWidth: Float = Float.MAX_VALUE): Float {
        if (text.isEmpty()) return 0f
        val p = textPaint(color, capHeight / capRatio, align)
        val width = p.measureText(text)
        if (width > maxWidth) p.textSize *= maxWidth / width
        c.drawText(text, x, top + p.textSize * capRatio, p)
        return min(width, maxWidth)
    }

    /**
     * A number plus optional unit fitted into [r]: digits as tall as [inkFraction] of the box unless
     * the width runs out. [align] places the group; the unit sits on the number's baseline.
     */
    private fun number(
        c: Canvas,
        value: String,
        unit: String,
        r: RectF,
        color: Int,
        unitColor: Int,
        inkFraction: Float = 0.82f,
        align: Paint.Align = Paint.Align.LEFT,
        unitScale: Float = 0.28f,
        verticalCenter: Boolean = true,
    ): Float {
        val p = textPaint(color, 100f)
        p.getTextBounds(DIGITS, 0, DIGITS.length, inkBounds)
        val inkPer100 = inkBounds.height().toFloat()
        // "--" (no data) at full digit height looks like a glitch; draw it smaller, keep the unit as is.
        val placeholder = if (value == Fmt.NONE) 0.45f else 1f
        var size = r.height() * inkFraction / inkPer100 * 100f
        p.textSize = size * placeholder
        val u = textPaint(unitColor, size * unitScale)
        val gap = if (unit.isNotEmpty()) size * 0.06f else 0f
        var total = p.measureText(value) + gap + (if (unit.isNotEmpty()) u.measureText(unit) else 0f)
        if (total > r.width()) {
            val k = r.width() / total
            size *= k
            p.textSize = size * placeholder
            u.textSize = size * unitScale
            total *= k
        }
        val ink = inkPer100 * size / 100f
        val baseline = if (verticalCenter) r.centerY() + ink / 2 else r.bottom
        val x = when (align) {
            Paint.Align.LEFT -> r.left
            Paint.Align.CENTER -> r.centerX() - total / 2
            Paint.Align.RIGHT -> r.right - total
        }
        c.drawText(value, x, baseline, p)
        if (unit.isNotEmpty()) c.drawText(unit, x + p.measureText(value) + gap, baseline, u)
        return total
    }

    /** Label on top, value + unit below, filling [r]. */
    private fun stat(c: Canvas, p: Palette, s: Stat, r: RectF, align: Paint.Align = Paint.Align.LEFT) {
        val labelH = r.height() * 0.2f
        val x = when (align) {
            Paint.Align.LEFT -> r.left
            Paint.Align.CENTER -> r.centerX()
            Paint.Align.RIGHT -> r.right
        }
        label(c, s.label, x, r.top, labelH, p.dim, align, r.width())
        number(c, s.value, s.unit, RectF(r.left, r.top + labelH * 1.6f, r.right, r.bottom), s.accent ?: p.text, p.dim, 0.8f, align)
    }

    private fun card(c: Canvas, r: RectF, color: Int, radius: Float) = c.drawRoundRect(r, radius, radius, fill(color))

    private fun row(r: RectF, n: Int, gap: Float): List<RectF> {
        val w = (r.width() - gap * (n - 1)) / n
        return (0 until n).map { i -> RectF(r.left + i * (w + gap), r.top, r.left + i * (w + gap) + w, r.bottom) }
    }

    private fun splitV(r: RectF, vararg weights: Float, gap: Float = 0f): List<RectF> {
        val total = weights.sum()
        val avail = r.height() - gap * (weights.size - 1)
        var y = r.top
        return weights.map { wt ->
            val h = avail * wt / total
            RectF(r.left, y, r.right, y + h).also { y += h + gap }
        }
    }

    // ---------------------------------------------------------------- hero

    private fun hero(c: Canvas, m: FieldModel, r: RectF) {
        val h = m.hero
        val p = m.palette
        c.drawRect(r, fill(h.background))
        val pad = r.width() * 0.025f
        val (pillsR, bigR, cardsR) = splitV(RectF(r.left + pad, r.top + pad * 0.6f, r.right - pad, r.bottom - pad * 0.6f), 0.15f, 0.55f, 0.30f, gap = pad * 0.5f)

        // Pills: all of them up to three, else the big number on top and the one a tap brings next.
        val pillH = pillsR.height()
        var x = pillsR.left
        val pills = if (h.options.size <= 3) h.options else {
            val i = h.options.indexOf(h.big).coerceAtLeast(0)
            listOf(h.options[i], h.options[(i + 1) % h.options.size])
        }
        for (option in pills) {
            val selected = option == h.big
            val bg = when {
                !selected -> ZoneColors.blend(p.faint, h.background, 0.85f)
                h.filled -> h.textColor
                else -> h.zoneColor ?: p.accent
            }
            val fg = if (selected) ZoneColors.onColor(bg) else h.textColor
            val capH = pillH * 0.36f
            val tw = textPaint(fg, capH / capRatio).measureText(option.pill)
            val pr = RectF(x, pillsR.top + pillH * 0.06f, x + tw + pillH * 0.7f, pillsR.bottom - pillH * 0.06f)
            if (pr.right > pillsR.right) break
            c.drawRoundRect(pr, pillH / 2, pillH / 2, fill(bg))
            label(c, option.pill, pr.centerX(), pr.centerY() - capH / 2, capH, fg, Paint.Align.CENTER)
            x = pr.right + pad * 0.4f
        }
        // Stats, stacked label over value, right-aligned.
        var right = pillsR.right
        for (st in h.stats.reversed()) {
            val vp = textPaint(h.textColor, pillH * 0.62f / capRatio)
            val text = st.value + st.unit
            val vw = max(vp.measureText(text), textPaint(h.dimColor, pillH * 0.26f / capRatio).measureText(st.label))
            if (right - vw < x) break
            label(c, st.label, right, pillsR.top, pillH * 0.26f, h.dimColor, Paint.Align.RIGHT)
            label(c, text, right, pillsR.top + pillH * 0.38f, pillH * 0.62f, h.textColor, Paint.Align.RIGHT)
            right -= vw + pad * 0.9f
        }

        // Big number + zone badge and ladder
        val zoneW = r.width() * 0.16f
        val numR = RectF(bigR.left, bigR.top, bigR.right - zoneW - pad, bigR.bottom)
        number(c, h.value, h.unit, numR, h.numberColor, h.dimColor, inkFraction = 0.86f, unitScale = 0.2f)
        val zr = RectF(bigR.right - zoneW, bigR.top, bigR.right, bigR.bottom)
        val badge = RectF(zr.left, zr.top + zr.height() * 0.04f, zr.right, zr.top + zr.height() * 0.3f)
        val badgeBg = if (h.filled) h.textColor else h.zoneColor ?: p.faint
        val corner = badge.height() * 0.22f
        c.drawRoundRect(badge, corner, corner, fill(badgeBg))
        number(c, h.zone?.let { "Z$it" } ?: "Z-", "", RectF(badge.left, badge.top, badge.right, badge.bottom), ZoneColors.onColor(badgeBg), 0, 0.56f, Paint.Align.CENTER)
        // On a filled hero the current zone's bar is the background color: the ladder gets a card behind it.
        val ladder = if (h.filled) {
            val plate = RectF(zr.left, badge.bottom + zr.height() * 0.04f, zr.right, zr.bottom)
            card(c, plate, p.card, corner)
            RectF(plate.left + zoneW * 0.2f, plate.top + zoneW * 0.14f, plate.right - zoneW * 0.2f, plate.bottom - zoneW * 0.14f)
        } else {
            RectF(zr.left + zoneW * 0.12f, badge.bottom + zr.height() * 0.06f, zr.right - zoneW * 0.12f, zr.bottom - zr.height() * 0.02f)
        }
        zoneLadder(c, h.zoneColors, h.zone, ladder, if (h.filled) p.card else h.background, p.faint)

        // Cards
        row(cardsR, h.cards.size, pad).forEachIndexed { i, cr -> heroCard(c, m, h.cards[i], cr) }
    }

    private fun heroCard(c: Canvas, m: FieldModel, card: CardModel, r: RectF) {
        val p = m.palette
        val radius = r.height() * 0.14f
        card(c, r, p.card, radius)
        val bar = r.width() * 0.025f
        // On a filled hero the accent can be the background color: keep a rim of card around the bar.
        val rim = if (m.hero.filled) bar * 0.7f else 0f
        val edge = RectF(r.left + rim, r.top + rim, r.right - rim, r.bottom - rim)
        c.save()
        c.clipRect(edge.left, edge.top, edge.left + bar, edge.bottom)
        c.drawRoundRect(edge, radius - rim, radius - rim, fill(card.accent ?: p.faint))
        c.restore()
        val inner = RectF(edge.left + bar * 2.2f, r.top + r.height() * 0.12f, r.right - r.width() * 0.04f, r.bottom - r.height() * 0.1f)
        label(c, card.label, inner.left, inner.top, r.height() * 0.13f, p.dim)
        card.avg?.let { label(c, "AVG $it", inner.right, inner.top, r.height() * 0.13f, p.dim, Paint.Align.RIGHT) }
        number(c, card.value, card.unit, RectF(inner.left, inner.top + r.height() * 0.24f, inner.right, inner.bottom), p.text, p.dim, 0.86f, unitScale = 0.3f)
    }

    private fun zoneLadder(c: Canvas, colors: List<Int>, current: Int?, r: RectF, background: Int, idle: Int) {
        if (colors.isEmpty()) return
        val n = colors.size
        val gap = r.height() * 0.04f
        val barH = (r.height() - gap * (n - 1)) / n
        for (i in 0 until n) {
            val zone = n - i
            val top = r.top + i * (barH + gap)
            val active = current != null && zone <= current
            val inset = if (zone == current) 0f else r.width() * 0.16f
            val color = if (active) colors[zone - 1] else ZoneColors.blend(colors[zone - 1], ZoneColors.blend(idle, background, 0.5f), 0.3f)
            c.drawRoundRect(RectF(r.left + inset, top, r.right - inset, top + barH), barH / 2, barH / 2, fill(color))
        }
    }

    // ---------------------------------------------------------------- header

    private fun header(c: Canvas, m: FieldModel, r: RectF) {
        val p = m.palette
        c.drawRect(r, fill(p.background))
        val h = r.height()
        val chevW = h * 0.22f
        fun chevron(cx: Float, dir: Int) {
            val path = Path().apply {
                moveTo(cx + dir * -chevW / 2, r.centerY() - h * 0.22f)
                lineTo(cx + dir * chevW / 2, r.centerY())
                lineTo(cx + dir * -chevW / 2, r.centerY() + h * 0.22f)
            }
            c.drawPath(path, stroke(p.dim, h * 0.08f))
        }
        chevron(r.left + r.width() * 0.06f, -1)
        chevron(r.right - r.width() * 0.06f, 1)
        label(c, m.panel.title, r.centerX(), r.top + h * 0.14f, h * 0.38f, p.text, Paint.Align.CENTER)
        val n = m.panels.size
        val cur = m.panels.indexOf(m.panel)
        val dotR = h * 0.065f
        val step = dotR * 3.4f
        val start = r.centerX() - step * (n - 1) / 2
        for (i in 0 until n) {
            c.drawCircle(start + i * step, r.top + h * 0.78f, if (i == cur) dotR * 1.25f else dotR, fill(if (i == cur) p.accent else p.faint))
        }
    }

    // ---------------------------------------------------------------- panels

    private fun panel(c: Canvas, m: FieldModel, r: RectF) {
        when (val content = m.content) {
            is RideContent -> ride(c, m, content, r)
            is EffortContent -> effort(c, m, content, r)
            is WeatherContent -> weather(c, m, content, r)
            is LapContent -> lap(c, m, content, r)
            is AnalysisContent -> analysis(c, m, content, r)
            is WorkoutContent -> workout(c, m, content, r)
            is BikeContent -> bike(c, m, content, r)
            is CoachContent -> coach(c, m, content, r)
            is ClimbContent -> climb(c, m, content, r)
            is SegmentContent -> segment(c, m, content, r)
            is ResultsContent -> results(c, m, content, r)
            is FuelContent -> fuel(c, m, content, r)
            is PaceContent -> pace(c, m, content, r)
        }
    }

    private fun message(c: Canvas, m: FieldModel, text: String, r: RectF) {
        val lines = text.split("\n")
        val lh = min(r.height() * 0.11f, r.width() * 0.05f)
        val top = r.centerY() - lines.size * lh * 1.8f / 2
        lines.forEachIndexed { i, line -> label(c, line, r.centerX(), top + i * lh * 1.8f, lh, m.palette.dim, Paint.Align.CENTER, r.width()) }
    }

    private fun ride(c: Canvas, m: FieldModel, content: RideContent, r: RectF) {
        val p = m.palette
        val gap = r.width() * 0.015f
        val rows = splitV(r, 1f, 1f, gap = gap)
        rows.forEachIndexed { ri, rowR ->
            row(rowR, 2, gap).forEachIndexed { ci, tr ->
                val t = content.tiles.getOrNull(ri * 2 + ci) ?: return@forEachIndexed
                val radius = tr.height() * 0.1f
                card(c, tr, p.card, radius)
                val pad = tr.width() * 0.05f
                label(c, t.label, tr.left + pad, tr.top + tr.height() * 0.09f, tr.height() * 0.1f, p.dim, maxWidth = tr.width() - 2 * pad)
                number(c, t.value, t.unit, RectF(tr.left + pad, tr.top + tr.height() * 0.26f, tr.right - pad, tr.top + tr.height() * 0.66f), p.text, p.dim, 0.86f, unitScale = 0.3f)
                t.graph?.let { g ->
                    c.save()
                    c.clipRect(tr)
                    sparkline(c, g, RectF(tr.left, tr.top + tr.height() * 0.7f, tr.right, tr.bottom), t.graphColor, p.card)
                    c.restore()
                }
            }
        }
    }

    private fun sparkline(c: Canvas, values: List<Float?>, r: RectF, color: Int, background: Int) {
        val pts = values.withIndex().filter { it.value != null }
        if (pts.size < 2) return
        val lo = pts.minOf { it.value!! }
        val hi = pts.maxOf { it.value!! }
        val span = (hi - lo).takeIf { it > 1e-3f } ?: 1f
        val pad = r.height() * 0.1f
        fun x(i: Int) = r.left + i.toFloat() / (values.size - 1).coerceAtLeast(1) * r.width()
        fun y(v: Float) = r.bottom - pad - (v - lo) / span * (r.height() - 2 * pad)
        val line = Path()
        val area = Path()
        pts.forEachIndexed { k, (i, v) ->
            if (k == 0) {
                line.moveTo(x(i), y(v!!))
                area.moveTo(x(i), r.bottom)
                area.lineTo(x(i), y(v))
            } else {
                line.lineTo(x(i), y(v!!))
                area.lineTo(x(i), y(v))
            }
        }
        area.lineTo(x(pts.last().index), r.bottom)
        area.close()
        c.drawPath(area, fill(ZoneColors.blend(color, background, 0.25f)))
        c.drawPath(line, stroke(color, max(2f, r.height() / 18f)))
    }

    private fun effort(c: Canvas, m: FieldModel, e: EffortContent, r: RectF) {
        val p = m.palette
        val gap = r.height() * 0.04f
        val (barsR, statsR, terrainR) = splitV(r, 0.36f, 0.3f, 0.34f, gap = gap)
        // Effort bars
        val grid = stroke(p.faint, 1f)
        for (k in 1..3) c.drawLine(barsR.left, barsR.top + barsR.height() * k / 4, barsR.right, barsR.top + barsR.height() * k / 4, grid)
        val slot = barsR.width() / e.bars.size.coerceAtLeast(1)
        val bw = slot * 0.78f
        e.bars.forEachIndexed { i, (v, color) ->
            if (v == null) return@forEachIndexed
            val bh = (v / e.barMax.coerceAtLeast(1f)).coerceIn(0.02f, 1f) * barsR.height()
            val left = barsR.left + i * slot + (slot - bw) / 2
            c.drawRoundRect(RectF(left, barsR.bottom - bh, left + bw, barsR.bottom), bw / 4, bw / 4, fill(color))
        }
        // W' + stats
        val cells = row(statsR, 4, r.width() * 0.02f)
        stat(c, m.palette, e.wPrime, RectF(cells[0].left, cells[0].top, cells[0].right, cells[0].bottom - statsR.height() * 0.14f))
        progress(c, e.wPrimeFraction, RectF(cells[0].left, cells[0].bottom - statsR.height() * 0.1f, cells[0].right, cells[0].bottom), p.accent, p.faint)
        e.stats.forEachIndexed { i, s -> stat(c, m.palette, s, cells[i + 1]) }
        // Terrain
        terrain(c, e.terrain, terrainR, p.faint, p.dim, p.accent)
        label(c, e.terrainLabel, terrainR.left, terrainR.top, terrainR.height() * 0.12f, p.dim)
    }

    private fun terrain(c: Canvas, points: List<Pair<Float, Float>>, r: RectF, behind: Int, ahead: Int, marker: Int) {
        if (points.size < 2) return
        val minX = min(points.minOf { it.first }, 0f)
        val maxX = max(points.maxOf { it.first }, 0f)
        val spanX = (maxX - minX).takeIf { it > 1f } ?: 1f
        val lo = points.minOf { it.second }
        val hi = points.maxOf { it.second }
        val spanY = max(hi - lo, 30f)
        val top = r.top + r.height() * 0.2f
        fun x(d: Float) = r.left + (d - minX) / spanX * r.width()
        fun y(e: Float) = r.bottom - (e - lo) / spanY * (r.bottom - top)
        fun area(seg: List<Pair<Float, Float>>, color: Int) {
            if (seg.size < 2) return
            val path = Path()
            path.moveTo(x(seg.first().first), r.bottom)
            seg.forEach { path.lineTo(x(it.first), y(it.second)) }
            path.lineTo(x(seg.last().first), r.bottom)
            path.close()
            c.drawPath(path, fill(color))
        }
        area(points.filter { it.first <= 0f }, behind)
        area(points.filter { it.first >= 0f }, ahead)
        points.minByOrNull { abs(it.first) }?.let { here ->
            val mx = x(0f)
            c.drawLine(mx, r.top, mx, r.bottom, stroke(marker, 2f))
            c.drawCircle(mx, y(here.second), r.height() * 0.06f, fill(marker))
        }
    }

    private fun windColor(kind: WindKind?, p: Palette) = when (kind) {
        WindKind.HEAD -> p.bad
        WindKind.TAIL -> p.good
        WindKind.CROSS -> p.warn
        null -> p.dim
    }

    private fun arrow(c: Canvas, cx: Float, cy: Float, length: Float, deg: Float, color: Int) {
        c.save()
        c.rotate(deg, cx, cy)
        val head = length * 0.42f
        val path = Path().apply {
            moveTo(cx, cy - length / 2)
            lineTo(cx + head * 0.62f, cy - length / 2 + head)
            lineTo(cx + head * 0.2f, cy - length / 2 + head)
            lineTo(cx + head * 0.2f, cy + length / 2)
            lineTo(cx - head * 0.2f, cy + length / 2)
            lineTo(cx - head * 0.2f, cy - length / 2 + head)
            lineTo(cx - head * 0.62f, cy - length / 2 + head)
            close()
        }
        c.drawPath(path, fill(color))
        c.restore()
    }

    fun compass(c: Canvas, relativeToDeg: Float?, kind: WindKind?, r: RectF, p: Palette) {
        val size = min(r.width(), r.height())
        val cx = r.centerX()
        val cy = r.centerY()
        val radius = size * 0.44f
        c.drawCircle(cx, cy, radius, stroke(p.faint, size * 0.035f))
        val tick = Path().apply {
            moveTo(cx, cy - radius - size * 0.02f)
            lineTo(cx - size * 0.05f, cy - radius + size * 0.07f)
            lineTo(cx + size * 0.05f, cy - radius + size * 0.07f)
            close()
        }
        c.drawPath(tick, fill(p.dim))
        if (relativeToDeg != null) arrow(c, cx, cy, radius * 1.25f, relativeToDeg, windColor(kind, p))
    }

    private fun weather(c: Canvas, m: FieldModel, w: WeatherContent, r: RectF) {
        val p = m.palette
        if (w.message != null) return message(c, m, w.message, r)
        val (topR, hoursR) = splitV(r, 0.62f, 0.38f, gap = r.height() * 0.04f)
        val compassR = RectF(topR.left, topR.top, topR.left + topR.height(), topR.bottom)
        compass(c, w.relativeToDeg, w.kind, compassR, p)
        val info = RectF(compassR.right + r.width() * 0.04f, topR.top, topR.right, topR.bottom)
        val (compR, s1, s2) = splitV(info, 0.34f, 0.33f, 0.33f, gap = info.height() * 0.03f)
        number(c, w.component, w.unit, compR, windColor(w.kind, p), p.dim, 0.8f, unitScale = 0.32f)
        row(s1, 2, r.width() * 0.03f).forEachIndexed { i, cell -> w.stats.getOrNull(i)?.let { stat(c, m.palette, it, cell) } }
        row(s2, 2, r.width() * 0.03f).forEachIndexed { i, cell -> w.stats.getOrNull(i + 2)?.let { stat(c, m.palette, it, cell) } }
        // Next hours
        if (w.hours.isEmpty()) return
        val cols = row(hoursR, w.hours.size, 0f)
        w.hours.forEachIndexed { i, h ->
            val col = cols[i]
            val arrowLen = min(col.width() * 0.45f, col.height() * 0.32f)
            h.relativeToDeg?.let { arrow(c, col.centerX(), col.top + col.height() * 0.2f, arrowLen, it, windColor(h.kind, p)) }
            label(c, h.speed, col.centerX(), col.top + col.height() * 0.44f, col.height() * 0.2f, p.text, Paint.Align.CENTER)
            label(c, h.label, col.centerX(), col.top + col.height() * 0.76f, col.height() * 0.15f, p.dim, Paint.Align.CENTER)
        }
    }

    private fun lap(c: Canvas, m: FieldModel, l: LapContent, r: RectF) {
        val p = m.palette
        val gap = r.width() * 0.015f
        val (titleR, gridR) = splitV(r, 0.13f, 0.87f, gap = gap)
        label(c, l.title, titleR.left, titleR.top + titleR.height() * 0.1f, titleR.height() * 0.62f, p.dim)
        splitV(gridR, 1f, 1f, gap = gap).forEachIndexed { ri, rowR ->
            row(rowR, 2, gap).forEachIndexed { ci, cr ->
                val f = l.fields.getOrNull(ri * 2 + ci) ?: return@forEachIndexed
                card(c, cr, p.card, cr.height() * 0.1f)
                val pad = cr.width() * 0.05f
                label(c, f.label, cr.left + pad, cr.top + cr.height() * 0.1f, cr.height() * 0.12f, p.dim)
                val (arrowText, color) = when (f.trend) {
                    Trend.UP -> "▲" to p.good
                    Trend.DOWN -> "▼" to p.bad
                    Trend.SAME -> "=" to p.dim
                    Trend.NONE -> "" to p.dim
                }
                val arrowW = cr.width() * 0.16f
                number(c, f.value, f.unit, RectF(cr.left + pad, cr.top + cr.height() * 0.3f, cr.right - pad - arrowW, cr.top + cr.height() * 0.72f), p.text, p.dim, 0.9f, unitScale = 0.3f)
                if (arrowText.isNotEmpty()) {
                    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = cr.height() * 0.28f; textAlign = Paint.Align.CENTER }
                    c.drawText(arrowText, cr.right - pad - arrowW / 2, cr.top + cr.height() * 0.64f, tp)
                }
                f.previous?.let { label(c, "prev $it", cr.left + pad, cr.top + cr.height() * 0.8f, cr.height() * 0.1f, p.dim) }
            }
        }
    }

    private fun analysis(c: Canvas, m: FieldModel, a: AnalysisContent, r: RectF) {
        val p = m.palette
        val gap = r.width() * 0.02f
        val (bestR, titleR, zonesR, totalsR) = splitV(r, 0.25f, 0.08f, 0.42f, 0.25f, gap = r.height() * 0.025f)
        row(bestR, a.best.size, gap).forEachIndexed { i, cell -> stat(c, m.palette, a.best[i], cell) }
        label(c, a.zoneTitle, titleR.left, titleR.top, titleR.height() * 0.8f, p.dim)
        if (a.zones.isNotEmpty()) {
            val rowH = zonesR.height() / a.zones.size
            val labelW = zonesR.width() * 0.08f
            val timeW = zonesR.width() * 0.14f
            val barMax = zonesR.width() - labelW - timeW
            a.zones.forEachIndexed { i, z ->
                val top = zonesR.top + i * rowH
                label(c, z.label, zonesR.left, top + rowH * 0.18f, rowH * 0.58f, p.dim)
                val bw = max(2f, barMax * z.fraction)
                c.drawRoundRect(RectF(zonesR.left + labelW, top + rowH * 0.16f, zonesR.left + labelW + bw, top + rowH * 0.84f), rowH * 0.2f, rowH * 0.2f, fill(z.color))
                label(c, Fmt.shortDuration(z.seconds), zonesR.right, top + rowH * 0.18f, rowH * 0.58f, p.text, Paint.Align.RIGHT)
            }
        }
        row(totalsR, a.totals.size, gap).forEachIndexed { i, cell -> stat(c, m.palette, a.totals[i], cell) }
    }

    private fun statusColor(s: TargetStatus, p: Palette) = when (s) {
        TargetStatus.PUSH -> p.push
        TargetStatus.EASE -> p.bad
        TargetStatus.IN_RANGE -> p.good
        TargetStatus.NO_DATA -> p.dim
    }

    private fun statusLabel(s: TargetStatus) = when (s) {
        TargetStatus.PUSH -> "PUSH ▲"
        TargetStatus.EASE -> "EASE ▼"
        TargetStatus.IN_RANGE -> "IN RANGE"
        TargetStatus.NO_DATA -> "--"
    }

    /**
     * Green inside the band, fading to blue (push) below it and to red (ease) above it as the gap
     * grows; the plain status color without a reading.
     */
    private fun deviationColor(output: Int?, low: Int?, high: Int?, status: TargetStatus, p: Palette): Int {
        val d = if (low != null && high != null) TargetBand.deviation(output, low to high) else null
        return when {
            d == null -> statusColor(status, p)
            d < 0 -> ZoneColors.blend(p.push, p.good, (0.35 + 0.65 * -d).toFloat())
            d > 0 -> ZoneColors.blend(p.bad, p.good, (0.35 + 0.65 * d).toFloat())
            else -> p.good
        }
    }

    private fun workout(c: Canvas, m: FieldModel, wc: WorkoutContent, r: RectF) {
        val p = m.palette
        val s = wc.state ?: return message(c, m, "No workout running", r)
        val color = deviationColor(s.output, s.low, s.high, s.status, p)
        val (lineR, targetR, gaugeR, statsR, nextR) = splitV(r, 0.12f, 0.3f, 0.1f, 0.3f, 0.12f, gap = r.height() * 0.025f)
        label(c, "STEP ${s.stepNumber}/${s.stepCount} · ${s.kind.label}", lineR.left, lineR.top + lineR.height() * 0.15f, lineR.height() * 0.62f, p.text)
        label(c, s.stepRemainingS?.let { Fmt.seconds(it.toLong()) + " LEFT" } ?: "OPEN", lineR.right, lineR.top + lineR.height() * 0.15f, lineR.height() * 0.62f, p.text, Paint.Align.RIGHT)

        val radius = targetR.height() * 0.14f
        card(c, targetR, p.card, radius)
        c.save()
        c.clipRect(targetR.left, targetR.top, targetR.left + targetR.width() * 0.02f, targetR.bottom)
        c.drawRoundRect(targetR, radius, radius, fill(color))
        c.restore()
        val target = if (s.low != null && s.high != null) "${s.low}–${s.high}" else Fmt.NONE
        val statusW = targetR.width() * 0.3f
        number(c, target, wc.unit, RectF(targetR.left + targetR.width() * 0.05f, targetR.top + targetR.height() * 0.12f, targetR.right - statusW, targetR.bottom - targetR.height() * 0.12f), p.text, p.dim, 0.82f, unitScale = 0.25f)
        label(c, statusLabel(s.status), targetR.right - targetR.width() * 0.03f, targetR.centerY() - targetR.height() * 0.13f, targetR.height() * 0.26f, color, Paint.Align.RIGHT, statusW)

        gauge(c, s.low, s.high, s.output, gaugeR, p, color)

        val cells = row(statsR, 3, r.width() * 0.03f)
        stat(c, m.palette, Stat("STEP AVG", Fmt.int(s.stepAverage?.toDouble()), wc.unit), cells[0])
        stat(c, m.palette, Stat("IN TARGET", Fmt.int(s.inTargetPercent?.toDouble()), "%", p.good), cells[1])
        label(c, "PROGRESS", cells[2].left, cells[2].top, cells[2].height() * 0.2f, p.dim)
        progress(c, s.progress, RectF(cells[2].left, cells[2].centerY(), cells[2].right, cells[2].centerY() + cells[2].height() * 0.18f), p.text, p.faint)

        val whatNext = wc.next ?: "STEP ${s.stepNumber + 1}/${s.stepCount}"
        val next = when {
            s.isLastStep -> "LAST STEP" + (s.totalRemainingS?.let { " · ${Fmt.seconds(it.toLong())} TO GO" } ?: "")
            s.stepRemainingS != null -> "NEXT  $whatNext · IN ${Fmt.seconds(s.stepRemainingS.toLong())}"
            else -> "NEXT  $whatNext · AFTER LAP"
        }
        // A planned target is worth reading, so it isn't dimmed.
        label(c, next, nextR.left, nextR.top + nextR.height() * 0.15f, nextR.height() * 0.6f, if (wc.next != null && !s.isLastStep) p.text else p.dim, maxWidth = nextR.width())
    }

    private fun gauge(c: Canvas, low: Int?, high: Int?, value: Int?, r: RectF, p: Palette, statusColor: Int) {
        val trackH = r.height() * 0.36f
        val cy = r.centerY()
        val track = RectF(r.left, cy - trackH / 2, r.right, cy + trackH / 2)
        c.drawRoundRect(track, trackH / 2, trackH / 2, fill(p.faint))
        if (low == null || high == null) return
        val span = (high - low).coerceAtLeast(1)
        val minV = low - span * 1.2f
        val maxV = high + span * 1.2f
        fun x(v: Float) = r.left + ((v - minV) / (maxV - minV)).coerceIn(0f, 1f) * r.width()
        c.drawRoundRect(RectF(x(low.toFloat()), track.top, x(high.toFloat()), track.bottom), trackH / 2, trackH / 2, fill(p.good))
        if (value != null) {
            val vx = x(value.toFloat())
            val mw = r.height() * 0.09f
            c.drawRoundRect(RectF(vx - mw, r.top, vx + mw, r.bottom), mw, mw, fill(statusColor))
        }
    }

    private fun progress(c: Canvas, fraction: Float?, r: RectF, color: Int, track: Int) {
        val h = r.height()
        c.drawRoundRect(r, h / 2, h / 2, fill(track))
        val f = (fraction ?: 0f).coerceIn(0f, 1f)
        if (f > 0f) c.drawRoundRect(RectF(r.left, r.top, max(r.left + h, r.left + r.width() * f), r.bottom), h / 2, h / 2, fill(color))
    }

    private fun batteryColor(status: BatteryStatus?, p: Palette): Int = when (status) {
        BatteryStatus.NEW, BatteryStatus.GOOD -> p.good
        BatteryStatus.OK -> p.text
        BatteryStatus.LOW -> p.warn
        BatteryStatus.CRITICAL -> p.bad
        BatteryStatus.INVALID, null -> p.faint
    }

    private fun battery(c: Canvas, status: BatteryStatus?, r: RectF, p: Palette) {
        val color = batteryColor(status, p)
        val bodyR = r.left + r.width() * 0.88f
        val sw = max(2f, r.height() * 0.1f)
        c.drawRoundRect(RectF(r.left + sw / 2, r.top + sw / 2, bodyR, r.bottom - sw / 2), r.height() * 0.15f, r.height() * 0.15f, stroke(color, sw))
        c.drawRect(RectF(bodyR, r.top + r.height() * 0.32f, r.right, r.top + r.height() * 0.68f), fill(color))
        val segments = when (status) {
            BatteryStatus.NEW -> 4
            BatteryStatus.GOOD -> 3
            BatteryStatus.OK -> 2
            BatteryStatus.LOW, BatteryStatus.CRITICAL -> 1
            else -> 0
        }
        val inner = RectF(r.left + sw * 1.6f, r.top + sw * 1.6f, bodyR - sw * 1.1f, r.bottom - sw * 1.6f)
        val segW = inner.width() / 4
        for (i in 0 until segments) c.drawRect(RectF(inner.left + i * segW + 1, inner.top, inner.left + (i + 1) * segW - 1, inner.bottom), fill(color))
    }

    private fun bike(c: Canvas, m: FieldModel, b: BikeContent, r: RectF) {
        val p = m.palette
        if (b.gearing == null) return message(c, m, "Pair Di2 or AXS\nto see your gears", r)
        val gap = r.width() * 0.03f
        val (topR, battR, trainR, statsR) = splitV(r, 0.25f, 0.1f, 0.4f, 0.25f, gap = r.height() * 0.02f)
        val (gearsR, balR) = row(topR, 2, gap)
        stat(c, m.palette, Stat("GEARS", b.gears), gearsR)
        stat(c, m.palette, b.balance, RectF(balR.left, balR.top, balR.right, balR.bottom - topR.height() * 0.16f))
        balanceBar(c, b.balanceLeft, RectF(balR.left, balR.bottom - topR.height() * 0.1f, balR.right, balR.bottom), p)
        // Batteries: label + glyph for each.
        val slots = row(battR, b.batteries.size.coerceAtLeast(1), gap)
        b.batteries.forEachIndexed { i, (name, status) ->
            val s = slots[i]
            val lw = label(c, name, s.left, s.top + s.height() * 0.2f, s.height() * 0.6f, p.dim)
            val bw = min(s.height() * 2.2f, s.width() - lw - gap * 0.5f)
            battery(c, status, RectF(s.left + lw + gap * 0.4f, s.top + s.height() * 0.1f, s.left + lw + gap * 0.4f + bw, s.bottom - s.height() * 0.1f), p)
        }
        drivetrain(c, b.gearing, trainR, p, b.teeth)
        row(statsR, b.stats.size, gap).forEachIndexed { i, cell -> stat(c, m.palette, b.stats[i], cell) }
    }

    private fun balanceBar(c: Canvas, left: Float?, r: RectF, p: Palette) {
        val h = r.height()
        c.drawRoundRect(r, h / 2, h / 2, fill(p.faint))
        left?.let { c.drawRoundRect(RectF(r.left, r.top, r.left + r.width() * it.coerceIn(0f, 1f), r.bottom), h / 2, h / 2, fill(p.accent)) }
        c.drawLine(r.centerX(), r.top - h * 0.3f, r.centerX(), r.bottom + h * 0.3f, stroke(p.text, 2f))
    }

    private fun drivetrain(c: Canvas, g: Gearing, r: RectF, p: Palette, teeth: String) {
        val h = r.height()
        val cy = r.centerY()
        val ringX = r.left + r.width() * 0.24f
        val cassX = r.left + r.width() * 0.78f
        val ringR = h * 0.42f
        val chain = if (g.crossChained) p.bad else p.good
        val fronts = (g.frontMax ?: 2).coerceIn(1, 3)
        for (i in 0 until fronts) {
            val rad = ringR * (1f - i * 0.17f)
            val active = g.frontGear != null && (fronts - i) == g.frontGear
            c.drawCircle(ringX, cy, rad, stroke(if (active) chain else p.faint, if (active) h * 0.045f else h * 0.02f))
        }
        val cogs = (g.rearMax ?: 11).coerceIn(1, 13)
        val maxCog = h * 0.3f
        val minCog = h * 0.1f
        val pos = g.rearPosition
        var activeR = 0f
        for (i in 0 until cogs) {
            val f = if (cogs > 1) i.toFloat() / (cogs - 1) else 0f
            val rad = maxCog - (maxCog - minCog) * f
            val active = pos != null && abs(f - pos) < 0.5f / cogs
            if (active) activeR = rad
            c.drawCircle(cassX, cy, rad, stroke(if (active) chain else p.faint, if (active) h * 0.04f else h * 0.012f))
        }
        if (g.frontGear != null && activeR > 0f) {
            val fr = ringR * (1f - (fronts - g.frontGear).coerceAtLeast(0) * 0.17f)
            c.drawLine(ringX, cy - fr, cassX, cy - activeR, stroke(chain, h * 0.025f))
            c.drawLine(ringX, cy + fr, cassX, cy + activeR, stroke(chain, h * 0.025f))
        }
        c.drawCircle(ringX, cy, h * 0.06f, fill(p.dim))
        label(c, teeth, (ringX + cassX) / 2, r.top + h * 0.02f, h * 0.12f, if (g.crossChained) p.bad else p.text, Paint.Align.CENTER)
        if (g.crossChained) label(c, "CROSS-CHAIN", (ringX + cassX) / 2, r.bottom - h * 0.13f, h * 0.11f, p.bad, Paint.Align.CENTER)
    }

    /** Title and W′ on top, to the top (or the climb's size) large, the climb's profile by grade, then four stats. */
    private fun climb(c: Canvas, m: FieldModel, cc: ClimbContent, r: RectF) {
        val p = m.palette
        cc.message?.let { return message(c, m, it, r) }
        val gap = r.width() * 0.03f
        val (topR, bigR, profR, statsR) = splitV(r, 0.11f, 0.27f, 0.36f, 0.2f, gap = r.height() * 0.025f)
        val capH = topR.height() * 0.62f
        val top = topR.top + (topR.height() - capH) / 2
        val effortW = cc.effort?.let { label(c, it, topR.right, top, capH, if (cc.effortWarning) p.bad else p.good, Paint.Align.RIGHT, topR.width() * 0.45f) } ?: 0f
        label(c, cc.title, topR.left, top, capH, p.text, maxWidth = topR.width() - effortW - gap)

        row(bigR, cc.big.size.coerceAtLeast(1), gap).forEachIndexed { i, cell -> cc.big.getOrNull(i)?.let { stat(c, p, it, cell) } }

        // Profile: one filled slice per stretch, colored by its grade; the part behind the rider dimmed.
        val floor = profR.height() * 0.08f
        fun x(f: Float) = profR.left + f * profR.width()
        fun y(h: Float) = profR.bottom - floor - h * (profR.height() - floor)
        for (b in cc.bars) {
            val path = Path().apply {
                moveTo(x(b.from), profR.bottom)
                lineTo(x(b.from), y(b.fromHeight))
                lineTo(x(b.to), y(b.toHeight))
                lineTo(x(b.to), profR.bottom)
                close()
            }
            val color = gradeColor(b.grade)
            c.drawPath(path, fill(if (cc.rider != null && b.to <= cc.rider) ZoneColors.blend(color, p.background, 0.35f) else color))
        }
        cc.rider?.let { f ->
            val bar = cc.bars.firstOrNull { f >= it.from && f <= it.to } ?: cc.bars.lastOrNull()
            val h = bar?.let { b -> b.fromHeight + (b.toHeight - b.fromHeight) * ((f - b.from) / (b.to - b.from).coerceAtLeast(1e-3f)) } ?: 0f
            val rx = x(f)
            c.drawLine(rx, profR.top, rx, profR.bottom, stroke(p.text, max(2f, profR.height() * 0.015f)))
            c.drawCircle(rx, y(h), profR.height() * 0.06f, fill(p.text))
        }

        row(statsR, cc.stats.size.coerceAtLeast(1), gap).forEachIndexed { i, cell -> cc.stats.getOrNull(i)?.let { stat(c, p, it, cell) } }
    }

    /** The gap to your PR as large as it fits, green ahead and red behind; the KOM gap, progress and three stats below. */
    private fun segment(c: Canvas, m: FieldModel, sc: SegmentContent, r: RectF) {
        val p = m.palette
        sc.message?.let { return message(c, m, it, r) }
        fun color(ahead: Boolean?) = when (ahead) {
            true -> p.good
            false -> p.bad
            null -> p.text
        }
        val gap = r.width() * 0.03f
        val (topR, mainR, secondR, barR, statsR) = splitV(r, 0.11f, 0.36f, 0.13f, 0.05f, 0.22f, gap = r.height() * 0.025f)
        label(c, sc.title, topR.left, topR.top + topR.height() * 0.19f, topR.height() * 0.62f, p.text, maxWidth = topR.width())

        val labelH = mainR.height() * 0.16f
        label(c, sc.main.label, mainR.left, mainR.top, labelH, p.dim)
        number(c, sc.main.value, sc.main.unit, RectF(mainR.left, mainR.top + labelH * 1.4f, mainR.right, mainR.bottom), color(sc.mainAhead), p.dim, 0.9f, Paint.Align.CENTER)

        val capH = secondR.height() * 0.55f
        val top = secondR.top + (secondR.height() - capH) / 2
        val lw = label(c, sc.second.label, secondR.left, top, capH, p.dim)
        label(c, sc.second.value, secondR.left + lw + gap, top, capH, color(sc.secondAhead))

        progress(c, sc.progress, barR, p.accent, p.faint)
        row(statsR, sc.stats.size.coerceAtLeast(1), gap).forEachIndexed { i, cell -> sc.stats.getOrNull(i)?.let { stat(c, p, it, cell) } }
    }

    /** Newest first: what was ridden, the time large, power and gaps beside it. */
    private fun results(c: Canvas, m: FieldModel, rc: ResultsContent, r: RectF) {
        val p = m.palette
        if (rc.rows.isEmpty()) return message(c, m, "Climbs and segments you\nfinish show up here", r)
        val n = 4
        val gap = r.height() * 0.025f
        val rowH = (r.height() - gap * (n - 1)) / n
        rc.rows.take(n).forEachIndexed { i, row ->
            val rr = RectF(r.left, r.top + i * (rowH + gap), r.right, r.top + i * (rowH + gap) + rowH)
            card(c, rr, p.card, rowH * 0.14f)
            val inner = RectF(rr.left + rr.width() * 0.03f, rr.top + rowH * 0.1f, rr.right - rr.width() * 0.03f, rr.bottom - rowH * 0.1f)
            label(c, row.title, inner.left, inner.top, rowH * 0.2f, p.dim, maxWidth = inner.width())
            val color = when (row.good) {
                true -> p.good
                false -> p.bad
                null -> p.text
            }
            val mainW = number(c, row.main, "", RectF(inner.left, inner.top + rowH * 0.3f, inner.left + inner.width() * 0.38f, inner.bottom), color, p.dim, 0.85f)
            label(c, row.detail, inner.left + mainW + inner.width() * 0.04f, inner.top + rowH * 0.42f, rowH * 0.22f, p.text, maxWidth = inner.width() - mainW - inner.width() * 0.04f)
        }
    }

    /** Eaten, burned and the balance; the target bar; when to eat and drink next; what a tap logs. */
    private fun fuel(c: Canvas, m: FieldModel, fc: FuelContent, r: RectF) {
        val p = m.palette
        val rows = splitV(r, 0.1f, 0.3f, 0.06f, 0.2f, 0.12f, 0.1f, gap = r.height() * 0.025f)
        val (topR, statsR, barR, eatR, drinkR) = rows
        val hintR = rows[5]
        label(c, fc.title, topR.left, topR.top + topR.height() * 0.19f, topR.height() * 0.62f, p.text, maxWidth = topR.width())
        val cells = row(statsR, 3, r.width() * 0.03f)
        stat(c, p, fc.eaten, cells[0])
        stat(c, p, fc.burned, cells[1])
        stat(c, p, fc.balance.copy(accent = if (fc.onTrack) p.good else p.warn), cells[2])
        progress(c, fc.targetFraction, barR, if (fc.onTrack) p.good else p.warn, p.faint)
        label(c, fc.eat, eatR.left, eatR.top + eatR.height() * 0.12f, eatR.height() * 0.7f, if (fc.eatDue) p.warn else p.text, maxWidth = eatR.width())
        label(c, fc.drink, drinkR.left, drinkR.top + drinkR.height() * 0.15f, drinkR.height() * 0.62f, if (fc.drinkDue) p.warn else p.dim, maxWidth = drinkR.width())
        label(c, fc.hint, hintR.centerX(), hintR.top + hintR.height() * 0.15f, hintR.height() * 0.6f, p.accent, Paint.Align.CENTER, hintR.width())
    }

    /** Like the Workout panel: this stretch's target with PUSH/EASE, a gauge, the plan's numbers and the next change. */
    private fun pace(c: Canvas, m: FieldModel, pc: PaceContent, r: RectF) {
        val p = m.palette
        pc.message?.let { return message(c, m, it, r) }
        val color = deviationColor(pc.output, pc.low, pc.high, pc.status, p)
        val (lineR, targetR, gaugeR, statsR, nextR) = splitV(r, 0.11f, 0.3f, 0.09f, 0.3f, 0.11f, gap = r.height() * 0.025f)
        label(c, pc.title, lineR.left, lineR.top + lineR.height() * 0.15f, lineR.height() * 0.62f, p.text, maxWidth = lineR.width())

        val radius = targetR.height() * 0.14f
        card(c, targetR, p.card, radius)
        c.save()
        c.clipRect(targetR.left, targetR.top, targetR.left + targetR.width() * 0.02f, targetR.bottom)
        c.drawRoundRect(targetR, radius, radius, fill(color))
        c.restore()
        val statusW = targetR.width() * 0.3f
        number(c, pc.target, "W", RectF(targetR.left + targetR.width() * 0.05f, targetR.top + targetR.height() * 0.12f, targetR.right - statusW, targetR.bottom - targetR.height() * 0.12f), p.text, p.dim, 0.82f, unitScale = 0.25f)
        label(c, statusLabel(pc.status), targetR.right - targetR.width() * 0.03f, targetR.centerY() - targetR.height() * 0.13f, targetR.height() * 0.26f, color, Paint.Align.RIGHT, statusW)

        gauge(c, pc.low, pc.high, pc.output, gaugeR, p, color)
        row(statsR, pc.stats.size.coerceAtLeast(1), r.width() * 0.03f).forEachIndexed { i, cell -> pc.stats.getOrNull(i)?.let { stat(c, p, it, cell) } }
        label(c, pc.next, nextR.left, nextR.top + nextR.height() * 0.15f, nextR.height() * 0.6f, p.dim, maxWidth = nextR.width())
    }

    /** Climb grades as riders know them from climb profiles: green, yellow, orange, red, dark red. */
    private fun gradeColor(grade: Float): Int = when {
        grade >= 12 -> 0xFF9B0E2A.toInt()
        grade >= 9 -> 0xFFFF3B30.toInt()
        grade >= 6 -> 0xFFFF8A00.toInt()
        grade >= 3 -> 0xFFFFC400.toInt()
        else -> 0xFF00C46A.toInt()
    }

    /** Status and age on top, the message as large as it fits in a card, then tap to confirm. */
    private fun coach(c: Canvas, m: FieldModel, cc: CoachContent, r: RectF) {
        val p = m.palette
        val (topR, cardR, hintR) = splitV(r, 0.12f, 0.76f, 0.12f, gap = r.height() * 0.025f)
        val statusColor = if (cc.warning) p.warn else p.good
        val capH = topR.height() * 0.62f
        val top = topR.top + (topR.height() - capH) / 2
        val dot = capH * 0.36f
        c.drawCircle(topR.left + dot, top + capH / 2, dot, fill(statusColor))
        val ageW = label(c, cc.age, topR.right, top, capH, p.dim, Paint.Align.RIGHT)
        label(c, cc.status, topR.left + dot * 3f, top, capH, statusColor, maxWidth = topR.width() - dot * 3f - ageW - topR.height())

        val radius = cardR.height() * 0.08f
        card(c, cardR, p.card, radius)
        if (cc.unread) {
            c.save()
            c.clipRect(cardR.left, cardR.top, cardR.left + cardR.width() * 0.02f, cardR.bottom)
            c.drawRoundRect(cardR, radius, radius, fill(p.accent))
            c.restore()
        }
        val inner = RectF(cardR.left + cardR.width() * 0.05f, cardR.top + cardR.height() * 0.08f, cardR.right - cardR.width() * 0.04f, cardR.bottom - cardR.height() * 0.06f)
        var textR = inner
        cc.title?.let { title ->
            val titleH = inner.height() * 0.1f
            label(c, title, inner.left, inner.top, titleH, p.dim, maxWidth = inner.width())
            textR = RectF(inner.left, inner.top + titleH * 2f, inner.right, inner.bottom)
        }
        val color = if (cc.placeholder || !cc.unread) p.dim else p.text
        paragraph(c, cc.text, textR, color, maxCap = if (cc.placeholder) textR.height() * 0.1f else textR.height() * 0.2f, minCap = textR.height() * 0.07f)

        if (cc.hint.isNotEmpty()) {
            label(c, cc.hint, hintR.centerX(), hintR.top + hintR.height() * 0.15f, hintR.height() * 0.6f, if (cc.unread) p.accent else p.dim, Paint.Align.CENTER)
        }
    }

    /** Free text wrapped and sized to fill [r], centered vertically, left-aligned. */
    private fun paragraph(c: Canvas, text: String, r: RectF, color: Int, maxCap: Float, minCap: Float) {
        val atCap100 = textPaint(color, 100f / capRatio)
        val fitted = TextFit.fit(text, r.width(), r.height(), maxCap, minCap) { atCap100.measureText(it) / 100f }
        val cap = fitted.capHeight
        var y = r.top + (r.height() - TextFit.height(fitted.lines.size, cap)) / 2
        for (line in fitted.lines) {
            label(c, line, r.left, y, cap, color, maxWidth = r.width())
            y += cap * TextFit.LINE_PITCH
        }
    }

    // ---------------------------------------------------------------- overlay

    private fun overlay(c: Canvas, m: FieldModel, o: OverlayModel, r: RectF) {
        val p = m.palette
        val radius = min(r.width(), r.height()) * 0.05f
        c.drawRoundRect(r, radius, radius, fill(p.card))
        c.drawRoundRect(r, radius, radius, stroke(p.accent, max(3f, r.width() * 0.008f)))
        val inner = RectF(r.left + r.width() * 0.05f, r.top + r.height() * 0.06f, r.right - r.width() * 0.05f, r.bottom - r.height() * 0.06f)
        when (o) {
            is OverlayModel.NextStep -> {
                val (titleR, subR, numR, barR) = splitV(inner, 0.16f, 0.1f, 0.6f, 0.06f, gap = inner.height() * 0.02f)
                label(c, "NEXT · STEP ${o.nextStep}/${o.stepCount}", titleR.centerX(), titleR.top, titleR.height() * 0.75f, p.text, Paint.Align.CENTER, titleR.width())
                if (o.target != null) {
                    // The planned target large, the countdown beside the duration.
                    val sub = listOfNotNull(o.duration, "IN ${o.secondsLeft} S").joinToString(" · ")
                    label(c, sub, subR.centerX(), subR.top, subR.height() * 0.7f, p.dim, Paint.Align.CENTER, subR.width())
                    number(c, o.target, "W", numR, p.accent, p.dim, 0.85f, Paint.Align.CENTER, unitScale = 0.25f)
                } else {
                    label(c, "STARTS IN", subR.centerX(), subR.top, subR.height() * 0.7f, p.dim, Paint.Align.CENTER)
                    number(c, "${o.secondsLeft}", "", numR, p.accent, p.dim, 0.9f, Paint.Align.CENTER)
                }
                progress(c, o.secondsLeft / 10f, barR, p.accent, p.faint)
            }
            is OverlayModel.NewTarget -> {
                val (titleR, numR, subR) = splitV(inner, 0.18f, 0.6f, 0.12f, gap = inner.height() * 0.03f)
                label(c, o.title, titleR.centerX(), titleR.top, titleR.height() * 0.7f, p.text, Paint.Align.CENTER, titleR.width())
                number(c, o.target, o.unit, numR, p.good, p.dim, 0.85f, Paint.Align.CENTER, unitScale = 0.25f)
                label(c, "NEW TARGET", subR.centerX(), subR.top, subR.height() * 0.7f, p.dim, Paint.Align.CENTER)
            }
        }
    }

    private companion object {
        const val DIGITS = "0123456789"
    }
}
