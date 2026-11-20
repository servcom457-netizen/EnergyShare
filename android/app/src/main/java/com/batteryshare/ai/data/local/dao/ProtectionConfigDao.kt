package com.batteryshare.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.batteryshare.ai.data.local.entity.ProtectionConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionConfigDao {
    @Query("SELECT * FROM protection_config WHERE id = 1")
    fun observeConfig(): Flow<ProtectionConfigEntity?>

    @Query("SELECT * FROM protection_config WHERE id = 1")
    suspend fun getConfigOnce(): ProtectionConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ProtectionConfigEntity)
}
