package com.samuelcruz.bangtinnha.core.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** When each source last worked, last tried, and the last error (null once it works again). */
@Entity(tableName = "source_status")
data class SourceStatus(
    @PrimaryKey val sourceId: String,
    val lastSuccessAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val lastSummary: String? = null,
)

@Dao
interface SourceStatusDao {
    @Query("SELECT * FROM source_status")
    fun observeAll(): Flow<List<SourceStatus>>

    @Query("SELECT * FROM source_status WHERE sourceId = :id")
    suspend fun get(id: String): SourceStatus?

    @Upsert
    suspend fun upsert(status: SourceStatus)
}

/** Sent notifications, so the same alert is never shown twice. */
@Entity(tableName = "notification_log")
data class NotificationLog(@PrimaryKey val key: String, val sentAt: Long)

@Dao
interface NotificationLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM notification_log WHERE `key` = :key)")
    suspend fun wasSent(key: String): Boolean

    @Upsert
    suspend fun markSent(log: NotificationLog)

    @Query("DELETE FROM notification_log WHERE sentAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
