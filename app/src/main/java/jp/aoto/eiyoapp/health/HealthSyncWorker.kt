package jp.aoto.eiyoapp.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import jp.aoto.eiyoapp.data.AppDatabase
import java.time.LocalDate

class HealthSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val health = HealthConnectManager(applicationContext)
        if (health.availability() != HealthAvailability.AVAILABLE || !health.hasAllPermissions()) return Result.success()
        val dao = AppDatabase.get(applicationContext).activityDao()
        return runCatching {
            (0L..7L).forEach { offset ->
                val date = LocalDate.now().minusDays(offset)
                dao.upsert(health.readDay(date).entity(date))
            }
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
