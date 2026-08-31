package com.example.momentra.ui.shell.perf

/**
 * S9-J lightweight shell performance marks.
 * On device: android.util.Log. In JVM unit tests Log stubs throw — swallowed.
 */
object ShellPerf {
    private const val TAG = "MomentraPerf"

    data class Mark(
        val name: String,
        val startedAtMs: Long = System.currentTimeMillis(),
    )

    fun start(name: String): Mark = Mark(name)

    fun end(mark: Mark, extras: Map<String, Any?> = emptyMap()): Long {
        val elapsed = System.currentTimeMillis() - mark.startedAtMs
        val payload = buildString {
            append("event=").append(mark.name)
            append(" elapsedMs=").append(elapsed)
            extras.forEach { (k, v) ->
                if (v != null) append(' ').append(k).append('=').append(v)
            }
        }
        emit(payload)
        last[mark.name] = elapsed
        return elapsed
    }

    fun instant(name: String, extras: Map<String, Any?> = emptyMap()) {
        end(Mark(name, System.currentTimeMillis()), extras)
    }

    val last: MutableMap<String, Long> = mutableMapOf()

    fun clear() {
        last.clear()
    }

    private fun emit(payload: String) {
        try {
            android.util.Log.i(TAG, payload)
        } catch (_: Throwable) {
            // Unit-test JVM stubs throw for Log.*; marks still recorded in `last`.
        }
    }
}
