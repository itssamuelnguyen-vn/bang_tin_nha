package com.samuelcruz.bangtinnha.fx

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A currency name lives here once; rate rows only keep its id (normalised, like a belongs_to). */
@Entity(tableName = "currency", indices = [Index(value = ["code"], unique = true)])
data class Currency(@PrimaryKey(autoGenerate = true) val id: Long = 0, val code: String, val name: String)

/**
 * Vietcombank rates in hundredths of a đồng (26,345.50 → 2634550), so prices are exact integers.
 * A row is only added when one of the three prices changed since the currency's previous row.
 */
@Entity(tableName = "fx_rate", indices = [Index(value = ["currencyId", "fetchedAt"])])
data class FxRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currencyId: Long,
    val fetchedAt: Long,
    /** The bank's own timestamp for the table, epoch millis. */
    val sourceTime: Long?,
    val buyCash: Long?,
    val buyTransfer: Long?,
    val sell: Long?,
)

/** Latest rate of a currency, joined with its name. */
data class FxLatest(
    val code: String,
    val name: String,
    val buyCash: Long?,
    val buyTransfer: Long?,
    val sell: Long?,
    val fetchedAt: Long,
    val sourceTime: Long?,
    /** Sell price of the row before the latest, for the ▲/▼ badge. */
    val previousSell: Long?,
)

@Dao
interface FxDao {
    @Query("SELECT * FROM currency WHERE code = :code")
    suspend fun currency(code: String): Currency?

    @Insert
    suspend fun insertCurrency(currency: Currency): Long

    @Query("SELECT * FROM fx_rate WHERE currencyId = :currencyId ORDER BY fetchedAt DESC LIMIT 1")
    suspend fun latest(currencyId: Long): FxRate?

    @Insert
    suspend fun insertRate(rate: FxRate)

    @Query(
        """
        SELECT c.code, c.name, r.buyCash, r.buyTransfer, r.sell, r.fetchedAt, r.sourceTime,
          (SELECT sell FROM fx_rate WHERE currencyId = c.id ORDER BY fetchedAt DESC LIMIT 1 OFFSET 1) AS previousSell
        FROM currency c JOIN fx_rate r
          ON r.id = (SELECT id FROM fx_rate WHERE currencyId = c.id ORDER BY fetchedAt DESC LIMIT 1)
        ORDER BY c.code
        """,
    )
    fun observeLatest(): Flow<List<FxLatest>>

    @Query("SELECT r.* FROM fx_rate r JOIN currency c ON c.id = r.currencyId WHERE c.code = :code ORDER BY r.fetchedAt")
    fun observeHistory(code: String): Flow<List<FxRate>>

    @Query("SELECT code FROM currency ORDER BY code")
    fun observeCodes(): Flow<List<String>>
}
