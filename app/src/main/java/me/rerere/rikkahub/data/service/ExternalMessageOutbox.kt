package me.rerere.rikkahub.data.service

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.ExternalMemory
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.concurrent.TimeUnit

/** Durable upload intent. Never persist credentials, never redirect old jobs to a new destination. */
@Serializable
internal data class ExternalMessageUpload(
    val configId: String,
    val url: String,
    val table: String,
    val messageId: String,
    val assistantId: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: String,
)

object ExternalMessageOutbox {
    private const val TAG = "ExternalMessageOutbox"
    private val drainMutex = Mutex()
    private fun directory(context: Context) = File(context.noBackupFilesDir, "external-message-outbox").apply { mkdirs() }

    suspend fun enqueue(
        context: Context,
        configs: List<ExternalMemory>,
        assistantId: String,
        conversationId: String,
        message: UIMessage,
        role: String,
        content: String,
    ) = withContext(NonCancellable + Dispatchers.IO) {
        if (content.isBlank()) return@withContext
        drainMutex.withLock {
        configs.forEach { config ->
            val upload = ExternalMessageUpload(
                config.id.toString(), config.supabaseUrl.trimEnd('/'), config.tableName,
                message.id.toString(), assistantId, conversationId, role, content,
                message.createdAt.toInstant(TimeZone.currentSystemDefault()).toString(),
            )
            // Separate immutable intents avoid overwriting a file while a worker acknowledges it.
            val file = AtomicFile(File(directory(context), "${java.util.UUID.randomUUID()}.json"))
            val stream = file.startWrite()
            try {
                stream.write(Json.encodeToString(upload).toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                throw error
            }
        }
        }
        if (configs.isNotEmpty()) kick(context)
    }

    fun kick(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "external-message-upload", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ExternalMessageUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build(),
        )
    }

    fun recover(context: Context) {
        // Periodic recovery also covers death between durable file creation and work submission.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "external-message-upload-recovery", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExternalMessageUploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build(),
        )
        kick(context)
    }

    suspend fun pendingCount(context: Context): Int = withContext(Dispatchers.IO) {
        directory(context).listFiles().orEmpty().count { it.extension == "json" }
    }

    internal suspend fun drain(context: Context): Boolean = withContext(Dispatchers.IO) {
        val settings = GlobalContext.get().get<SettingsStore>().settingsFlowRaw.first()
        var retry = false
        directory(context).listFiles().orEmpty().filter { it.extension == "json" }.forEach { file ->
            try {
                val upload = drainMutex.withLock {
                    Json.decodeFromString<ExternalMessageUpload>(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
                }
                val config = settings.externalMemories.firstOrNull {
                    it.id.toString() == upload.configId && it.enabled && it.autoSaveMessages &&
                        it.supabaseUrl.trimEnd('/') == upload.url && it.tableName == upload.table
                } ?: return@forEach // Disabled/deleted/reconfigured destination: retain, do not send elsewhere.
                ExternalMemoryService(config).saveMessage(
                    assistantId = upload.assistantId, conversationId = upload.conversationId,
                    role = upload.role, content = upload.content,
                    messageId = upload.messageId, createdAt = upload.createdAt,
                ).getOrThrow()
                AtomicFile(file).delete()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                retry = true
                // No payloads, credentials, or remote response bodies in diagnostics.
                Log.w(TAG, "Upload pending: ${file.name}; ${error.javaClass.simpleName}")
            }
        }
        retry
    }
}

class ExternalMessageUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (ExternalMessageOutbox.drain(applicationContext)) Result.retry() else Result.success()
}
