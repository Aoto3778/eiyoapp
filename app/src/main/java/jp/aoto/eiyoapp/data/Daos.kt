package jp.aoto.eiyoapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val FOOD_WITH_COUNT = """
SELECT f.*, COUNT(e.id) AS usageCount FROM foods f
LEFT JOIN entries e ON e.foodId = f.id
"""

@Dao
interface FoodDao {
    @Query("$FOOD_WITH_COUNT GROUP BY f.id ORDER BY usageCount DESC, f.name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodWithCount>>

    @Query("$FOOD_WITH_COUNT WHERE (:query = '' OR f.name LIKE '%' || :query || '%') GROUP BY f.id ORDER BY usageCount DESC, f.name COLLATE NOCASE LIMIT :limit")
    fun observeCandidates(query: String, limit: Int = 6): Flow<List<FoodWithCount>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun get(id: Long): FoodEntity?

    @Query("SELECT * FROM foods ORDER BY name")
    suspend fun getAll(): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Update suspend fun update(food: FoodEntity)
    @Delete suspend fun delete(food: FoodEntity)

    @Query("UPDATE foods SET lastAmount = :amount WHERE id = :id")
    suspend fun updateLastAmount(id: Long, amount: Double)
}

@Dao
interface EntryDao {
    @Query("""
        SELECT e.id AS entryId, e.foodId, e.amount, e.timestamp, f.name, f.unit, f.additivesJson,
        f.perKcal, f.perProtein, f.perSugar, f.perFat, f.perFiber, f.perSalt, f.perWater,
        f.perVitC, f.perVitD, f.perVitB, f.perCa, f.perFe, f.perMg
        FROM entries e JOIN foods f ON f.id = e.foodId
        WHERE e.timestamp BETWEEN :fromInclusive AND :toInclusive
        ORDER BY e.timestamp DESC
    """)
    fun observeBetween(fromInclusive: Long, toInclusive: Long): Flow<List<EntryWithFood>>

    @Query("""
        SELECT e.id AS entryId, e.foodId, e.amount, e.timestamp, f.name, f.unit, f.additivesJson,
        f.perKcal, f.perProtein, f.perSugar, f.perFat, f.perFiber, f.perSalt, f.perWater,
        f.perVitC, f.perVitD, f.perVitB, f.perCa, f.perFe, f.perMg
        FROM entries e JOIN foods f ON f.id = e.foodId
        WHERE e.timestamp BETWEEN :fromInclusive AND :toInclusive
        ORDER BY e.timestamp
    """)
    suspend fun getBetween(fromInclusive: Long, toInclusive: Long): List<EntryWithFood>

    @Query("SELECT * FROM entries ORDER BY timestamp") suspend fun getAll(): List<EntryEntity>
    @Insert suspend fun insert(entry: EntryEntity): Long
    @Insert suspend fun insertAll(entries: List<EntryEntity>)
    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM entries") suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals") fun observeAll(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals") suspend fun getAll(): List<GoalEntity>
    @Upsert suspend fun upsert(goal: GoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(goals: List<GoalEntity>)
    @Query("DELETE FROM goals WHERE nutrientKey = :key") suspend fun delete(key: String)
    @Query("DELETE FROM goals") suspend fun deleteAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: String, to: String): Flow<List<DailyActivityEntity>>
    @Query("SELECT * FROM daily_activity WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getBetween(from: String, to: String): List<DailyActivityEntity>
    @Query("SELECT * FROM daily_activity ORDER BY date") suspend fun getAll(): List<DailyActivityEntity>
    @Upsert suspend fun upsert(activity: DailyActivityEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<DailyActivityEntity>)
    @Query("DELETE FROM daily_activity") suspend fun deleteAll()
}
