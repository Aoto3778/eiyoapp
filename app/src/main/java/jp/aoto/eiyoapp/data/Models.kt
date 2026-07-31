package jp.aoto.eiyoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

data class Nutrients(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val sugar: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val salt: Double = 0.0,
    val water: Double = 0.0,
    val vitC: Double = 0.0,
    val vitD: Double = 0.0,
    val vitB: Double = 0.0,
    val ca: Double = 0.0,
    val fe: Double = 0.0,
    val mg: Double = 0.0,
) {
    operator fun times(amount: Double) = Nutrients(
        kcal * amount, protein * amount, sugar * amount, fat * amount, fiber * amount,
        salt * amount, water * amount, vitC * amount, vitD * amount, vitB * amount,
        ca * amount, fe * amount, mg * amount,
    )
    operator fun plus(other: Nutrients) = Nutrients(
        kcal + other.kcal, protein + other.protein, sugar + other.sugar, fat + other.fat,
        fiber + other.fiber, salt + other.salt, water + other.water, vitC + other.vitC,
        vitD + other.vitD, vitB + other.vitB, ca + other.ca, fe + other.fe, mg + other.mg,
    )
    fun value(key: String): Double = when (key) {
        "kcal" -> kcal; "protein" -> protein; "sugar" -> sugar; "fat" -> fat
        "fiber" -> fiber; "salt" -> salt; "water" -> water; "vitC" -> vitC
        "vitD" -> vitD; "vitB" -> vitB; "ca" -> ca; "fe" -> fe; "mg" -> mg
        else -> 0.0
    }
}

data class NutrientSpec(val key: String, val label: String, val unit: String)

val nutrientSpecs = listOf(
    NutrientSpec("kcal", "エネルギー", "kcal"), NutrientSpec("protein", "たんぱく質", "g"),
    NutrientSpec("sugar", "糖質", "g"), NutrientSpec("fat", "脂質", "g"),
    NutrientSpec("fiber", "食物繊維", "g"), NutrientSpec("salt", "塩分", "g"),
    NutrientSpec("water", "水分", "ml"), NutrientSpec("vitC", "ビタミンC", "mg"),
    NutrientSpec("vitD", "ビタミンD", "µg"), NutrientSpec("vitB", "ビタミンB群", "mg"),
    NutrientSpec("ca", "カルシウム", "mg"), NutrientSpec("fe", "鉄", "mg"),
    NutrientSpec("mg", "マグネシウム", "mg"),
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val unitNote: String? = null,
    val lastAmount: Double = 1.0,
    val additivesJson: String = "[]",
    val perKcal: Double = 0.0,
    val perProtein: Double = 0.0,
    val perSugar: Double = 0.0,
    val perFat: Double = 0.0,
    val perFiber: Double = 0.0,
    val perSalt: Double = 0.0,
    val perWater: Double = 0.0,
    val perVitC: Double = 0.0,
    val perVitD: Double = 0.0,
    val perVitB: Double = 0.0,
    val perCa: Double = 0.0,
    val perFe: Double = 0.0,
    val perMg: Double = 0.0,
) {
    fun nutrients() = Nutrients(perKcal, perProtein, perSugar, perFat, perFiber, perSalt,
        perWater, perVitC, perVitD, perVitB, perCa, perFe, perMg)
}

@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(
        entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("foodId"), Index("timestamp")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val amount: Double,
    val timestamp: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(@PrimaryKey val nutrientKey: String, val target: Double)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String,
    val totalCaloriesKcal: Double? = null,
    val activeCaloriesKcal: Double? = null,
    val steps: Long? = null,
    val exerciseMinutes: Int? = null,
    val sleepMinutes: Int? = null,
    val restingHr: Int? = null,
    val syncedAt: Long = System.currentTimeMillis(),
)

data class FoodWithCount(
    val id: Long,
    val name: String,
    val unit: String,
    val unitNote: String?,
    val lastAmount: Double,
    val additivesJson: String,
    val perKcal: Double,
    val perProtein: Double,
    val perSugar: Double,
    val perFat: Double,
    val perFiber: Double,
    val perSalt: Double,
    val perWater: Double,
    val perVitC: Double,
    val perVitD: Double,
    val perVitB: Double,
    val perCa: Double,
    val perFe: Double,
    val perMg: Double,
    val usageCount: Long,
) {
    fun food() = FoodEntity(id, name, unit, unitNote, lastAmount, additivesJson, perKcal,
        perProtein, perSugar, perFat, perFiber, perSalt, perWater, perVitC, perVitD,
        perVitB, perCa, perFe, perMg)
}

data class EntryWithFood(
    val entryId: Long,
    val foodId: Long,
    val amount: Double,
    val timestamp: Long,
    val name: String,
    val unit: String,
    val additivesJson: String,
    val perKcal: Double,
    val perProtein: Double,
    val perSugar: Double,
    val perFat: Double,
    val perFiber: Double,
    val perSalt: Double,
    val perWater: Double,
    val perVitC: Double,
    val perVitD: Double,
    val perVitB: Double,
    val perCa: Double,
    val perFe: Double,
    val perMg: Double,
) {
    fun nutrients() = Nutrients(perKcal, perProtein, perSugar, perFat, perFiber, perSalt,
        perWater, perVitC, perVitD, perVitB, perCa, perFe, perMg) * amount
}
