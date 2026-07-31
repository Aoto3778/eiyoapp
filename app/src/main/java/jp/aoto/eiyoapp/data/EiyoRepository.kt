package jp.aoto.eiyoapp.data

import androidx.room.withTransaction
import jp.aoto.eiyoapp.domain.ExportData
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EiyoRepository(private val db: AppDatabase) {
    val foods: Flow<List<FoodWithCount>> = db.foodDao().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goalDao().observeAll()

    fun candidates(query: String) = db.foodDao().observeCandidates(query.trim())
    fun entries(from: LocalDate, to: LocalDate) = db.entryDao().observeBetween(from.startMillis(), to.endMillis())
    fun activities(from: LocalDate, to: LocalDate) = db.activityDao().observeBetween(from.toString(), to.toString())

    suspend fun addEntry(food: FoodEntity, amount: Double, timestamp: Long) = db.withTransaction {
        db.entryDao().insert(EntryEntity(foodId = food.id, amount = amount, timestamp = timestamp))
        db.foodDao().updateLastAmount(food.id, amount)
    }

    suspend fun deleteEntry(id: Long) = db.entryDao().delete(id)
    suspend fun saveFood(food: FoodEntity): Long = if (food.id == 0L) db.foodDao().insert(food) else {
        db.foodDao().update(food); food.id
    }
    suspend fun deleteFood(food: FoodEntity) = db.foodDao().delete(food)
    suspend fun setGoal(key: String, target: Double?) {
        if (target == null || target <= 0) db.goalDao().delete(key)
        else db.goalDao().upsert(GoalEntity(key, target))
    }
    suspend fun saveActivity(activity: DailyActivityEntity) = db.activityDao().upsert(activity)

    suspend fun exportData(from: LocalDate, to: LocalDate): ExportData = ExportData(
        entries = db.entryDao().getBetween(from.startMillis(), to.endMillis()),
        goals = db.goalDao().getAll(),
        activities = db.activityDao().getBetween(from.toString(), to.toString()),
    )

    suspend fun backupJson(): String {
        val root = JSONObject().put("version", 1)
        root.put("foods", JSONArray(db.foodDao().getAll().map { f ->
            JSONObject().put("id", f.id).put("name", f.name).put("unit", f.unit)
                .put("unitNote", f.unitNote).put("lastAmount", f.lastAmount)
                .put("additivesJson", f.additivesJson).put("perKcal", f.perKcal)
                .put("perProtein", f.perProtein).put("perSugar", f.perSugar)
                .put("perFat", f.perFat).put("perFiber", f.perFiber).put("perSalt", f.perSalt)
                .put("perWater", f.perWater).put("perVitC", f.perVitC).put("perVitD", f.perVitD)
                .put("perVitB", f.perVitB).put("perCa", f.perCa).put("perFe", f.perFe).put("perMg", f.perMg)
        }))
        root.put("entries", JSONArray(db.entryDao().getAll().map { e ->
            JSONObject().put("id", e.id).put("foodId", e.foodId).put("amount", e.amount).put("timestamp", e.timestamp)
        }))
        root.put("goals", JSONArray(db.goalDao().getAll().map { g ->
            JSONObject().put("nutrientKey", g.nutrientKey).put("target", g.target)
        }))
        root.put("activities", JSONArray(db.activityDao().getAll().map { a ->
            JSONObject().put("date", a.date).putNullable("totalCaloriesKcal", a.totalCaloriesKcal)
                .putNullable("activeCaloriesKcal", a.activeCaloriesKcal).putNullable("steps", a.steps)
                .putNullable("exerciseMinutes", a.exerciseMinutes).putNullable("sleepMinutes", a.sleepMinutes)
                .putNullable("restingHr", a.restingHr).put("syncedAt", a.syncedAt)
        }))
        return root.toString(2)
    }

    suspend fun restoreJson(text: String) {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "対応していないバックアップ形式です" }
        val foods = root.getJSONArray("foods").objects().map { o -> FoodEntity(
            id=o.getLong("id"), name=o.getString("name"), unit=o.getString("unit"),
            unitNote=o.optString("unitNote").takeIf { it.isNotBlank() && it != "null" },
            lastAmount=o.getDouble("lastAmount"), additivesJson=o.getString("additivesJson"),
            perKcal=o.getDouble("perKcal"), perProtein=o.getDouble("perProtein"), perSugar=o.getDouble("perSugar"),
            perFat=o.getDouble("perFat"), perFiber=o.getDouble("perFiber"), perSalt=o.getDouble("perSalt"),
            perWater=o.getDouble("perWater"), perVitC=o.getDouble("perVitC"), perVitD=o.getDouble("perVitD"),
            perVitB=o.getDouble("perVitB"), perCa=o.getDouble("perCa"), perFe=o.getDouble("perFe"), perMg=o.getDouble("perMg"),
        ) }
        val entries = root.getJSONArray("entries").objects().map { o -> EntryEntity(o.getLong("id"), o.getLong("foodId"), o.getDouble("amount"), o.getLong("timestamp")) }
        val goals = root.getJSONArray("goals").objects().map { o -> GoalEntity(o.getString("nutrientKey"), o.getDouble("target")) }
        val activities = root.getJSONArray("activities").objects().map { o -> DailyActivityEntity(
            date=o.getString("date"), totalCaloriesKcal=o.optDoubleOrNull("totalCaloriesKcal"),
            activeCaloriesKcal=o.optDoubleOrNull("activeCaloriesKcal"), steps=o.optLongOrNull("steps"),
            exerciseMinutes=o.optIntOrNull("exerciseMinutes"), sleepMinutes=o.optIntOrNull("sleepMinutes"),
            restingHr=o.optIntOrNull("restingHr"), syncedAt=o.getLong("syncedAt"),
        ) }
        db.withTransaction {
            db.entryDao().deleteAll(); db.goalDao().deleteAll(); db.activityDao().deleteAll()
            db.foodDao().getAll().forEach { db.foodDao().delete(it) }
            db.foodDao().insertAll(foods); db.entryDao().insertAll(entries)
            db.goalDao().insertAll(goals); db.activityDao().insertAll(activities)
        }
    }
}

fun LocalDate.startMillis(zone: ZoneId = ZoneId.systemDefault()) = atStartOfDay(zone).toInstant().toEpochMilli()
fun LocalDate.endMillis(zone: ZoneId = ZoneId.systemDefault()) = plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
fun Long.localDate(zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.localTime(zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

fun additivesFromJson(json: String): List<String> = runCatching {
    JSONArray(json).let { array -> (0 until array.length()).map { array.getString(it) } }
}.getOrDefault(emptyList())

fun additivesToJson(text: String): String = JSONArray(
    text.split('、', ',').map(String::trim).filter(String::isNotBlank).distinct()
).toString()

private fun JSONObject.putNullable(key: String, value: Any?) = put(key, value ?: JSONObject.NULL)
private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.optDoubleOrNull(key: String) = if (isNull(key)) null else getDouble(key)
private fun JSONObject.optLongOrNull(key: String) = if (isNull(key)) null else getLong(key)
private fun JSONObject.optIntOrNull(key: String) = if (isNull(key)) null else getInt(key)
