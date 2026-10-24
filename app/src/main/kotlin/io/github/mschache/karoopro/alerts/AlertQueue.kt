package io.github.mschache.karoopro.alerts

import io.github.mschache.karoopro.R
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.KarooEffect
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.PlayBeepPattern.Tone
import io.hammerhead.karooext.models.TurnScreenOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

enum class Beep { TICK, NEXT_SOON, STEP_START, GEAR_TOP, GEAR_BOTTOM, FUEL }

/**
 * Sends beeps and pop-ups to the Karoo one after another. Old items are dropped when the queue
 * backs up, so a late countdown never plays after the step it announced.
 */
class AlertQueue(private val karoo: KarooSystemService, scope: CoroutineScope) {
    private val effects = Channel<KarooEffect>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            for (effect in effects) karoo.dispatch(effect)
        }
    }

    fun beep(beep: Beep) {
        effects.trySend(PlayBeepPattern(tones(beep, karoo.hardwareType == HardwareType.K2)))
    }

    fun popup(id: String, title: String, detail: String?, durationMs: Long) {
        effects.trySend(TurnScreenOn)
        effects.trySend(
            InRideAlert(
                id = id,
                icon = R.drawable.ic_karoopro,
                title = title,
                detail = detail,
                autoDismissMs = durationMs,
                backgroundColor = R.color.alert_background,
                textColor = R.color.alert_text,
            ),
        )
    }

    companion object {
        /** Karoo 3 plays any tone; the Karoo 2 buzzer is only loud at 4-6 kHz with longer tones. */
        fun tones(beep: Beep, k2: Boolean): List<Tone> {
            val f = if (k2) 2 else 1
            val hz = { base: Int -> base * f }
            return when (beep) {
                Beep.TICK -> listOf(Tone(hz(2000), 120 * f))
                Beep.NEXT_SOON -> listOf(Tone(hz(2500), 160 * f), Tone(null, 100), Tone(hz(2500), 160 * f))
                Beep.STEP_START -> listOf(Tone(hz(3000), 450 * f))
                // End of the cassette: two short high blips at the smallest cog, two low ones at the largest.
                Beep.GEAR_TOP -> listOf(Tone(hz(3300), 70 * f), Tone(null, 50), Tone(hz(3300), 70 * f))
                Beep.GEAR_BOTTOM -> listOf(Tone(hz(1800), 70 * f), Tone(null, 50), Tone(hz(1800), 70 * f))
                Beep.FUEL -> listOf(Tone(hz(2200), 120 * f), Tone(null, 80), Tone(hz(2800), 120 * f), Tone(null, 80), Tone(hz(3300), 160 * f))
            }
        }
    }
}
