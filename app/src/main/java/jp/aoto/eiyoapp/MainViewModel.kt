package jp.aoto.eiyoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.aoto.eiyoapp.data.DailyActivityEntity
import jp.aoto.eiyoapp.data.EiyoRepository
import jp.aoto.eiyoapp.data.FoodEntity
import jp.aoto.eiyoapp.data.Nutrients
import jp.aoto.eiyoapp.domain.Exporter
import jp.aoto.eiyoapp.health.HealthAvailability
import jp.aoto.eiyoapp.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val entries: List<jp.aoto.eiyoapp.data.EntryWithFood> = emptyList(),
    val goals: List<jp.aoto.eiyoapp.data.GoalEntity> = emptyList(),
    val activity: DailyActivityEntity? = null,
) { val total: Nutrients get() = Exporter.sum(entries) }

class MainViewModel(
    application: Application,
    private val repository: EiyoRepository,
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
            MainViewModel(app, app.repository, app.health) as T
    }
}
