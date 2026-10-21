package io.github.mschache.karoopro.karoo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HttpResponseState
import io.hammerhead.karooext.models.OnHttpResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class HttpException(message: String) : Exception(message)

/**
 * GET through the Karoo: it uses WiFi when connected, otherwise it tunnels over Bluetooth via
 * the Hammerhead Companion app. Bodies are capped at [OnHttpResponse.MAX_REQUEST_SIZE] (100 KB),
 * so callers must ask for small payloads.
 */
suspend fun KarooSystemService.httpGet(
    url: String,
    headers: Map<String, String> = emptyMap(),
    timeoutMs: Long = 30_000,
): ByteArray {
    val complete = withTimeout(timeoutMs) {
        callbackFlow {
            val id = addConsumer(
                OnHttpResponse.MakeHttpRequest("GET", url, headers, body = null, waitForConnection = false),
                onError = { close(HttpException(it)) },
            ) { event: OnHttpResponse -> trySend(event.state) }
            awaitClose { removeConsumer(id) }
        }.filterIsInstance<HttpResponseState.Complete>().first()
    }
    if (complete.error != null) throw HttpException(complete.error!!)
    if (complete.statusCode !in 200..299) throw HttpException("HTTP ${complete.statusCode}")
    return complete.body ?: throw HttpException("empty body")
}
