package jp.aoto.eiyoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodEntity::class, EntryEntity::class, GoalEntity::class, DailyActivityEntity::class,
        WorkoutExerciseEntity::class, WorkoutSessionEntity::class, WorkoutSetEntity::class,
        BodyMetricEntity::class, WorkoutSettingEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao
    abstract fun activityDao(): ActivityDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "eiyo.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            instance?.foodDao()?.insertAll(seedFoods)
                            instance?.goalDao()?.insertAll(listOf(
                                GoalEntity("protein", 80.0), GoalEntity("water", 2000.0),
                            ))
                            instance?.workoutDao()?.insertSettings(listOf(
                                WorkoutSettingEntity("weekGoal", "2"),
                                WorkoutSettingEntity("restSeconds", "90"),
                                WorkoutSettingEntity("recoveryAllowance", "1"),
                                WorkoutSettingEntity("defaultStepKg", "2.5"),
                                WorkoutSettingEntity("xp", "0"),
                            ))
                        }
                    }
                }).build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `part` TEXT NOT NULL, `unit` TEXT NOT NULL, `stepKg` REAL NOT NULL, `provisional` INTEGER NOT NULL, `photoUri` TEXT, `note` TEXT, `createdAt` INTEGER NOT NULL, `mergedInto` INTEGER)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_mergedInto` ON `workout_exercises` (`mergedInto`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `activeExerciseId` INTEGER, `conditionNote` TEXT NOT NULL, `completed` INTEGER NOT NULL, FOREIGN KEY(`activeExerciseId`) REFERENCES `workout_exercises`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_date` ON `workout_sessions` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_activeExerciseId` ON `workout_sessions` (`activeExerciseId`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `exerciseId` INTEGER NOT NULL, `setNo` INTEGER NOT NULL, `weightKg` REAL, `reps` INTEGER, `seconds` INTEGER, `rpe` INTEGER, `isPr` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`exerciseId`) REFERENCES `workout_exercises`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON `workout_sets` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_recordedAt` ON `workout_sets` (`recordedAt`)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `body_metrics` (`date` TEXT NOT NULL, `weightKg` REAL, `bodyFatPct` REAL, `armCm` REAL, `chestCm` REAL, `waistCm` REAL, `thighCm` REAL, PRIMARY KEY(`date`))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS `workout_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))""")
                db.execSQL("INSERT OR IGNORE INTO `workout_settings` (`key`,`value`) VALUES ('weekGoal','2'),('restSeconds','90'),('recoveryAllowance','1'),('defaultStepKg','2.5'),('xp','0')")
            }
        }
    }
}

val seedFoods = listOf(
    FoodEntity(name="ミックスナッツ", unit="握り拳", unitNote="約30g", perKcal=180.0, perProtein=6.0, perSugar=3.0, perFat=16.0, perFiber=2.4, perVitC=.3, perVitB=.8, perCa=25.0, perFe=1.2, perMg=75.0),
    FoodEntity(name="水", unit="コップ", unitNote="約200ml", perWater=200.0),
    FoodEntity(name="白米", unit="茶碗", unitNote="約150g", perKcal=234.0, perProtein=3.8, perSugar=53.4, perFat=.5, perFiber=2.3, perWater=90.0),
    FoodEntity(name="卵", unit="個", unitNote="約60g", perKcal=76.0, perProtein=6.2, perSugar=.2, perFat=5.2, perSalt=.2),
    FoodEntity(name="納豆", unit="パック", unitNote="約45g", perKcal=86.0, perProtein=7.4, perSugar=2.4, perFat=4.5, perFiber=3.0, perSalt=.3),
    FoodEntity(name="鶏むね肉", unit="100g", unitNote="皮なし", perKcal=108.0, perProtein=22.3, perFat=1.5),
    FoodEntity(name="プロテイン", unit="杯", unitNote="約30g", perKcal=120.0, perProtein=21.0, perSugar=3.0, perFat=2.0),
    FoodEntity(name="ヨーグルト", unit="個", unitNote="約100g", perKcal=61.0, perProtein=3.6, perSugar=4.9, perFat=3.0, perCa=120.0),
    FoodEntity(name="サラダチキン", unit="個", unitNote="約110g", perKcal=120.0, perProtein=24.0, perSugar=1.0, perFat=1.5, perSalt=1.2),
    FoodEntity(name="バナナ", unit="本", unitNote="約100g", perKcal=93.0, perProtein=1.1, perSugar=21.4, perFat=.2, perFiber=1.1, perWater=75.4, perVitC=16.0, perMg=32.0),
)
