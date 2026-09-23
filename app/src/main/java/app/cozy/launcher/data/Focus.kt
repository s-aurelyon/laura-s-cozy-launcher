package app.cozy.launcher.data

import app.cozy.launcher.system.Alarms

/** The focus timer. Keeps its end time so it carries on while the app is closed. */
object Focus {
    const val FOCUS = "focus"
    const val SHORT = "short"
    const val LONG = "long"

    private val state get() = Store.timer.value
    private fun set(s: TimerState) { Store.timer.value = s }

    fun minutesFor(mode: String): Int {
        val s = Store.settings.value
        return when (mode) {
            SHORT -> s.shortMinutes
            LONG -> s.longMinutes
            else -> s.focusMinutes
        }
    }

    fun label(mode: String) = when (mode) {
        SHORT -> "Short break"
        LONG -> "Long break"
        else -> "Focus"
    }

    fun isRunning(s: TimerState = state) = s.endAt != null

    fun remainingMs(s: TimerState = state, now: Long = System.currentTimeMillis()): Long =
        s.endAt?.let { (it - now).coerceAtLeast(0L) } ?: (s.remainingSec * 1000L)

    fun setMode(mode: String) {
        Alarms.cancelTimer(Store.app)
        val sec = minutesFor(mode) * 60
        set(state.copy(mode = mode, totalSec = sec, remainingSec = sec, endAt = null, finishedMode = null))
    }

    fun setMinutes(min: Int) {
        Alarms.cancelTimer(Store.app)
        val sec = min.coerceIn(1, 240) * 60
        set(state.copy(totalSec = sec, remainingSec = sec, endAt = null, finishedMode = null))
    }

    fun start() {
        val s = state
        val remaining = if (s.remainingSec <= 0) s.totalSec else s.remainingSec
        val end = System.currentTimeMillis() + remaining * 1000L
        set(s.copy(remainingSec = remaining, endAt = end, finishedMode = null))
        Alarms.scheduleTimer(Store.app, end, label(s.mode))
    }

    fun pause() {
        val s = state
        val left = ((remainingMs(s) + 999) / 1000).toInt()
        set(s.copy(remainingSec = left, endAt = null))
        Alarms.cancelTimer(Store.app)
    }

    fun reset() {
        Alarms.cancelTimer(Store.app)
        set(state.copy(remainingSec = state.totalSec, endAt = null, finishedMode = null))
    }

    fun setFocusingOn(text: String) = set(state.copy(focusingOn = text))

    /** Called about once a second. Moves on to the next session when time runs out. */
    fun tick(now: Long = System.currentTimeMillis()) {
        val s = state
        val end = s.endAt ?: return
        if (now < end) return
        val finished = s.mode
        val session = if (finished == FOCUS) s.session + 1 else s.session
        val next = when {
            finished != FOCUS -> FOCUS
            session % 4 == 0 -> LONG
            else -> SHORT
        }
        val sec = minutesFor(next) * 60
        set(
            s.copy(
                mode = next,
                totalSec = sec,
                remainingSec = sec,
                endAt = null,
                session = if (finished == LONG) 0 else session,
                finishedMode = finished,
            )
        )
        if (finished == FOCUS && Store.settings.value.autoStartBreaks) start()
    }
}
