package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.health.HealthMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthMapperTest {
    @Test fun mapsAggregateUnitsToDailyCache() {
        val day=HealthMapper.map(2340.5,679.2,8432,42*60L,6*3600L+48*60L,58)
        assertEquals(2340.5,day.totalCaloriesKcal!!,0.001)
        assertEquals(8432L,day.steps); assertEquals(42,day.exerciseMinutes)
        assertEquals(408,day.sleepMinutes); assertEquals(58,day.restingHr)
    }
}
