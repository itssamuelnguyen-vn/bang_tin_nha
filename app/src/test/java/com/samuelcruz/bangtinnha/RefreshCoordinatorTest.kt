package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.core.db.SourceStatus
import com.samuelcruz.bangtinnha.core.db.SourceStatusDao
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshCoordinator
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private class FakeStatusDao : SourceStatusDao {
    val rows = MutableStateFlow<Map<String, SourceStatus>>(emptyMap())
    override fun observeAll(): Flow<List<SourceStatus>> = MutableStateFlow(rows.value.values.toList())
    override suspend fun get(id: String) = rows.value[id]
    override suspend fun upsert(status: SourceStatus) {
        rows.value = rows.value + (status.sourceId to status)
    }
}

private class FakeSource(override val id: String, override val refreshInterval: Duration, val fail: Boolean = false) : DataSource {
    override val title = id
    var calls = 0
    override suspend fun refresh(): RefreshResult {
        calls++
        if (fail) throw IOException("HTTP 500 từ $id")
        return RefreshResult("ok $id")
    }
}

class RefreshCoordinatorTest {
    @Test
    fun oneFailingSourceDoesNotStopTheOthers() = runTest {
        val dao = FakeStatusDao()
        val good = FakeSource("weather", 1.hours)
        val bad = FakeSource("fuel", 12.hours, fail = true)
        val other = FakeSource("power", 24.hours)
        var afterCalls = 0
        val coordinator = RefreshCoordinator(listOf(good, bad, other), dao, afterRefresh = { afterCalls++ }, now = { 1_000L })

        val results = coordinator.refreshAll()

        assertTrue(results.getValue("weather").isSuccess)
        assertTrue(results.getValue("fuel").isFailure)
        assertTrue(results.getValue("power").isSuccess)
        assertEquals(1_000L, dao.get("weather")!!.lastSuccessAt)
        assertEquals("HTTP 500 từ fuel", dao.get("fuel")!!.lastError)
        assertNull(dao.get("fuel")!!.lastSuccessAt)
        assertEquals(1, afterCalls)
    }

    @Test
    fun onlyDueSourcesRefresh() = runTest {
        val dao = FakeStatusDao()
        var clock = 0L
        val hourly = FakeSource("weather", 1.hours)
        val daily = FakeSource("power", 24.hours)
        val coordinator = RefreshCoordinator(listOf(hourly, daily), dao, now = { clock })

        coordinator.refreshDue() // never refreshed: both run
        clock += 2 * 3600_000L
        coordinator.refreshDue() // 2 hours later: only the hourly one is due
        assertEquals(2, hourly.calls)
        assertEquals(1, daily.calls)
    }

    @Test
    fun aRecoveredSourceClearsItsError() = runTest {
        val dao = FakeStatusDao()
        dao.upsert(SourceStatus("weather", lastSuccessAt = null, lastError = "timeout"))
        RefreshCoordinator(listOf(FakeSource("weather", 1.hours)), dao, now = { 5L }).refreshAll()
        assertNull(dao.get("weather")!!.lastError)
        assertEquals("ok weather", dao.get("weather")!!.lastSummary)
    }
}
