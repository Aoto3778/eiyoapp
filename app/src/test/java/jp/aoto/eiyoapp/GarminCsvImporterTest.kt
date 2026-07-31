package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.domain.GarminCsvImporter
import org.junit.Assert.assertEquals
import org.junit.Test

class GarminCsvImporterTest {
    @Test fun importsEnglishGarminColumnsAndQuotedNumbers() {
        val csv="Date,Total Calories,Steps,Exercise Minutes,Sleep Duration,Resting Heart Rate\n2026-07-31,2340,\"8,432\",42,6.8,58"
        val row=GarminCsvImporter.parse(csv).single()
        assertEquals("2026-07-31",row.date); assertEquals(2340.0,row.totalCaloriesKcal!!,0.001)
        assertEquals(8432L,row.steps); assertEquals(42,row.exerciseMinutes); assertEquals(408,row.sleepMinutes)
    }
}
