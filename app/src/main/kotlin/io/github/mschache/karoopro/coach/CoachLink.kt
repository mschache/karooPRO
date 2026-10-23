package io.github.mschache.karoopro.coach

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import io.github.mschache.karoopro.EXTENSION_ID
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.ui.field.ProFieldDataType
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Talks to the Karoo Coach extension, when it is installed: reads its latest message and status,
 * follows changes with a ContentObserver, sends acks, tells it whether the KarooPRO field
 * shows coach messages (so Karoo Coach checks in for KarooPRO profiles and skips its pop-up), and
 * sends it the [effort] values for its check-ins while they run.
 */
class CoachLink(
    private val app: Context,
    private val settings: StateFlow<Settings>,
    private val scope: CoroutineScope,
    /** [CoachEffort.values] for the ride right now. */
    private val effort: () -> Map<String, Double>,
) {
    private val uri = Uri.parse(CoachContract.STATE_URI)
    private val fieldId = DataType.dataTypeId(EXTENSION_ID, ProFieldDataType.TYPE_ID)
    private val _state = MutableStateFlow<CoachInfo?>(null)

    /** Null while Karoo Coach is missing or can't be read. */
    val state: StateFlow<CoachInfo?> = _state

    /** Karoo Coach is installed and set up with a relay, so the Coach panel is offered. */
    val available: Boolean get() = _state.value?.configured == true

    private var observing = false
    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            scope.launch { refresh() }
        }
    }

    fun start() {
        scope.launch {
            while (isActive) {
                // The observer brings changes right away; this loop catches installs, removals and restarts.
                observe()
                refresh()
                announce(settings.value)
                delay(REFRESH_MS)
            }
        }
        scope.launch {
            settings.map { hostFlags(it) }.distinctUntilChanged().drop(1).collect { announce(settings.value) }
        }
        scope.launch {
            while (isActive) {
                delay(EFFORT_MS)
                if (_state.value?.active != true) continue
                val values = effort().takeIf { it.isNotEmpty() } ?: continue
                call(CoachContract.METHOD_EFFORT, null, Bundle().apply { values.forEach { (k, v) -> putDouble(k, v) } })
            }
        }
    }

    /** Confirms the shown message, as a tap on the Coach field would. */
    fun acknowledge() {
        val id = _state.value?.message?.id ?: return
        _state.update { it?.copy(acknowledged = true) }
        scope.launch {
            call(CoachContract.METHOD_ACK, id, null)
            refresh()
        }
    }

    private fun observe() {
        if (observing) return
        observing = try {
            app.contentResolver.registerContentObserver(uri, false, observer)
            true
        } catch (e: SecurityException) {
            false // Karoo Coach is not installed (or too old to share its state).
        }
    }

    private suspend fun refresh() {
        val info = withContext(Dispatchers.IO) {
            try {
                app.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) CoachInfo.from(c.row()) else null }
            } catch (e: Exception) {
                null
            }
        }
        if (info != _state.value) Log.i(TAG, "Karoo Coach: ${info?.let { "configured=${it.configured} active=${it.active} message=${it.message?.id}" } ?: "unavailable"}")
        _state.value = info
    }

    /** Karoo Coach ignores repeats, so this is sent on every refresh and survives its data being reset. */
    private suspend fun announce(s: Settings) {
        if (_state.value == null) return
        val (shows, opens) = hostFlags(s)
        call(CoachContract.METHOD_HOST, fieldId, Bundle().apply {
            putBoolean(CoachContract.EXTRA_SHOWS, shows)
            putBoolean(CoachContract.EXTRA_OPENS, opens)
        })
    }

    private fun hostFlags(s: Settings): Pair<Boolean, Boolean> {
        val shows = s.orderedPanels.any { it.panel == Panel.COACH && it.enabled }
        return shows to (shows && s.autoOpenCoach)
    }

    private suspend fun call(method: String, arg: String?, extras: Bundle?) = withContext(Dispatchers.IO) {
        try {
            app.contentResolver.call(uri, method, arg, extras)
        } catch (e: Exception) {
            Log.w(TAG, "Karoo Coach $method failed: ${e.message}")
            null
        }
    }

    private fun Cursor.row(): Map<String, Any?> = columnNames.indices.associate { i ->
        columnNames[i] to when (getType(i)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(i)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
            Cursor.FIELD_TYPE_STRING -> getString(i)
            else -> null
        }
    }

    private companion object {
        const val TAG = "KarooPRO"
        const val REFRESH_MS = 30_000L
        /** Karoo Coach drops values older than 20 s. */
        const val EFFORT_MS = 5_000L
    }
}
