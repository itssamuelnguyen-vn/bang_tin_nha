package com.samuelcruz.bangtinnha.core.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.samuelcruz.bangtinnha.fuel.FuelDao
import com.samuelcruz.bangtinnha.fuel.FuelPrice
import com.samuelcruz.bangtinnha.fuel.FuelProduct
import com.samuelcruz.bangtinnha.fx.Currency
import com.samuelcruz.bangtinnha.fx.FxDao
import com.samuelcruz.bangtinnha.fx.FxRate
import com.samuelcruz.bangtinnha.gold.GoldDao
import com.samuelcruz.bangtinnha.gold.GoldPrice
import com.samuelcruz.bangtinnha.gold.GoldProduct
import com.samuelcruz.bangtinnha.power.PowerDao
import com.samuelcruz.bangtinnha.power.PowerOutage
import com.samuelcruz.bangtinnha.weather.WeatherCurrent
import com.samuelcruz.bangtinnha.weather.WeatherDaily
import com.samuelcruz.bangtinnha.weather.WeatherHourly
import com.samuelcruz.bangtinnha.weather.WeatherDao

/**
 * The one SQLite database (Room ~ ActiveRecord: entities are models, DAOs are their query methods).
 * A new source adds its entities and DAO here and bumps [version] with a migration.
 */
@Database(
    entities = [
        SourceStatus::class, NotificationLog::class,
        WeatherCurrent::class, WeatherDaily::class, WeatherHourly::class,
        Currency::class, FxRate::class,
        FuelProduct::class, FuelPrice::class,
        PowerOutage::class,
        GoldProduct::class, GoldPrice::class,
    ],
    version = 2,
    exportSchema = true,
    // Room writes the migration itself from the exported schemas (v2 only added the gold tables).
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceStatusDao(): SourceStatusDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun weatherDao(): WeatherDao
    abstract fun fxDao(): FxDao
    abstract fun fuelDao(): FuelDao
    abstract fun powerDao(): PowerDao
    abstract fun goldDao(): GoldDao

    companion object {
        const val NAME = "bang_tin_nha.db"

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
