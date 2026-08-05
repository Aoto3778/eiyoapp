package jp.aoto.eiyoapp.domain

import jp.aoto.eiyoapp.data.WorkoutSetEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object WorkoutMath {
    fun volume(weightKg: Double?, reps: Int?): Double = (weightKg ?: 0.0) * (reps ?: 0)

    fun estimatedOneRepMax(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || reps == null || weightKg <= 0 || reps <= 0) return null
        return weightKg * (1.0 + reps / 30.0)
    }

    fun isPersonalRecord(unit: String, now: WorkoutSetEntity, previous: WorkoutSetEntity?): Boolean {
        if (previous == null) return false
        return when (unit) {
            "kg" -> volume(now.weightKg, now.reps) > volume(previous.weightKg, previous.reps)
            "sec" -> (now.seconds ?: 0) > (previous.seconds ?: 0)
            else -> (now.reps ?: 0) > (previous.reps ?: 0)
        }
    }

    fun completedWeeks(
        sessionDates: List<LocalDate>,
        weekGoal: Int,
        today: LocalDate = LocalDate.now(),
    ): Map<LocalDate, Boolean> {
        val startOfCurrent = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return sessionDates.groupingBy { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .eachCount()
            .filterKeys { !it.isAfter(startOfCurrent) }
            .mapValues { it.value >= weekGoal }
    }

    fun streakWeeks(
        sessionDates: List<LocalDate>,
        weekGoal: Int,
        recoveryAllowance: Int,
        today: LocalDate = LocalDate.now(),
    ): Int {
        val weeks = completedWeeks(sessionDates, weekGoal, today)
        var cursor = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var recoveryLeft = recoveryAllowance
        var streak = 0
        repeat(260) {
            if (weeks[cursor] == true) streak++
            else if (recoveryLeft > 0) recoveryLeft--
            else return streak
            cursor = cursor.minusWeeks(1)
        }
        return streak
    }
}

