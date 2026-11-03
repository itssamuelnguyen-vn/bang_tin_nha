package com.samuelcruz.bangtinnha.power

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A planned outage published by EVNSPC for one power unit. All outages of the chosen units are stored;
 * the area keywords are applied when showing and notifying, so changing keywords needs no refresh.
 */
@Entity(
    tableName = "power_outage",
    indices = [Index(value = ["unitCode", "startAt", "endAt", "area"], unique = true), Index(value = ["startAt"])],
)
data class PowerOutage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitCode: String,
    val unitName: String,
    /** Areas as published, one per line ("Khu phố Ninh Thành phường Bình Minh"). */
    val area: String,
    val startAt: Long,
    val endAt: Long,
    val reason: String,
    val firstSeenAt: Long,
)

@Dao
interface PowerDao {
    @Query("SELECT * FROM power_outage WHERE endAt >= :now ORDER BY startAt")
    fun observeUpcoming(now: Long): Flow<List<PowerOutage>>

    @Query("SELECT * FROM power_outage WHERE endAt >= :now ORDER BY startAt")
    suspend fun upcoming(now: Long): List<PowerOutage>

    @Query("SELECT * FROM power_outage WHERE unitCode = :unit AND startAt >= :from")
    suspend fun futureOf(unit: String, from: Long): List<PowerOutage>

    /** Keeps the first row (and its firstSeenAt) when the same outage is seen again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<PowerOutage>)

    @Query("DELETE FROM power_outage WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM power_outage WHERE endAt < :before")
    suspend fun deleteEndedBefore(before: Long)

    @Query("DELETE FROM power_outage WHERE unitCode NOT IN (:units)")
    suspend fun deleteOtherUnits(units: List<String>)
}
