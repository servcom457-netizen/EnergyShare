package com.batteryshare.ai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batteryshare.ai.data.local.BatteryShareDatabase
import com.batteryshare.ai.data.local.dao.EnergySessionDao
import com.batteryshare.ai.data.local.entity.EnergySessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnergySessionDaoTest {

    private lateinit var db: BatteryShareDatabase
    private lateinit var dao: EnergySessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BatteryShareDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.energySessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndObserveSession_returnsInsertedRow() = runBlocking {
        val session = EnergySessionEntity(
            mode = "Partage USB-C / OTG",
            peerDeviceName = "Pixel 8",
            startBatteryPct = 82,
            endBatteryPct = 74,
            peakTempCelsius = 36.4f,
            startedAtMs = 1_700_000_000_000L,
            endedAtMs = 1_700_000_600_000L,
            terminationReason = "Arrêt manuel",
            hardwareTransferObserved = true
        )
        dao.insertSession(session)
        val rows = dao.observeAllSessions().first()
        assertEquals(1, rows.size)
        assertEquals("Pixel 8", rows.first().peerDeviceName)
    }
}
