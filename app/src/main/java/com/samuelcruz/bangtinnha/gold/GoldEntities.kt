package com.samuelcruz.bangtinnha.gold

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A PNJ gold product ("Vàng miếng SJC 999.9"); [code] is PNJ's own code ("SJC", "N24K"…). */
@Entity(tableName = "gold_product", indices = [Index(value = ["code"], unique = true)])
data class GoldProduct(@PrimaryKey(autoGenerate = true) val id: Long = 0, val code: String, val name: String, val displayOrder: Int)

/** Buy and sell prices in đồng per chỉ; a row is added only when one of them changes. */
@Entity(tableName = "gold_price", indices = [Index(value = ["productId", "fetchedAt"])])
data class GoldPrice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val buy: Long,
    val sell: Long,
    /** PNJ's "updateDate", epoch millis. */
    val sourceTime: Long?,
    val fetchedAt: Long,
)

data class GoldLatest(
    val productId: Long,
    val code: String,
    val name: String,
    val buy: Long?,
    val sell: Long?,
    val previousSell: Long?,
    val sourceTime: Long?,
)

@Dao
interface GoldDao {
    @Query("SELECT * FROM gold_product WHERE code = :code")
    suspend fun product(code: String): GoldProduct?

    @Insert
    suspend fun insertProduct(product: GoldProduct): Long

    @Update
    suspend fun updateProduct(product: GoldProduct)

    @Query("SELECT * FROM gold_price WHERE productId = :productId ORDER BY fetchedAt DESC, id DESC LIMIT 1")
    suspend fun latestPrice(productId: Long): GoldPrice?

    @Insert
    suspend fun insertPrice(price: GoldPrice)

    @Query(LATEST_SQL)
    fun observeLatest(): Flow<List<GoldLatest>>

    @Query(LATEST_SQL)
    suspend fun latest(): List<GoldLatest>

    @Query("SELECT * FROM gold_price WHERE productId = :productId ORDER BY fetchedAt")
    fun observeHistory(productId: Long): Flow<List<GoldPrice>>

    @Query("SELECT * FROM gold_product ORDER BY displayOrder")
    fun observeProducts(): Flow<List<GoldProduct>>

    /** Keeps only the last price of each Vietnam day for rows older than [before] (see Maintenance). */
    @Query(
        """
        DELETE FROM gold_price WHERE fetchedAt < :before AND id NOT IN (
          SELECT MAX(id) FROM gold_price WHERE fetchedAt < :before
          GROUP BY productId, (fetchedAt + 25200000) / 86400000)
        """,
    )
    suspend fun thinBefore(before: Long): Int

    companion object {
        private const val ORDERED = "FROM gold_price WHERE productId = p.id ORDER BY fetchedAt DESC, id DESC"
        const val LATEST_SQL = """
            SELECT p.id AS productId, p.code, p.name,
              (SELECT buy $ORDERED LIMIT 1) AS buy,
              (SELECT sell $ORDERED LIMIT 1) AS sell,
              (SELECT sell $ORDERED LIMIT 1 OFFSET 1) AS previousSell,
              (SELECT sourceTime $ORDERED LIMIT 1) AS sourceTime
            FROM gold_product p ORDER BY p.displayOrder
        """
    }
}
