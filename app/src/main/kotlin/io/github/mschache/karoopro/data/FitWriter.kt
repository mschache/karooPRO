package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.weather.WeatherRepository
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DeveloperField
import io.hammerhead.karooext.models.FieldValue
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Adds KarooPro's own values to the ride's FIT file as developer fields, so they show up in
 * analysis afterwards (intervals.icu shows developer fields): every second W' balance, carbs burned
 * and eaten, stamina and headwind; the carb totals also go to the session.
 */
class FitWriter(
    private val hub: RideDataHub,
    private val recorder: RideRecorder,
    private val weather: WeatherRepository,
    private val scope: CoroutineScope,
) {
    fun run(emitter: Emitter<FitEffect>): Job = scope.launch {
        var ticks = 0
        while (isActive) {
            delay(1000)
            when (hub.rideState.value) {
                is RideState.Recording -> {
                    record().takeIf { it.isNotEmpty() }?.let { emitter.onNext(WriteToRecordMesg(it)) }
                    if (++ticks % SESSION_EVERY_S == 0) emitter.onNext(WriteToSessionMesg(session()))
                }
                // Paused (often just before the ride ends): keep the session totals current.
                is RideState.Paused -> emitter.onNext(WriteToSessionMesg(session()))
                else -> Unit
            }
        }
    }

    private fun record(): List<FieldValue> = buildList {
        hub.value(Metric.W_PRIME)?.let { add(FieldValue(W_PRIME, it / 1000)) }
        add(FieldValue(CARBS_BURNED, hub.value(Metric.CARBS_BURNED) ?: 0.0))
        add(FieldValue(CARBS_EATEN, hub.value(Metric.CARBS_EATEN) ?: 0.0))
        hub.value(Metric.STAMINA)?.let { add(FieldValue(STAMINA, it)) }
        weather.headwind()?.let { add(FieldValue(HEADWIND, it * 3.6)) }
    }

    private fun session(): List<FieldValue> {
        val fuel = recorder.read { fuel.state }
        return listOf(FieldValue(CARBS_BURNED, fuel.burnedG), FieldValue(CARBS_EATEN, fuel.eatenG))
    }

    private companion object {
        const val SESSION_EVERY_S = 60
        /** FIT base type float32. */
        const val FLOAT32: Short = 136

        // Field definition numbers are this extension's own, sequential from 0.
        val W_PRIME = DeveloperField(0, FLOAT32, "W Prime Balance", "kJ")
        val CARBS_BURNED = DeveloperField(1, FLOAT32, "Carbs Burned", "g")
        val CARBS_EATEN = DeveloperField(2, FLOAT32, "Carbs Eaten", "g")
        val STAMINA = DeveloperField(3, FLOAT32, "Stamina", "%")
        val HEADWIND = DeveloperField(4, FLOAT32, "Headwind", "km/h")
    }
}
