package me.rerere.rikkahub.service

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.model.Conversation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.uuid.Uuid

class ConversationSessionTest {
    @Test
    fun cancelledPredecessorCannotClearSuccessor() = runTest {
        val id = Uuid.random()
        val session = ConversationSession(id, Conversation.ofId(id), backgroundScope, {})
        val previous = backgroundScope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { delay(100) }
            }
        }
        runCurrent()
        session.setJob(previous)
        val successor = backgroundScope.launch { awaitCancellation() }
        session.setJob(successor)
        testScheduler.advanceTimeBy(101)
        runCurrent()
        assertSame(successor, session.getJob())
        session.cleanup()
    }

    @Test
    fun lowPriorityTaskCannotReplaceActiveGeneration() = runTest {
        val id = Uuid.random()
        val session = ConversationSession(id, Conversation.ofId(id), backgroundScope, {})
        val active = backgroundScope.launch { awaitCancellation() }
        session.setJob(active)
        val candidate = backgroundScope.launch(start = CoroutineStart.LAZY) { awaitCancellation() }
        assertFalse(session.tryClaimGeneration(candidate))
        assertSame(active, session.getJob())
        candidate.cancel()
        session.cleanup()
    }
}