package io.github.mschache.karoopro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mschache.karoopro.alerts.Beep
import io.hammerhead.karooext.models.ZoomPage

/**
 * adb shell am broadcast -a io.github.mschache.karoopro.debug.ALERT -n io.github.mschache.karoopro/.DebugReceiver
 *   [--ei zoomout N | --ei zoomin N]   zoom the visible map page instead of firing the alert
 *   [--es demoroute "lat,lng" | --es demoroute off]   a synthetic 12 km loop with three climbs for the map
 *     layers and the Climb panel; the ride distance moves the rider round it
 *   [--es demoalong 1850]   put the rider that many meters along the demo route
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val g = context.graph
        intent.getStringExtra("demoroute")?.let { arg ->
            val ll = arg.split(",").mapNotNull { it.trim().toDoubleOrNull() }
            if (ll.size == 2) g.routes.showDemoRoute(ll[0], ll[1]) else g.routes.hideDemoRoute()
            return
        }
        intent.getStringExtra("demoalong")?.toDoubleOrNull()?.let {
            g.routes.setDemoAlong(it)
            return
        }
        val zoomOut = intent.getIntExtra("zoomout", 0)
        val zoomIn = intent.getIntExtra("zoomin", 0)
        if (zoomOut > 0 || zoomIn > 0) {
            repeat(zoomOut) { g.karoo.dispatch(ZoomPage(zoomIn = false)) }
            repeat(zoomIn) { g.karoo.dispatch(ZoomPage(zoomIn = true)) }
            return
        }
        g.alerts.beep(Beep.NEXT_SOON)
        g.alerts.popup("karoopro-test", "Next: step 3/8", "in 10 s", 8_000)
    }
}
