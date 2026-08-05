package jp.aoto.eiyoapp.data

import androidx.room.withTransaction
import jp.aoto.eiyoapp.domain.WorkoutMath
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class RecordedSet(val set: WorkoutSetEntity, val previous: WorkoutSetEntity?)

class WorkoutRepository(private val db: AppDatabase) {
    private val dao = db.workoutDao()
    val exercises = dao.observeExercises()
    val activeSession = dao.observeActiveSession()
    val lastSets = dao.observeLastSets()
    val settings = dao.observeSettings()

    fun sessions(from: LocalDate, to: LocalDate) = dao.observeSessions(from.toString(), to.toString())
    fun sessionSets(id: Long): Flow<List<WorkoutSetEntity>> = dao.observeSessionSets(id)
    fun bodyMetrics(from: LocalDate, to: LocalDate) = dao.observeBodyMetrics(from.toString(), to.toString())
    fun sets(from: LocalDate, to: LocalDate) = dao.observeSetsWithExercise(from.toString(), to.toString())

    suspend fun addExercise(
        name: String,
        part: String,
        unit: String,
        stepKg: Double,
        photoUri: String? = null,
        note: String? = null,
    ): Long {
        val resolvedName = name.trim().ifBlank { "${part}のマシン" }
        val provisional = Regex("マシン[A-Z]?$", RegexOption.IGNORE_CASE).containsMatchIn(resolvedName)
        return dao.insertExercise(WorkoutExerciseEntity(
            name = resolvedName,
            part = part,
            unit = unit,
            stepKg = stepKg,
            provisional = provisional,
            photoUri = photoUri,
            note = note?.trim()?.takeIf(String::isNotBlank),
        ))
    }

    suspend fun renameExercise(id: Long, name: String) {
        val exercise = requireNotNull(dao.getExercise(id))
        val clean = name.trim()
        require(clean.isNotBlank())
        dao.updateExercise(exercise.copy(
            name = clean,
            provisional = Regex("マシン[A-Z]?$", RegexOption.IGNORE_CASE).containsMatchIn(clean),
        ))
    }

    suspend fun mergeExercises(sourceId: Long, targetId: Long) = db.withTransaction {
        require(sourceId != targetId)
        requireNotNull(dao.getExercise(sourceId))
        requireNotNull(dao.getExercise(targetId))
        dao.moveSets(sourceId, targetId)
        dao.markMerged(sourceId, targetId)
    }

    suspend fun startOrResume(exerciseId: Long): WorkoutSessionEntity = db.withTransaction {
        val active = dao.getActiveSession()
        if (active != null) {
            val updated = active.copy(activeExerciseId = exerciseId)
            dao.updateSession(updated)
            updated
        } else {
            val now = System.currentTimeMillis()
            val id = dao.insertSession(WorkoutSessionEntity(
                date = LocalDate.now().toString(),
                startedAt = now,
                activeExerciseId = exerciseId,
            ))
            requireNotNull(dao.getSession(id))
        }
    }

    suspend fun switchExercise(sessionId: Long, exerciseId: Long) {
        val session = requireNotNull(dao.getSession(sessionId))
        dao.updateSession(session.copy(activeExerciseId = exerciseId))
    }

    suspend fun recordSet(
        sessionId: Long,
        exercise: WorkoutExerciseEntity,
        weightKg: Double?,
        reps: Int?,
        seconds: Int?,
    ): RecordedSet = db.withTransaction {
        val sessionSets = dao.getSessionSets(sessionId).filter { it.exerciseId == exercise.id }
        val setNo = sessionSets.size + 1
        val previous = dao.getPreviousSet(exercise.id, setNo, sessionId)
            ?: dao.getLatestSet(exercise.id, sessionId)
        val draft = WorkoutSetEntity(
            sessionId = sessionId,
            exerciseId = exercise.id,
            setNo = setNo,
            weightKg = weightKg.takeIf { exercise.unit == "kg" },
            reps = reps.takeIf { exercise.unit != "sec" },
            seconds = seconds.takeIf { exercise.unit == "sec" },
        )
        val saved = draft.copy(isPr = WorkoutMath.isPersonalRecord(exercise.unit, draft, previous))
        val id = dao.insertSet(saved)
        RecordedSet(saved.copy(id = id), previous)
    }

    suspend fun setRpe(set: WorkoutSetEntity, rpe: Int?) = dao.updateSet(set.copy(rpe = rpe))

    suspend fun completeSession(id: Long, conditionNote: String) {
        val session = requireNotNull(dao.getSession(id))
        require(dao.getSessionSets(id).isNotEmpty()) { "1セット以上記録してください" }
        dao.updateSession(session.copy(
            endedAt = System.currentTimeMillis(),
            conditionNote = conditionNote.trim(),
            completed = true,
        ))
    }

    suspend fun updateSetting(key: String, value: String) =
        dao.upsertSetting(WorkoutSettingEntity(key, value))

    suspend fun saveBodyMetric(metric: BodyMetricEntity) = dao.upsertBodyMetric(metric)

    suspend fun exportData(from: LocalDate, to: LocalDate) = WorkoutExportData(
        exercises = dao.getExercises(),
        sessions = dao.getSessions(from.toString(), to.toString()),
        sets = dao.getSetsWithExercise(from.toString(), to.toString()),
        bodyMetrics = dao.getBodyMetrics(from.toString(), to.toString()),
        settings = dao.getSettings(),
    )
}

data class WorkoutExportData(
    val exercises: List<WorkoutExerciseEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<WorkoutSetWithExercise>,
    val bodyMetrics: List<BodyMetricEntity>,
    val settings: List<WorkoutSettingEntity>,
)
