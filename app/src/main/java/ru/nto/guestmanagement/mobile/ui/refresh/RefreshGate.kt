package ru.nto.guestmanagement.mobile.ui.refresh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RefreshGate {
    private val lock = Any()
    private var running: Job? = null

    fun launch(scope: CoroutineScope, block: suspend () -> Unit): Job? = synchronized(lock) {
        if (running?.isActive == true) return null
        val job = scope.launch(start = CoroutineStart.LAZY) { block() }
        running = job
        job.invokeOnCompletion {
            synchronized(lock) {
                if (running === job) running = null
            }
        }
        job.start()
        job
    }

    fun cancel() {
        synchronized(lock) {
            val job = running
            running = null
            job?.cancel()
        }
    }
}
