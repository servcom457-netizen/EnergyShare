package com.batteryshare.ai.data.repository

import com.batteryshare.ai.data.local.dao.EnergySessionDao
import com.batteryshare.ai.data.local.dao.ProtectionConfigDao
import com.batteryshare.ai.data.local.entity.EnergySessionEntity
import com.batteryshare.ai.data.local.entity.ProtectionConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BatteryShareRepository(
    private val sessionDao: EnergySessionDao,
    private val protectionDao: ProtectionConfigDao
) {
    fun observeSessions(): Flow<List<EnergySessionEntity>> =
        sessionDao.observeAllSessions()

    fun observeProtectionConfig(): Flow<ProtectionConfigEntity> =
        protectionDao.observeConfig().map { it ?: ProtectionConfigEntity() }

    suspend fun ensureDefaultProtectionConfig(): ProtectionConfigEntity {
        val existing = protectionDao.getConfigOnce()
        if (existing != null) return existing
        val defaultConfig = ProtectionConfigEntity()
        protectionDao.upsertConfig(defaultConfig)
        return defaultConfig
    }

    suspend fun saveProtectionConfig(config: ProtectionConfigEntity) {
        protectionDao.upsertConfig(config)
    }

    suspend fun startSession(session: EnergySessionEntity): Long =
        sessionDao.insertSession(session)

    suspend fun finishSession(
        session: EnergySessionEntity,
        endBatteryPct: Int,
        peakTempCelsius: Float?,
        terminationReason: String,
        hardwareTransferObserved: Boolean
    ) {
        sessionDao.updateSession(
            session.copy(
                endBatteryPct = endBatteryPct,
                peakTempCelsius = peakTempCelsius,
                endedAtMs = System.currentTimeMillis(),
                terminationReason = terminationReason,
                hardwareTransferObserved = hardwareTransferObserved
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun clearHistory() {
        sessionDao.clearAllSessions()
    }
}
