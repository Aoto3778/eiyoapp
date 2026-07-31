package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.data.EntryWithFood
import jp.aoto.eiyoapp.data.GoalEntity
import jp.aoto.eiyoapp.domain.ExportData
import jp.aoto.eiyoapp.domain.Exporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ExporterTest {
    private val entry = EntryWithFood(
        entryId=1, foodId=1, amount=1.5,
        timestamp=LocalDateTime.of(2026,7,31,8,30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        name="ミックスナッツ", unit="握り拳", additivesJson="[\"乳化剤\"]",
        perKcal=180.0, perProtein=6.0, perSugar=3.0, perFat=16.0, perFiber=2.4,
        perSalt=0.0, perWater=1.0, perVitC=.3, perVitD=.8, perVitB=.5, perCa=25.0, perFe=1.2, perMg=75.0,
    )

    @Test fun markdownHasRequiredSectionsAndCalculatedValues() {
        val date=LocalDate.of(2026,7,31)
        val output=Exporter.markdown(date,date,ExportData(listOf(entry),listOf(GoalEntity("protein",80.0)),emptyList()))
        assertTrue(output.contains("## 日別サマリー"))
        assertTrue(output.contains("## Garmin 活動データ"))
        assertTrue(output.contains("08:30 朝：ミックスナッツ ×1.5握り拳（270 kcal / P 9 g）"))
        assertTrue(output.contains("乳化剤: 1回"))
    }

    @Test fun csvHasUtf8BomAndScaledNutrition() {
        val bytes=Exporter.csv(ExportData(listOf(entry),emptyList(),emptyList()))
        assertEquals(0xEF.toByte(),bytes[0]); assertEquals(0xBB.toByte(),bytes[1]); assertEquals(0xBF.toByte(),bytes[2])
        val text=bytes.copyOfRange(3,bytes.size).toString(Charsets.UTF_8)
        assertTrue(text.contains("\"270\",\"9\",\"4.5\""))
    }

    @Test fun timeBandsMatchSpecification() {
        assertEquals("深夜",Exporter.timeBand(4)); assertEquals("朝",Exporter.timeBand(5))
        assertEquals("昼",Exporter.timeBand(10)); assertEquals("間食",Exporter.timeBand(15))
        assertEquals("夜",Exporter.timeBand(18)); assertEquals("深夜",Exporter.timeBand(23))
    }
}
