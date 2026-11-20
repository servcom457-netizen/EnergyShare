package com.batteryshare.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.batteryshare.ai.data.local.entity.EnergySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergySessionDao {
    @Query("SELECT * FROM energy_sessions ORDER BY startedAtMs DESC")
    fun observeAllSessions(): Flow<List<EnergySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: EnergySessionEntity): Long

    @Update
    suspend fun updateSession(session: EnergySessionEntity)

    @Query("DELETE FROM energy_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM energy_sessions")
    suspend fun clearAllSessions()
}
