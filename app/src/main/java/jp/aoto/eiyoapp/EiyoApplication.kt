package jp.aoto.eiyoapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import jp.aoto.eiyoapp.data.AppDatabase
import jp.aoto.eiyoapp.data.EiyoRepository
import jp.aoto.eiyoapp.data.WorkoutRepository
import jp.aoto.eiyoapp.health.HealthConnectManager
import jp.aoto.eiyoapp.health.HealthSyncWorker
import java.util.concurrent.TimeUnit

class EiyoApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { EiyoRepository(database) }
    val workoutRepository by lazy { WorkoutRepository(database) }
    val health by lazy { HealthConnectManager(this) }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health-daily-sync", ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }
}
