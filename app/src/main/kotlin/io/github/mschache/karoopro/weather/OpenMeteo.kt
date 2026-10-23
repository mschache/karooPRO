package io.github.mschache.karoopro.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/** Current conditions plus the next hours; speeds in m/s, directions in degrees "from". */
data class WeatherData(
    val fetchedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val windSpeed: Double?,
    val windFromDeg: Double?,
    val gust: Double?,
    val temperature: Double?,
    val rainChance: Int?,
    val hours: List<HourForecast>,
)

data class HourForecast(
    val epochSeconds: Long,
    val windSpeed: Double?,
    val windFromDeg: Double?,
    val gust: Double?,
    val rainChance: Int?,
    val temperature: Double?,
)

@Serializable
internal data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val current: Current? = null,
    val hourly: Hourly? = null,
) {
    @Serializable
    data class Current(
        val time: Long = 0,
        @SerialName("temperature_2m") val temperature: Double? = null,
        @SerialName("wind_speed_10m") val windSpeed: Double? = null,
        @SerialName("wind_direction_10m") val windDirection: Double? = null,
        @SerialName("wind_gusts_10m") val gusts: Double? = null,
    )

    @Serializable
    data class Hourly(
        val time: List<Long> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
        @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
        @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList(),
        @SerialName("wind_gusts_10m") val gusts: List<Double?> = emptyList(),
        @SerialName("precipitation_probability") val rainChance: List<Int?> = emptyList(),
    )
}

/** Open-Meteo (open-meteo.com, CC BY 4.0) forecast endpoint, kept to a ~2 KB response. */
object OpenMeteo {
    private val json = Json { ignoreUnknownKeys = true }

    fun url(lat: Double, lng: Double): String = String.format(
        Locale.US,
        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f" +
            "&current=temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&hourly=temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,precipitation_probability" +
            "&forecast_hours=7&wind_speed_unit=ms&timeformat=unixtime&timezone=auto",
        lat,
        lng,
    )

    /** Several points in one request (Open-Meteo then answers with a JSON array); wind only, next 6 h. */
    fun routeUrl(points: List<Pair<Double, Double>>): String {
        val lats = points.joinToString(",") { String.format(Locale.US, "%.3f", it.first) }
        val lngs = points.joinToString(",") { String.format(Locale.US, "%.3f", it.second) }
        return "https://api.open-meteo.com/v1/forecast?latitude=$lats&longitude=$lngs" +
            "&current=wind_speed_10m,wind_direction_10m" +
            "&hourly=wind_speed_10m,wind_direction_10m&forecast_hours=6&wind_speed_unit=ms&timeformat=unixtime"
    }

    /**
     * Wind (speed m/s, direction from) per requested point, for the time each point will be reached:
     * [etaSeconds] per point. Uses current conditions when the ETA is within 30 minutes.
     */
    fun parseRoute(body: String, etaSeconds: List<Long>, nowMs: Long): List<Pair<Double, Double>?> {
        val trimmed = body.trimStart()
        val responses = if (trimmed.startsWith("[")) {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(OpenMeteoResponse.serializer()), body)
        } else {
            listOf(json.decodeFromString(OpenMeteoResponse.serializer(), body))
        }
        val nowS = nowMs / 1000
        return responses.mapIndexed { i, r ->
            val eta = etaSeconds.getOrElse(i) { nowS }
            val current = r.current?.let { c -> c.windSpeed?.let { s -> c.windDirection?.let { d -> s to d } } }
            if (eta - nowS < 1800 && current != null) return@mapIndexed current
            val h = r.hourly ?: return@mapIndexed current
            val idx = h.time.indices.minByOrNull { kotlin.math.abs(h.time[it] - eta) } ?: return@mapIndexed current
            val s = h.windSpeed.getOrNull(idx)
            val d = h.windDirection.getOrNull(idx)
            if (s != null && d != null) s to d else current
        }
    }

    fun parse(body: String, nowMs: Long): WeatherData {
        val r = json.decodeFromString(OpenMeteoResponse.serializer(), body)
        val h = r.hourly
        val hours = h?.time?.indices?.map { i ->
            HourForecast(
                epochSeconds = h.time[i],
                windSpeed = h.windSpeed.getOrNull(i),
                windFromDeg = h.windDirection.getOrNull(i),
                gust = h.gusts.getOrNull(i),
                rainChance = h.rainChance.getOrNull(i),
                temperature = h.temperature.getOrNull(i),
            )
        }.orEmpty()
        // The hour that contains "now" carries the rain chance for the current conditions.
        val nowS = nowMs / 1000
        val currentHour = hours.lastOrNull { it.epochSeconds <= nowS } ?: hours.firstOrNull()
        return WeatherData(
            fetchedAt = nowMs,
            latitude = r.latitude,
            longitude = r.longitude,
            windSpeed = r.current?.windSpeed,
            windFromDeg = r.current?.windDirection,
            gust = r.current?.gusts,
            temperature = r.current?.temperature,
            rainChance = currentHour?.rainChance,
            hours = hours.filter { it.epochSeconds > nowS }.take(6),
        )
    }
}
