package es.unizar.urlshortener.core.queues

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener

class CoroutineScopeManager {
    private val supervisorJob = SupervisorJob()

    val applicationScope: CoroutineScope = CoroutineScope(supervisorJob + Dispatchers.Default)

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        supervisorJob.cancel()
    }
}
