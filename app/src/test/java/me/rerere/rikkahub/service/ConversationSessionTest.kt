package me.rerere.rikkahub.service

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.model.Conversation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.uuid.Uuid

class ConversationSessionTest {
    @Test
    fun cancelledPredecessorCannotClearSuccessor() = runBlocking {
        val id = Uuid.random()
        val session = ConversationSession(id, Conversation.ofId(id), this, {})
        val previous = launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { delay(100) }
            }
        }
        session.setJob(previous)
        val successor = launch { awaitCancellation() }
        session.setJob(successor)
        delay(101)
        assertSame(successor, session.getJob())
        session.cleanup()
    }

    @Test
    fun lowPriorityTaskCannotReplaceActiveGeneration() = runBlocking {
        val id = Uuid.random()
        val session = ConversationSession(id, Conversation.ofId(id), this, {})
        val active = launch { awaitCancellation() }
        session.setJob(active)
        val candidate = launch(start = CoroutineStart.LAZY) { awaitCancellation() }
        assertFalse(session.tryClaimGeneration(candidate))
        assertSame(active, session.getJob())
        candidate.cancel()
        session.cleanup()
    }
}