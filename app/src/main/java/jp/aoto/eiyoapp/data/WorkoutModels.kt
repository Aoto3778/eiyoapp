package jp.aoto.eiyoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    indices = [Index("mergedInto")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val part: String,
    val unit: String = "kg",
    val stepKg: Double = 2.5,
    val provisional: Boolean = false,
    val photoUri: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val mergedInto: Long? = null,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [ForeignKey(
        entity = WorkoutExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["activeExerciseId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("date"), Index("activeExerciseId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val activeExerciseId: Long? = null,
    val conditionNote: String = "",
    val completed: Boolean = false,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index("recordedAt")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNo: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val rpe: Int? = null,
    val isPr: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val date: String,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val armCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val thighCm: Double? = null,
)

@Entity(tableName = "workout_settings")
data class WorkoutSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class WorkoutSetWithExercise(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val setNo: Int,
    val weightKg: Double?,
    val reps: Int?,
    val seconds: Int?,
    val rpe: Int?,
    val isPr: Boolean,
    val recordedAt: Long,
    val exerciseName: String,
    val part: String,
    val unit: String,
    val stepKg: Double,
)

data class ExerciseLastSet(
    val exerciseId: Long,
    val weightKg: Double?,
    val reps: Int?,
    val seconds: Int?,
    val recordedAt: Long,
)

