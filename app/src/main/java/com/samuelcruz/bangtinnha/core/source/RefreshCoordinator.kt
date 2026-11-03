package com.samuelcruz.bangtinnha.core.source

import com.samuelcruz.bangtinnha.core.db.SourceStatus
import com.samuelcruz.bangtinnha.core.db.SourceStatusDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs sources one by one and records how each went. A failing source never stops the others
 * (each refresh is wrapped like a `rescue` per job). Only one refresh run happens at a time.
 */
class RefreshCoordinator(
    val sources: List<DataSource>,
    private val statusDao: SourceStatusDao,
    private val afterRefresh: suspend () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    /** Sources whose last success is older than their interval (or never succeeded). */
    suspend fun dueSources(): List<DataSource> = sources.filter { source ->
        val last = statusDao.get(source.id)?.lastSuccessAt ?: return@filter true
        (now() - last).milliseconds >= source.refreshInterval
    }

    suspend fun refreshDue() = run(dueSources())

    suspend fun refreshAll() = run(sources)

    suspend fun refreshOne(id: String) = run(sources.filter { it.id == id })

    /** Parsing runs on a background thread even when a screen starts the refresh, so the UI never stutters. */
    private suspend fun run(list: List<DataSource>): Map<String, Result<RefreshResult>> = withContext(Dispatchers.Default) {
        mutex.withLock { runLocked(list) }
    }

    private suspend fun runLocked(list: List<DataSource>): Map<String, Result<RefreshResult>> {
        val results = list.associate { source -> source.id to refreshSafely(source) }
        if (list.isNotEmpty()) runCatching { afterRefresh() }
        return results
    }

    private suspend fun refreshSafely(source: DataSource): Result<RefreshResult> {
        val started = now()
        val previous = statusDao.get(source.id) ?: SourceStatus(source.id)
        statusDao.upsert(previous.copy(lastAttemptAt = started))
        return try {
            val result = source.refresh()
            statusDao.upsert(
                previous.copy(lastAttemptAt = started, lastSuccessAt = now(), lastError = null, lastSummary = result.summary),
            )
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message?.take(500) ?: e::class.simpleName ?: "Lỗi không rõ"
            statusDao.upsert(previous.copy(lastAttemptAt = started, lastError = message))
            Result.failure(e)
        }
    }
}
