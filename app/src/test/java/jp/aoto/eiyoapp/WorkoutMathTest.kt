package jp.aoto.eiyoapp

import jp.aoto.eiyoapp.data.WorkoutSetEntity
import jp.aoto.eiyoapp.domain.WorkoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutMathTest {
    @Test fun volumeAndEstimatedOneRepMaxAreCalculated() {
        assertEquals(240.0, WorkoutMath.volume(40.0, 6), 0.001)
        assertEquals(48.0, WorkoutMath.estimatedOneRepMax(40.0, 6)!!, 0.001)
        assertEquals(null, WorkoutMath.estimatedOneRepMax(null, 6))
    }

    @Test fun personalRecordUsesTheExerciseUnit() {
        val previous = WorkoutSetEntity(sessionId = 1, exerciseId = 1, setNo = 1, weightKg = 40.0, reps = 6, seconds = 30)
        assertTrue(WorkoutMath.isPersonalRecord("kg", previous.copy(sessionId = 2, weightKg = 42.5), previous))
        assertTrue(WorkoutMath.isPersonalRecord("bw", previous.copy(sessionId = 2, reps = 7), previous))
        assertTrue(WorkoutMath.isPersonalRecord("sec", previous.copy(sessionId = 2, seconds = 31), previous))
        assertFalse(WorkoutMath.isPersonalRecord("kg", previous.copy(sessionId = 2), previous))
    }

    @Test fun recoveryAllowanceBridgesOneMissedWeek() {
        val today = LocalDate.of(2026, 8, 5)
        val sessions = listOf(
            LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 24),
        )
        assertEquals(2, WorkoutMath.streakWeeks(sessions, weekGoal = 2, recoveryAllowance = 1, today = today))
        assertEquals(1, WorkoutMath.streakWeeks(sessions, weekGoal = 2, recoveryAllowance = 0, today = today))
    }
}
