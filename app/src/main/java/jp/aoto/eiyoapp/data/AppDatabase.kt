package jp.aoto.eiyoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FoodEntity::class, EntryEntity::class, GoalEntity::class, DailyActivityEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "eiyo.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            instance?.foodDao()?.insertAll(seedFoods)
                            instance?.goalDao()?.insertAll(listOf(
                                GoalEntity("protein", 80.0), GoalEntity("water", 2000.0),
                            ))
                        }
                    }
                }).build().also { instance = it }
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
