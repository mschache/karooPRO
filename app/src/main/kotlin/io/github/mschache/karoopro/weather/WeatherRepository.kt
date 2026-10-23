package io.github.mschache.karoopro.weather

import android.util.Log
import io.github.mschache.karoopro.calc.Wind
import io.github.mschache.karoopro.calc.WindComponents
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.RideDataHub
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.karoo.field
import io.github.mschache.karoopro.karoo.httpGet
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fetches Open-Meteo weather while at least one consumer (Weather panel, Wind field, headwind field)
 * is active: every 15 minutes, or sooner after moving 3 km. Also tracks the direction of travel.
 */
enum class WeatherStatus { OFF, NO_POSITION, LOADING, OK, ERROR }

class WeatherRepository(
    private val hub: RideDataHub,
    private val settings: StateFlow<Settings>,
    private val scope: CoroutineScope,
) {
    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather
    private val _status = MutableStateFlow(WeatherStatus.LOADING)
    val status: StateFlow<WeatherStatus> = _status
    private val consumers = MutableStateFlow(0)
    private val count = AtomicInteger()

    @Volatile var heading: Double? = null
        private set

    fun start() {
        hub.registerComputed(Metric.HEADWIND) { headwind() }
        hub.registerComputed(Metric.WIND_SPEED) { _weather.value?.windSpeed }
        scope.launch {
            consumers.map { it > 0 }.distinctUntilChanged().collectLatest { active ->
                if (active) run()
            }
        }
    }

    /** Keep weather updating until the returned job is cancelled. */
    fun acquire(): Job = scope.launch {
        consumers.value = count.incrementAndGet()
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            consumers.value = count.decrementAndGet()
        }
    }

    /** Wind components in m/s for the current heading, from the weather already loaded (never fetches). */
    fun wind(): WindComponents? {
        val w = _weather.value ?: return null
        val h = heading ?: return null
        val speed = w.windSpeed ?: return null
        val from = w.windFromDeg ?: return null
        return Wind.components(speed, from, h)
    }

    /** Head (+) / tail (-) wind component in m/s for the current heading. */
    fun headwind(): Double? = wind()?.head

    private suspend fun run() {
        val location = hub.keepAlive(listOf(DataType.Type.LOCATION, DataType.Type.SPEED))
        try {
            var nextFetchAt = 0L
            while (scope.isActive) {
                val loc = hub.stream(DataType.Type.LOCATION).value
                val lat = loc.field(DataType.Field.LOC_LATITUDE)
                val lng = loc.field(DataType.Field.LOC_LONGITUDE)
                val speed = hub.value(Metric.SPEED) ?: 0.0
                // GPS bearing is only meaningful while moving.
                loc.field(DataType.Field.LOC_BEARING)?.takeIf { speed > 1.5 }?.let { heading = it }

                val now = System.currentTimeMillis()
                val last = _weather.value
                val moved = if (last != null && lat != null && lng != null) distanceM(last.latitude, last.longitude, lat, lng) else 0.0
                val due = last == null || now - last.fetchedAt > REFRESH_MS || moved > REFETCH_DISTANCE_M
                when {
                    !settings.value.weatherEnabled -> _status.value = WeatherStatus.OFF
                    lat == null || lng == null -> if (last == null) _status.value = WeatherStatus.NO_POSITION
                    due && now >= nextFetchAt -> {
                        if (last == null) _status.value = WeatherStatus.LOADING
                        nextFetchAt = try {
                            _weather.value = fetch(lat, lng, now)
                            _status.value = WeatherStatus.OK
                            now + 60_000
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w("KarooPRO", "Weather fetch failed: ${e.message}")
                            if (last == null) _status.value = WeatherStatus.ERROR
                            now + RETRY_MS
                        }
                    }
                }
                delay(2_000)
            }
        } finally {
            location.cancel()
        }
    }

    /** One Open-Meteo request through the Karoo's HTTP bridge. */
    suspend fun fetch(lat: Double, lng: Double, now: Long = System.currentTimeMillis()): WeatherData {
        val body = hub.karoo.httpGet(OpenMeteo.url(lat, lng), mapOf("User-Agent" to "KarooPRO"))
        return OpenMeteo.parse(body.decodeToString(), now)
    }

    companion object {
        private const val REFRESH_MS = 15 * 60_000L
        private const val RETRY_MS = 60_000L
        private const val REFETCH_DISTANCE_M = 3_000.0

        fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * r * asin(sqrt(a))
        }
    }
}
