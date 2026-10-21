package io.github.mschache.karoopro.numeric

import android.content.Context
import io.github.mschache.karoopro.AppGraph
import io.github.mschache.karoopro.EXTENSION_ID
import io.github.mschache.karoopro.data.Metric
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A plain Karoo number fed once per second from a KarooPRO computed value. */
abstract class ComputedNumber(
    protected val graph: AppGraph,
    typeId: String,
    /** Built-in type whose precision and unit conversion the Karoo applies. */
    private val formatAs: String,
) : DataTypeImpl(EXTENSION_ID, typeId) {

    abstract fun value(): Double?

    open fun hold(scope: CoroutineScope): Job? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        hold(scope)
        scope.launch {
            emitter.onNext(StreamState.Searching)
            while (isActive) {
                val v = value()
                emitter.onNext(
                    if (v == null) StreamState.Searching
                    else StreamState.Streaming(DataPoint(dataTypeId, mapOf(DataType.Field.SINGLE to v))),
                )
                delay(1_000)
            }
        }
        emitter.setCancellable { scope.cancel() }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateNumericConfig(formatDataTypeId = formatAs))
    }
}

/** W' balance in kJ with two decimals. */
class WPrimeDataType(graph: AppGraph) : ComputedNumber(graph, "wprime", DataType.Type.INTENSITY_FACTOR) {
    override fun value(): Double? = graph.hub.value(Metric.W_PRIME)?.div(1000)
}

/** Head (+) or tail (-) wind component, shown in the rider's speed unit. */
class HeadwindDataType(graph: AppGraph) : ComputedNumber(graph, "headwind", DataType.Type.SPEED) {
    override fun value(): Double? = graph.weather.headwind()

    override fun hold(scope: CoroutineScope): Job = scope.launch {
        val weather = graph.weather.acquire()
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            weather.cancel()
        }
    }
}
