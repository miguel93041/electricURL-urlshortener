package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickRepositoryService
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import kotlin.test.Test

class LogClickUseCaseTest {

    @Test
    fun `logClick fails silently`() {
        val repository = mock<ClickRepositoryService> ()
        whenever(repository.save(any())).thenReturn(Mono.error(RuntimeException()))

        val useCase = LogClickUseCaseImpl(repository)

        useCase.logClick("key")
        verify(repository).save(any())
    }
}

