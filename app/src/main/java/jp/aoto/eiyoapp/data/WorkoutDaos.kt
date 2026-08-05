package jp.aoto.eiyoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_exercises WHERE mergedInto IS NULL ORDER BY createdAt, name")
    fun observeExercises(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE mergedInto IS NULL ORDER BY createdAt, name")
    suspend fun getExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getExercise(id: Long): WorkoutExerciseEntity?

    @Insert suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercises(items: List<WorkoutExerciseEntity>)
    @Update suspend fun updateExercise(exercise: WorkoutExerciseEntity)

    @Query("UPDATE workout_sets SET exerciseId = :targetId WHERE exerciseId = :sourceId")
    suspend fun moveSets(sourceId: Long, targetId: Long)

    @Query("UPDATE workout_exercises SET mergedInto = :targetId WHERE id = :sourceId")
    suspend fun markMerged(sourceId: Long, targetId: Long)

    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY startedAt DESC")
    fun observeSessions(from: String, to: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY startedAt")
    suspend fun getSessions(from: String, to: String): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Insert suspend fun insertSession(session: WorkoutSessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSessions(items: List<WorkoutSessionEntity>)
    @Update suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY recordedAt, setNo")
    fun observeSessionSets(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY recordedAt, setNo")
    suspend fun getSessionSets(sessionId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets ORDER BY recordedAt")
    suspend fun getAllSets(): List<WorkoutSetEntity>

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND sessionId != :excludeSessionId
        ORDER BY recordedAt DESC LIMIT 1
    """)
    suspend fun getLatestSet(exerciseId: Long, excludeSessionId: Long): WorkoutSetEntity?

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND setNo = :setNo AND sessionId != :excludeSessionId
        ORDER BY recordedAt DESC LIMIT 1
    """)
    suspend fun getPreviousSet(exerciseId: Long, setNo: Int, excludeSessionId: Long): WorkoutSetEntity?

    @Query("""
        SELECT ws.exerciseId, ws.weightKg, ws.reps, ws.seconds, ws.recordedAt
        FROM workout_sets ws
        INNER JOIN (
            SELECT exerciseId, MAX(recordedAt) AS latest FROM workout_sets GROUP BY exerciseId
        ) latest ON latest.exerciseId = ws.exerciseId AND latest.latest = ws.recordedAt
    """)
    fun observeLastSets(): Flow<List<ExerciseLastSet>>

    @Query("""
        SELECT ws.id, ws.sessionId, ws.exerciseId, ws.setNo, ws.weightKg, ws.reps,
               ws.seconds, ws.rpe, ws.isPr, ws.recordedAt,
               we.name AS exerciseName, we.part, we.unit, we.stepKg
        FROM workout_sets ws JOIN workout_exercises we ON we.id = ws.exerciseId
        JOIN workout_sessions s ON s.id = ws.sessionId
        WHERE s.date BETWEEN :from AND :to
        ORDER BY ws.recordedAt
    """)
    suspend fun getSetsWithExercise(from: String, to: String): List<WorkoutSetWithExercise>

    @Query("""
        SELECT ws.id, ws.sessionId, ws.exerciseId, ws.setNo, ws.weightKg, ws.reps,
               ws.seconds, ws.rpe, ws.isPr, ws.recordedAt,
               we.name AS exerciseName, we.part, we.unit, we.stepKg
        FROM workout_sets ws JOIN workout_exercises we ON we.id = ws.exerciseId
        JOIN workout_sessions s ON s.id = ws.sessionId
        WHERE s.date BETWEEN :from AND :to
        ORDER BY ws.recordedAt
    """)
    fun observeSetsWithExercise(from: String, to: String): Flow<List<WorkoutSetWithExercise>>

    @Insert suspend fun insertSet(set: WorkoutSetEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSets(items: List<WorkoutSetEntity>)
    @Update suspend fun updateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets") suspend fun deleteAllSets()
    @Query("DELETE FROM workout_sessions") suspend fun deleteAllSessions()
    @Query("DELETE FROM workout_exercises") suspend fun deleteAllExercises()

    @Query("SELECT * FROM body_metrics WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBodyMetrics(from: String, to: String): Flow<List<BodyMetricEntity>>
    @Query("SELECT * FROM body_metrics WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getBodyMetrics(from: String, to: String): List<BodyMetricEntity>
    @Query("SELECT * FROM body_metrics ORDER BY date") suspend fun getAllBodyMetrics(): List<BodyMetricEntity>
    @Upsert suspend fun upsertBodyMetric(metric: BodyMetricEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBodyMetrics(items: List<BodyMetricEntity>)
    @Query("DELETE FROM body_metrics") suspend fun deleteAllBodyMetrics()

    @Query("SELECT * FROM workout_settings") fun observeSettings(): Flow<List<WorkoutSettingEntity>>
    @Query("SELECT * FROM workout_settings") suspend fun getSettings(): List<WorkoutSettingEntity>
    @Upsert suspend fun upsertSetting(setting: WorkoutSettingEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSettings(items: List<WorkoutSettingEntity>)
    @Query("DELETE FROM workout_settings") suspend fun deleteAllSettings()
}
