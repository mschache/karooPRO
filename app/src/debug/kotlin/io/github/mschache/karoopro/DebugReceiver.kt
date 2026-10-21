package io.github.mschache.karoopro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mschache.karoopro.alerts.Beep

/** adb shell am broadcast -a io.github.mschache.karoopro.debug.ALERT -n io.github.mschache.karoopro/.DebugReceiver */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val g = context.graph
        g.alerts.beep(Beep.NEXT_SOON)
        g.alerts.popup("karoopro-test", "Next: step 3/8", "in 10 s", 8_000)
    }
}
