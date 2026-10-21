package io.github.mschache.karoopro.karoo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Stream a Karoo data type until the collector goes away. */
fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> = callbackFlow {
    val id = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
        trySend(event.state)
    }
    awaitClose { removeConsumer(id) }
}

/** Collect a Karoo event type that has default params (RideState, UserProfile, Lap, ...). */
inline fun <reified T : KarooEvent> KarooSystemService.consumerFlow(): Flow<T> = callbackFlow {
    val id = addConsumer<T> { trySend(it) }
    awaitClose { removeConsumer(id) }
}

/** The value of [field] in a streaming state, falling back to the only value for single-field types. */
fun StreamState?.field(field: String?): Double? {
    val point: DataPoint = (this as? StreamState.Streaming)?.dataPoint ?: return null
    val value = if (field != null) point.values[field] else null
    return (value ?: if (point.values.size == 1) point.singleValue else null)?.takeIf { it.isFinite() }
}

val StreamState?.isStreaming: Boolean get() = this is StreamState.Streaming
