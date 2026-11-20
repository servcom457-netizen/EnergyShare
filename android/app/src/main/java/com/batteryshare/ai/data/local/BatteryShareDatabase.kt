package com.batteryshare.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.batteryshare.ai.data.local.dao.EnergySessionDao
import com.batteryshare.ai.data.local.dao.ProtectionConfigDao
import com.batteryshare.ai.data.local.entity.EnergySessionEntity
import com.batteryshare.ai.data.local.entity.ProtectionConfigEntity

@Database(
    entities = [
        EnergySessionEntity::class,
        ProtectionConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BatteryShareDatabase : RoomDatabase() {
    abstract fun energySessionDao(): EnergySessionDao
    abstract fun protectionConfigDao(): ProtectionConfigDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryShareDatabase? = null

        fun getInstance(context: Context): BatteryShareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryShareDatabase::class.java,
                    "batteryshare_ai.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
