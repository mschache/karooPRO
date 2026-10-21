package io.github.mschache.karoopro.karoo

import android.os.DeadObjectException
import android.util.Log
import android.widget.RemoteViews
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ViewEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * karoo-ext silently drops any view sent less than 900 ms after the previous one, which can leave
 * the last frame of a burst unpainted. This keeps only the newest frame and sends it once the
 * window has passed. Pattern from ki2's ThrottledViewEmitter (MIT).
 */
class ThrottledViewEmitter(private val emitter: ViewEmitter) : Emitter<ViewEvent> by emitter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val views = Channel<RemoteViews>(Channel.CONFLATED)

    init {
        scope.launch {
            for (view in views) {
                try {
                    emitter.updateView(view)
                } catch (e: DeadObjectException) {
                    // The Karoo system process died; it calls startView again when it is back.
                    Log.w(TAG, "Karoo system gone, stopping view updates", e)
                    emitter.cancel()
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "updateView failed", e)
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    fun updateView(view: RemoteViews) {
        views.trySend(view)
    }

    override fun setCancellable(cancellable: () -> Unit) {
        emitter.setCancellable {
            stop()
            cancellable()
        }
    }

    override fun onComplete() {
        stop()
        emitter.onComplete()
    }

    private fun stop() {
        views.close()
        scope.cancel()
    }

    private companion object {
        const val TAG = "KarooPRO"
        const val UPDATE_INTERVAL_MS = 1_000L
    }
}
