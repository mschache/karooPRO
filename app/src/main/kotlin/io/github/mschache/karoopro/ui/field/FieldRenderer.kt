package io.github.mschache.karoopro.ui.field

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.RemoteViews
import io.github.mschache.karoopro.R
import io.github.mschache.karoopro.graph

/** Turns painted bitmaps into RemoteViews for the Karoo ride app (and the in-app preview). */
object FieldRenderer {
    @Volatile private var painter: FieldPainter? = null

    fun painter(context: Context): FieldPainter =
        painter ?: synchronized(this) { painter ?: FieldPainter(context.applicationContext).also { painter = it } }

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** The Pro field: the image plus tap zones (none in the page editor, where taps belong to the editor). */
    fun field(context: Context, model: FieldModel, widthPx: Int, heightPx: Int): RemoteViews {
        val bitmap = painter(context).paint(model, widthPx, heightPx)
        if (!model.interactive) return image(context, bitmap)
        return RemoteViews(context.packageName, R.layout.field).apply {
            setImageViewBitmap(R.id.field_image, bitmap)
            setOnClickPendingIntent(R.id.tap_hero, action(context, FieldActionReceiver.CYCLE_METRIC))
            setOnClickPendingIntent(R.id.tap_previous, action(context, FieldActionReceiver.PREVIOUS_PANEL))
            setOnClickPendingIntent(R.id.tap_next, action(context, FieldActionReceiver.NEXT_PANEL))
        }
    }

    fun image(context: Context, bitmap: Bitmap): RemoteViews =
        RemoteViews(context.packageName, R.layout.field_image).apply { setImageViewBitmap(R.id.field_image, bitmap) }

    private fun action(context: Context, action: String): PendingIntent {
        val intent = Intent(context, FieldActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

/** Receives taps on the field's tap zones. */
class FieldActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = context.graph
        when (intent.action) {
            CYCLE_METRIC -> graph.cycleBigMetric()
            NEXT_PANEL -> graph.panels.step(1)
            PREVIOUS_PANEL -> graph.panels.step(-1)
        }
    }

    companion object {
        const val CYCLE_METRIC = "io.github.mschache.karoopro.CYCLE_METRIC"
        const val NEXT_PANEL = "io.github.mschache.karoopro.NEXT_PANEL"
        const val PREVIOUS_PANEL = "io.github.mschache.karoopro.PREVIOUS_PANEL"
    }
}
