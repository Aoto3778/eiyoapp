package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.data.WorkoutExportData
import jp.aoto.eiyoapp.data.WorkoutExerciseEntity
import jp.aoto.eiyoapp.data.WorkoutSessionEntity
import jp.aoto.eiyoapp.data.WorkoutSetWithExercise
import jp.aoto.eiyoapp.domain.ExportData
import jp.aoto.eiyoapp.domain.WorkoutExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutExporterTest {
    private val date = LocalDate.of(2026, 8, 5)
    private val workout = WorkoutExportData(
        exercises = listOf(WorkoutExerciseEntity(id = 1, name = "チェストプレス", part = "胸")),
        sessions = listOf(WorkoutSessionEntity(id = 1, date = date.toString(), startedAt = 1_000, endedAt = 61_000, completed = true)),
        sets = listOf(WorkoutSetWithExercise(1, 1, 1, 1, 40.0, 10, null, 8, true, 2_000, "チェストプレス", "胸", "kg", 2.5)),
        bodyMetrics = emptyList(),
        settings = emptyList(),
    )

    @Test fun markdownContainsWorkoutSummaryAndExercise() {
        val text = WorkoutExporter.markdown(date, date, workout, ExportData(emptyList(), emptyList(), emptyList()))
        assertTrue(text.contains("実施 1回"))
        assertTrue(text.contains("チェストプレス"))
        assertTrue(text.contains("推定1RM"))
    }

    @Test fun csvIsUtf8BomAndHasStableColumns() {
        val bytes = WorkoutExporter.csv(workout, ExportData(emptyList(), emptyList(), emptyList()))
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        val text = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        assertTrue(text.startsWith("date,exercise,part,type"))
        assertTrue(text.contains("\"チェストプレス\""))
    }
}
