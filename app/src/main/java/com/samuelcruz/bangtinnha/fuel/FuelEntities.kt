package com.samuelcruz.bangtinnha.fuel

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A Petrolimex product ("Xăng E10 RON 95-V"); [sourceKey] is Petrolimex's own id. */
@Entity(tableName = "fuel_product", indices = [Index(value = ["sourceKey"], unique = true)])
data class FuelProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceKey: String,
    val name: String,
    val displayOrder: Int,
)

/** Zone 1 retail price in đồng per litre/kg; a row is added only when the price changes. */
@Entity(tableName = "fuel_price", indices = [Index(value = ["productId", "fetchedAt"])])
data class FuelPrice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val price: Long,
    /** When Petrolimex says the price took effect (their LastModified), epoch millis. */
    val effectiveAt: Long,
    val fetchedAt: Long,
)

/** A product with its current and previous price (for the ▲/▼ arrow). */
data class FuelLatest(
    val productId: Long,
    val sourceKey: String,
    val name: String,
    val displayOrder: Int,
    val priceId: Long?,
    val price: Long?,
    val previousPrice: Long?,
    val effectiveAt: Long?,
    val fetchedAt: Long?,
)

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_product WHERE sourceKey = :key")
    suspend fun product(key: String): FuelProduct?

    @Insert
    suspend fun insertProduct(product: FuelProduct): Long

    @Update
    suspend fun updateProduct(product: FuelProduct)

    @Query("SELECT * FROM fuel_price WHERE productId = :productId ORDER BY fetchedAt DESC, id DESC LIMIT 1")
    suspend fun latestPrice(productId: Long): FuelPrice?

    @Insert
    suspend fun insertPrice(price: FuelPrice)

    @Query(LATEST_SQL)
    fun observeLatest(): Flow<List<FuelLatest>>

    @Query(LATEST_SQL)
    suspend fun latest(): List<FuelLatest>

    @Query("SELECT * FROM fuel_price WHERE productId = :productId ORDER BY fetchedAt")
    fun observeHistory(productId: Long): Flow<List<FuelPrice>>

    @Query("SELECT * FROM fuel_product ORDER BY displayOrder")
    fun observeProducts(): Flow<List<FuelProduct>>

    companion object {
        private const val ORDERED = "FROM fuel_price WHERE productId = p.id ORDER BY fetchedAt DESC, id DESC"
        const val LATEST_SQL = """
            SELECT p.id AS productId, p.sourceKey, p.name, p.displayOrder,
              (SELECT id $ORDERED LIMIT 1) AS priceId,
              (SELECT price $ORDERED LIMIT 1) AS price,
              (SELECT price $ORDERED LIMIT 1 OFFSET 1) AS previousPrice,
              (SELECT effectiveAt $ORDERED LIMIT 1) AS effectiveAt,
              (SELECT fetchedAt $ORDERED LIMIT 1) AS fetchedAt
            FROM fuel_product p ORDER BY p.displayOrder
        """
    }
}
