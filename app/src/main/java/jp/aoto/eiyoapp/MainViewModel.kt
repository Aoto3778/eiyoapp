package jp.aoto.eiyoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.aoto.eiyoapp.data.DailyActivityEntity
import jp.aoto.eiyoapp.data.EiyoRepository
import jp.aoto.eiyoapp.data.FoodEntity
import jp.aoto.eiyoapp.data.Nutrients
import jp.aoto.eiyoapp.data.BodyMetricEntity
import jp.aoto.eiyoapp.data.RecordedSet
import jp.aoto.eiyoapp.data.WorkoutExerciseEntity
import jp.aoto.eiyoapp.data.WorkoutRepository
import jp.aoto.eiyoapp.data.WorkoutSetEntity
import jp.aoto.eiyoapp.domain.Exporter
import jp.aoto.eiyoapp.domain.WorkoutExporter
import jp.aoto.eiyoapp.health.HealthAvailability
import jp.aoto.eiyoapp.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

data class HomeState(
    val entries: List<jp.aoto.eiyoapp.data.EntryWithFood> = emptyList(),
    val goals: List<jp.aoto.eiyoapp.data.GoalEntity> = emptyList(),
    val activity: DailyActivityEntity? = null,
) { val total: Nutrients get() = Exporter.sum(entries) }

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    private val repository: EiyoRepository,
    private val workoutRepository: WorkoutRepository,
    val health: HealthConnectManager,
) : AndroidViewModel(application) {
    private val today = LocalDate.now()
    private val query = MutableStateFlow("")
    val searchQuery = query
    val candidates = query.flatMapLatest(repository::candidates)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val foods = repository.foods.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val home = combine(
        repository.entries(today, today), repository.goals, repository.activities(today, today)
    ) { entries, goals, activities -> HomeState(entries, goals, activities.firstOrNull()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
    val history = repository.entries(today.minusDays(13), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historyActivities = repository.activities(today.minusDays(13), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val message = MutableStateFlow<String?>(null)
    val syncing = MutableStateFlow(false)
    val workoutExercises = workoutRepository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeWorkout = workoutRepository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val activeWorkoutSets = activeWorkout.flatMapLatest { session ->
        session?.let { workoutRepository.sessionSets(it.id) } ?: flowOf(emptyList<WorkoutSetEntity>())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workoutLastSets = workoutRepository.lastSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workoutSettings = workoutRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workoutSessions = workoutRepository.sessions(today.minusWeeks(16), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workoutHistorySets = workoutRepository.sets(today.minusMonths(6), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bodyMetrics = workoutRepository.bodyMetrics(today.minusMonths(6), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lastRecordedSet = MutableStateFlow<RecordedSet?>(null)

    fun setQuery(value: String) { query.value = value }
    fun clearMessage() { message.value = null }

    fun addEntry(food: FoodEntity, amount: Double, timestamp: Long) = viewModelScope.launch {
        if (amount <= 0) { message.value = "量は0より大きくしてください"; return@launch }
        repository.addEntry(food, amount, timestamp)
        query.value = ""
        message.value = "${food.name}を記録しました"
    }
    fun deleteEntry(id: Long) = viewModelScope.launch { repository.deleteEntry(id) }
    fun saveFood(food: FoodEntity, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (food.name.isBlank() || food.unit.isBlank()) { message.value = "名称と単位を入力してください"; return@launch }
        onSaved(repository.saveFood(food)); message.value = "食品を保存しました"
    }
    fun deleteFood(food: FoodEntity, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteFood(food); onDone(); message.value = "食品と関連記録を削除しました"
    }
    fun setGoal(key: String, value: String) = viewModelScope.launch {
        repository.setGoal(key, value.toDoubleOrNull()); message.value = "目標を保存しました"
    }
    fun saveManualActivity(activity: DailyActivityEntity) = viewModelScope.launch {
        repository.saveActivity(activity); message.value = "活動データを保存しました"
    }

    fun addWorkoutExercise(
        name: String,
        part: String,
        unit: String,
        stepKg: Double,
        note: String?,
        onSaved: (Long) -> Unit,
    ) = viewModelScope.launch {
        runCatching { workoutRepository.addExercise(name, part, unit, stepKg, note = note) }
            .onSuccess { id -> message.value = "種目を登録しました"; onSaved(id) }
            .onFailure { message.value = "種目を登録できませんでした: ${it.message}" }
    }

    fun renameWorkoutExercise(id: Long, name: String) = viewModelScope.launch {
        runCatching { workoutRepository.renameExercise(id, name) }
            .onSuccess { message.value = "種目名を変更しました。過去の記録も引き継ぎ済みです" }
            .onFailure { message.value = "名前を変更できませんでした" }
    }

    fun mergeWorkoutExercises(sourceId: Long, targetId: Long) = viewModelScope.launch {
        runCatching { workoutRepository.mergeExercises(sourceId, targetId) }
            .onSuccess { message.value = "種目を統合しました。記録は1本になります" }
            .onFailure { message.value = "種目を統合できませんでした: ${it.message}" }
    }

    fun startWorkout(exerciseId: Long, onStarted: () -> Unit = {}) = viewModelScope.launch {
        runCatching { workoutRepository.startOrResume(exerciseId) }
            .onSuccess { onStarted() }
            .onFailure { message.value = "ワークアウトを開始できませんでした" }
    }

    fun switchWorkoutExercise(exerciseId: Long) = viewModelScope.launch {
        activeWorkout.value?.let { workoutRepository.switchExercise(it.id, exerciseId) }
    }

    fun recordWorkoutSet(
        exercise: WorkoutExerciseEntity,
        weightKg: Double?,
        reps: Int?,
        seconds: Int?,
    ) = viewModelScope.launch {
        val session = activeWorkout.value ?: return@launch
        runCatching { workoutRepository.recordSet(session.id, exercise, weightKg, reps, seconds) }
            .onSuccess { result ->
                lastRecordedSet.value = result
                message.value = if (result.set.isPr) "前回超え。ちゃんと強くなっています" else "セット${result.set.setNo}を記録。ここまでで十分えらい。"
            }.onFailure { message.value = "セットを記録できませんでした: ${it.message}" }
    }

    fun setWorkoutRpe(set: WorkoutSetEntity, rpe: Int?) = viewModelScope.launch {
        workoutRepository.setRpe(set, rpe)
        lastRecordedSet.value = null
    }

    fun completeWorkout(conditionNote: String, onCompleted: () -> Unit) = viewModelScope.launch {
        val session = activeWorkout.value ?: return@launch
        runCatching { workoutRepository.completeSession(session.id, conditionNote) }
            .onSuccess { message.value = "今日の記録を保存しました"; lastRecordedSet.value = null; onCompleted() }
            .onFailure { message.value = it.message ?: "保存できませんでした" }
    }

    fun updateWorkoutSetting(key: String, value: String) = viewModelScope.launch {
        workoutRepository.updateSetting(key, value)
        message.value = "設定を保存しました"
    }

    fun saveBodyMetric(metric: BodyMetricEntity) = viewModelScope.launch {
        workoutRepository.saveBodyMetric(metric)
        message.value = "からだの記録を保存しました"
    }

    fun syncHealth(force: Boolean = false) = viewModelScope.launch {
        if (syncing.value) return@launch
        val prefs = getApplication<Application>().getSharedPreferences("sync", 0)
        val last = prefs.getLong("last", 0)
        if (!force && System.currentTimeMillis() - last < 5 * 60_000) return@launch
        if (health.availability() != HealthAvailability.AVAILABLE || !health.hasAllPermissions()) {
            message.value = "ヘルスコネクトの読み取り権限を設定してください"; return@launch
        }
        syncing.value = true
        runCatching {
            (0L..7L).forEach { offset ->
                val date = LocalDate.now().minusDays(offset)
                repository.saveActivity(health.readDay(date).entity(date))
            }
            prefs.edit().putLong("last", System.currentTimeMillis()).apply()
        }.onSuccess { message.value = "活動データを同期しました" }
            .onFailure { message.value = "同期できませんでした: ${it.message ?: "不明なエラー"}" }
        syncing.value = false
    }

    suspend fun export(from: LocalDate, to: LocalDate, markdown: Boolean): Pair<String, ByteArray> {
        val data = repository.exportData(from, to)
        return if (markdown) {
            val text = Exporter.markdown(from, to, data); text to text.toByteArray()
        } else {
            val bytes = Exporter.csv(data); bytes.toString(Charsets.UTF_8) to bytes
        }
    }
    suspend fun exportWorkout(from: LocalDate, to: LocalDate, markdown: Boolean): Pair<String, ByteArray> {
        val workout = workoutRepository.exportData(from, to)
        val nutrition = repository.exportData(from, to)
        return if (markdown) {
            val text = WorkoutExporter.markdown(from, to, workout, nutrition)
            text to text.toByteArray()
        } else {
            val bytes = WorkoutExporter.csv(workout, nutrition)
            bytes.toString(Charsets.UTF_8) to bytes
        }
    }
    suspend fun backup() = repository.backupJson()
    suspend fun restore(text: String) = repository.restoreJson(text)
    fun importGarminCsv(text: String) = viewModelScope.launch {
        runCatching { jp.aoto.eiyoapp.domain.GarminCsvImporter.parse(text) }
            .onSuccess { rows -> rows.forEach { repository.saveActivity(it) }; message.value = "Garmin CSVから${rows.size}日分を取り込みました" }
            .onFailure { message.value = "CSVを取り込めませんでした: ${it.message}" }
    }

    class Factory(private val app: EiyoApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(app, app.repository, app.workoutRepository, app.health) as T
    }
}
