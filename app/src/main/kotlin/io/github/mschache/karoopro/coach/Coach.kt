package io.github.mschache.karoopro.coach

/** A message from Karoo Coach; [receivedAt] is when the Karoo got it (epoch ms). */
data class CoachMessage(val id: String, val title: String, val text: String, val receivedAt: Long)

/** Karoo Coach's shared state, read from its content provider (see [CoachContract]). */
data class CoachInfo(
    val configured: Boolean,
    /** Check-ins are running: set up, riding, and the ride profile shows the coach. */
    val active: Boolean,
    val riding: Boolean,
    val lastCheckInAt: Long?,
    val lastError: String?,
    val message: CoachMessage?,
    val acknowledged: Boolean,
) {
    val unread: Boolean get() = message != null && !acknowledged

    companion object {
        /** Parses one provider row given as column name to value; null when the row isn't usable. */
        fun from(row: Map<String, Any?>): CoachInfo? {
            if ((row.long(CoachContract.COL_VERSION) ?: return null) > CoachContract.VERSION) return null
            val id = row[CoachContract.COL_MESSAGE_ID] as? String
            val text = row[CoachContract.COL_MESSAGE_TEXT] as? String
            return CoachInfo(
                configured = row.flag(CoachContract.COL_CONFIGURED),
                active = row.flag(CoachContract.COL_ACTIVE),
                riding = row.flag(CoachContract.COL_RIDING),
                lastCheckInAt = row.long(CoachContract.COL_LAST_CHECK_IN_AT),
                lastError = row[CoachContract.COL_LAST_ERROR] as? String,
                message = if (id != null && text != null) {
                    CoachMessage(
                        id = id,
                        title = (row[CoachContract.COL_MESSAGE_TITLE] as? String)?.takeIf { it.isNotBlank() } ?: "Coach",
                        text = text,
                        receivedAt = row.long(CoachContract.COL_MESSAGE_AT) ?: 0L,
                    )
                } else {
                    null
                },
                acknowledged = row.flag(CoachContract.COL_ACKNOWLEDGED),
            )
        }

        private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()
        private fun Map<String, Any?>.flag(key: String): Boolean = long(key) == 1L
    }
}

enum class CoachStatus(val label: String, val warning: Boolean) {
    NOT_SET_UP("NOT SET UP", true),
    WAITING("WAITING FOR A RIDE", false),
    OFF("OFF FOR THIS PROFILE", true),
    CONNECTING("CONNECTING", false),
    LIVE("LIVE", false),
    OFFLINE("OFFLINE", true),
    ;

    companion object {
        /** Same rules as the Karoo Coach field, so both show the same status. */
        fun of(info: CoachInfo, now: Long): CoachStatus = when {
            !info.configured -> NOT_SET_UP
            !info.riding -> WAITING
            !info.active -> OFF
            info.lastError != null && (info.lastCheckInAt == null || now - info.lastCheckInAt > OFFLINE_AFTER_MS) -> OFFLINE
            info.lastCheckInAt != null -> LIVE
            else -> CONNECTING
        }

        private const val OFFLINE_AFTER_MS = 90_000L
    }
}

object CoachText {
    fun ago(ms: Long): String = when {
        ms < 60_000 -> "just now"
        ms < 3_600_000 -> "${ms / 60_000} min ago"
        else -> "${ms / 3_600_000} h ago"
    }
}

/** Decides when a coach message opens the Coach panel: once per message, while it's unread and fresh. */
class CoachInbox {
    private var seenId: String? = null

    fun isNew(info: CoachInfo?, now: Long): Boolean {
        val m = info?.message ?: return false
        if (m.id == seenId) return false
        seenId = m.id
        return !info.acknowledged && now - m.receivedAt < FRESH_MS
    }

    private companion object {
        /** Older unread messages (e.g. after KarooPRO restarts) stay on the panel without opening it. */
        const val FRESH_MS = 5 * 60_000L
    }
}

/** Karoo Coach's side of the link; mirrors CoachContract in the karoo-mcp extension. */
object CoachContract {
    const val PACKAGE = "io.github.mschache.karoocoach"
    const val STATE_URI = "content://io.github.mschache.karoocoach.state/state"
    const val VERSION = 1

    const val COL_VERSION = "version"
    const val COL_CONFIGURED = "configured"
    const val COL_ACTIVE = "active"
    const val COL_RIDING = "riding"
    const val COL_LAST_CHECK_IN_AT = "last_check_in_at"
    const val COL_LAST_ERROR = "last_error"
    const val COL_MESSAGE_ID = "message_id"
    const val COL_MESSAGE_TITLE = "message_title"
    const val COL_MESSAGE_TEXT = "message_text"
    const val COL_MESSAGE_AT = "message_at"
    const val COL_ACKNOWLEDGED = "acknowledged"

    const val METHOD_ACK = "ack"
    const val METHOD_HOST = "host"
    const val EXTRA_SHOWS = "shows"
    const val EXTRA_OPENS = "opens"
}
