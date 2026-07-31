package jp.aoto.eiyoapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import jp.aoto.eiyoapp.data.DailyActivityEntity
import java.time.LocalDate
import java.time.ZoneId

enum class HealthAvailability { AVAILABLE, NEEDS_INSTALL, NOT_SUPPORTED }

data class HealthDay(
    val totalCaloriesKcal: Double?, val activeCaloriesKcal: Double?, val steps: Long?,
    val exerciseMinutes: Int?, val sleepMinutes: Int?, val restingHr: Int?,
) {
    fun entity(date: LocalDate) = DailyActivityEntity(
        date=date.toString(), totalCaloriesKcal=totalCaloriesKcal,
        activeCaloriesKcal=activeCaloriesKcal, steps=steps, exerciseMinutes=exerciseMinutes,
        sleepMinutes=sleepMinutes, restingHr=restingHr,
    )
}

object HealthMapper {
    fun map(
        totalCaloriesKcal: Double?, activeCaloriesKcal: Double?, steps: Long?,
        exerciseSeconds: Long?, sleepSeconds: Long?, restingHr: Int?,
    ) = HealthDay(
        totalCaloriesKcal, activeCaloriesKcal, steps,
        exerciseSeconds?.div(60)?.toInt(), sleepSeconds?.div(60)?.toInt(), restingHr,
    )
}

class HealthConnectManager(private val context: Context) {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    )

    fun availability(): HealthAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.NEEDS_INSTALL
        else -> HealthAvailability.NOT_SUPPORTED
    }

    fun clientOrNull(): HealthConnectClient? = if (availability() == HealthAvailability.AVAILABLE)
        HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean = clientOrNull()?.permissionController
        ?.getGrantedPermissions()?.containsAll(permissions) == true

    suspend fun readDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): HealthDay {
        val client = requireNotNull(clientOrNull()) { "ヘルスコネクトを利用できません" }
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result = client.aggregate(AggregateRequest(
            metrics = setOf(
                StepsRecord.COUNT_TOTAL,
                TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            ),
            timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
        ))

        val sleepStart = date.minusDays(1).atTime(20, 0).atZone(zone).toInstant()
        val sleepEnd = date.atTime(12, 0).atZone(zone).toInstant()
        val sleep = client.aggregate(AggregateRequest(
            metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(sleepStart, sleepEnd),
        ))
        val resting = client.readRecords(ReadRecordsRequest(
            recordType = RestingHeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
            ascendingOrder = false,
            pageSize = 1,
        )).records.firstOrNull()?.beatsPerMinute?.toInt()
        val fallback = if (resting == null) client.readRecords(ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd),
            ascendingOrder = true,
        )).records.flatMap { it.samples }.minOfOrNull { it.beatsPerMinute.toInt() } else null

        return result.toHealthDay(sleep, resting ?: fallback)
    }

    companion object {
        fun map(result: AggregationResult, sleep: AggregationResult, restingHr: Int?) =
            result.toHealthDay(sleep, restingHr)
    }
}

private fun AggregationResult.toHealthDay(sleep: AggregationResult, restingHr: Int?) = HealthDay(
    totalCaloriesKcal = this[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
    activeCaloriesKcal = this[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories,
    steps = this[StepsRecord.COUNT_TOTAL],
    exerciseMinutes = this[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toInt(),
    sleepMinutes = sleep[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()?.toInt(),
    restingHr = restingHr,
)
