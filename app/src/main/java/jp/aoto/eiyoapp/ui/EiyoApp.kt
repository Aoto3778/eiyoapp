package jp.aoto.eiyoapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.aoto.eiyoapp.MainViewModel
import jp.aoto.eiyoapp.data.DailyActivityEntity
import jp.aoto.eiyoapp.data.EntryWithFood
import jp.aoto.eiyoapp.data.FoodEntity
import jp.aoto.eiyoapp.data.FoodWithCount
import jp.aoto.eiyoapp.data.GoalEntity
import jp.aoto.eiyoapp.data.Nutrients
import jp.aoto.eiyoapp.data.additivesFromJson
import jp.aoto.eiyoapp.data.additivesToJson
import jp.aoto.eiyoapp.data.localDate
import jp.aoto.eiyoapp.data.localTime
import jp.aoto.eiyoapp.data.nutrientSpecs
import jp.aoto.eiyoapp.domain.Exporter
import jp.aoto.eiyoapp.health.HealthAvailability
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class Tab(val label: String) { TODAY("今日"), FOOD("食事"), WORKOUT("筋トレ"), ANALYSIS("分析"), MORE("その他") }
private sealed interface Overlay {
    data object Goals : Overlay
    data class FoodEdit(val food: FoodEntity? = null, val preset: String = "") : Overlay
    data object HealthGuide : Overlay
    data object WorkoutSession : Overlay
}

@Composable
fun EiyoApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val snack = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.clearMessage() } }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { vm.syncHealth(force=true) }

    Scaffold(
        modifier=Modifier.fillMaxSize(), containerColor=Paper, snackbarHost={ SnackbarHost(snack) },
        bottomBar={
            if (overlay == null) NavigationBar(containerColor=Paper, modifier=Modifier.navigationBarsPadding()) {
                Tab.entries.forEach { item -> NavigationBarItem(
                    selected=tab == item, onClick={ tab=item }, icon={}, label={ Text(item.label) },
                    colors=androidx.compose.material3.NavigationBarItemDefaults.colors(indicatorColor=Color.Transparent, selectedTextColor=Accent, unselectedTextColor=Muted),
                ) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).statusBarsPadding().fillMaxSize()) {
            when (val screen = overlay) {
                Overlay.Goals -> GoalsScreen(vm, onBack={ overlay=null })
                is Overlay.FoodEdit -> FoodEditScreen(vm, screen.food, screen.preset, onBack={ overlay=null })
                Overlay.HealthGuide -> HealthGuideScreen(vm, onBack={ overlay=null }, onPermission={ permissionLauncher.launch(vm.health.permissions) })
                Overlay.WorkoutSession -> WorkoutSessionScreen(vm, onBack={ overlay=null; tab=Tab.WORKOUT })
                null -> when (tab) {
                    Tab.TODAY -> HomeScreen(vm, onGoals={ overlay=Overlay.Goals }, onHealthGuide={ overlay=Overlay.HealthGuide }, onWorkout={ overlay=Overlay.WorkoutSession })
                    Tab.FOOD -> FoodAreaScreen(vm, onNewFood={ overlay=Overlay.FoodEdit(preset=it) }, onEdit={ overlay=Overlay.FoodEdit(it) })
                    Tab.WORKOUT -> WorkoutHubScreen(vm, onOpenSession={ overlay=Overlay.WorkoutSession })
                    Tab.ANALYSIS -> AnalysisAreaScreen(vm)
                    Tab.MORE -> MoreAreaScreen(vm, onGoals={ overlay=Overlay.Goals }, onHealthGuide={ overlay=Overlay.HealthGuide })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel, onGoals: () -> Unit, onHealthGuide: () -> Unit, onWorkout: () -> Unit) {
    val state by vm.home.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val activeWorkout by vm.activeWorkout.collectAsStateWithLifecycle()
    val exercises by vm.workoutExercises.collectAsStateWithLifecycle()
    val sessions by vm.workoutSessions.collectAsStateWithLifecycle()
    var manual by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(22.dp)) {
        item {
            Text("私のための栄養記録", color=Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.Bottom) {
                Text("${today.monthValue}月${today.dayOfMonth}日", style=androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.JAPANESE), color=Muted)
            }
        }
        item { GoalProgress(state.goals, state.total, onGoals) }
        item { ActivityCard(state.total.kcal, state.activity, syncing, onSync={ vm.syncHealth(force=true) }, onEdit={ manual=true }, onGuide=onHealthGuide) }
        item {
            OutlinedCard(shape=RectangleShape, border=BorderStroke(1.dp, Accent), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("今週の筋トレ", color=Accent)
                        val monday=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        Text("${sessions.count { it.completed && !LocalDate.parse(it.date).isBefore(monday) }} / 2回", color=Accent)
                    }
                    Text(activeWorkout?.let { "実行中のワークアウトがあります" } ?: exercises.firstOrNull()?.let { "次は ${it.name}" } ?: "最初の種目を登録しましょう", color=Muted)
                    LineButton(if(activeWorkout!=null) "ワークアウトに戻る" else "筋トレを始める", {
                        if(activeWorkout!=null) onWorkout() else exercises.firstOrNull()?.let { vm.startWorkout(it.id,onWorkout) }
                    }, Modifier.fillMaxWidth(), activeWorkout!=null || exercises.isNotEmpty())
                }
            }
        }
        item {
            SectionTitle("今日の栄養素")
            nutrientSpecs.forEach { spec ->
                val goal = state.goals.firstOrNull { it.nutrientKey == spec.key }?.target
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(spec.label)
                    Text("${number(state.total.value(spec.key))} ${spec.unit}${goal?.let { "  ${((state.total.value(spec.key)/it)*100).toInt()}%" } ?: ""}")
                }
                HorizontalDivider(color=Rule)
            }
        }
        item {
            SectionTitle("今日摂取した添加物")
            val additives = state.entries.flatMap { additivesFromJson(it.additivesJson) }.groupingBy { it }.eachCount()
            if (additives.isEmpty()) Text("なし", color=Muted) else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                additives.forEach { (name, count) -> AssistChip(onClick={}, label={ Text("$name ×$count") }) }
            }
        }
        item { SectionTitle("本日の記録") }
        if (state.entries.isEmpty()) item { Text("まだ記録がありません", color=Muted) }
        items(state.entries, key={ it.entryId }) { entry -> EntryRow(entry) { vm.deleteEntry(entry.entryId) } }
    }
    if (manual) ManualActivityDialog(state.activity, onDismiss={manual=false}, onSave={ vm.saveManualActivity(it); manual=false })
}

@Composable
private fun GoalProgress(goals: List<GoalEntity>, total: Nutrients, onEdit: () -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text("目標の達成度", color=Muted); TextButtonLike("目標を編集", onEdit)
        }
        goals.forEach { goal ->
            val spec = nutrientSpecs.firstOrNull { it.key == goal.nutrientKey } ?: return@forEach
            val value = total.value(goal.nutrientKey); val percent = (value / goal.target * 100).toInt()
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                Text(spec.label); Text("${number(value)} / ${number(goal.target)} ${spec.unit}")
            }
            ProgressLine((value / goal.target).toFloat())
            Text("$percent%", color=Muted, modifier=Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun ActivityCard(intake: Double, activity: DailyActivityEntity?, syncing: Boolean, onSync: () -> Unit, onEdit: () -> Unit, onGuide: () -> Unit) {
    OutlinedCard(shape=RectangleShape, border=BorderStroke(1.dp, Rule), modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                Text("摂取と消費"); Text("Garmin 連携", color=Muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                LabeledValue("摂取 kcal", number(intake), Modifier.weight(1f))
                LabeledValue("消費 kcal", activity?.totalCaloriesKcal?.let(::number) ?: "—", Modifier.weight(1f))
                LabeledValue("収支", activity?.totalCaloriesKcal?.let { "%+d".format((intake-it).toInt()) } ?: "—", Modifier.weight(1f))
            }
            HorizontalDivider(color=Rule)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                LabeledValue("歩数", activity?.steps?.toString() ?: "—", Modifier.weight(1f))
                LabeledValue("運動", activity?.exerciseMinutes?.let { "${it}分" } ?: "—", Modifier.weight(1f))
                LabeledValue("睡眠", activity?.sleepMinutes?.let { "%d:%02d".format(it/60,it%60) } ?: "—", Modifier.weight(1f))
                LabeledValue("安静時心拍", activity?.restingHr?.toString() ?: "—", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Text(activity?.syncedAt?.let { "最終同期 ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}" } ?: "未同期", color=Muted)
                Row { TextButtonLike("手入力", onEdit); TextButtonLike("設定", onGuide); TextButtonLike(if(syncing) "同期中" else "同期", onSync, !syncing) }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: EntryWithFood, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.width(58.dp)) { Text(entry.timestamp.localTime().format(DateTimeFormatter.ofPattern("HH:mm"))); Text(Exporter.timeBand(entry.timestamp.localTime().hour), color=Muted) }
        Column(Modifier.weight(1f)) { Text(entry.name); Text("×${number(entry.amount)}${entry.unit}  ${number(entry.nutrients().kcal)} kcal", color=Muted) }
        IconButton(onClick=onDelete) { Icon(Icons.Outlined.Close, "削除") }
    }
    HorizontalDivider(color=Rule)
}

@Composable
private fun FoodAreaScreen(vm: MainViewModel, onNewFood: (String) -> Unit, onEdit: (FoodEntity) -> Unit) {
    var section by rememberSaveable { mutableStateOf("記録") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LineButton("記録", {section="記録"}, Modifier.weight(1f).height(44.dp))
            LineButton("食品", {section="食品"}, Modifier.weight(1f).height(44.dp))
        }
        if(section=="記録") LogScreen(vm,onNewFood) else FoodsScreen(vm,onEdit,{onNewFood("")})
    }
}

@Composable
private fun AnalysisAreaScreen(vm: MainViewModel) {
    var section by rememberSaveable { mutableStateOf("筋トレ") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LineButton("筋トレ", {section="筋トレ"}, Modifier.weight(1f).height(44.dp))
            LineButton("栄養", {section="栄養"}, Modifier.weight(1f).height(44.dp))
        }
        if(section=="筋トレ") WorkoutProgressScreen(vm) else HistoryScreen(vm)
    }
}

@Composable
private fun MoreAreaScreen(vm: MainViewModel, onGoals: () -> Unit, onHealthGuide: () -> Unit) {
    var section by rememberSaveable { mutableStateOf("設定") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LineButton("設定", {section="設定"}, Modifier.weight(1f).height(44.dp))
            LineButton("書き出し", {section="書き出し"}, Modifier.weight(1f).height(44.dp))
        }
        if(section=="書き出し") ExportScreen(vm) else WorkoutSettingsScreen(vm,onHealthGuide,onGoals)
    }
}

@Composable
private fun LogScreen(vm: MainViewModel, onNewFood: (String) -> Unit) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    var amount by remember { mutableStateOf("1") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            PageHeader("食事を記録")
            Text("現在 ${timestamp.localTime().format(DateTimeFormatter.ofPattern("HH:mm"))}", color=Muted)
        }
        item { PaperField(query, { vm.setQuery(it); selected=null }, Modifier.fillMaxWidth(), placeholder="食品名を入力（例：ナ）") }
        if (selected == null) {
            item { Text(if(query.isBlank()) "よく使う食品" else "検索結果", color=Muted) }
            items(candidates, key={ it.id }) { candidate -> FoodCandidate(candidate) {
                selected=candidate.food(); amount=number(candidate.lastAmount); timestamp=System.currentTimeMillis()
            } }
            if (query.isNotBlank() && candidates.none { it.name == query.trim() }) item {
                LineButton("「${query.trim()}」を新しい食品として登録", { onNewFood(query.trim()) }, Modifier.fillMaxWidth())
            }
        } else item {
            val food = selected!!; val numericAmount = amount.toDoubleOrNull() ?: 0.0; val preview = food.nutrients() * numericAmount
            Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                OutlinedCard(shape=RectangleShape, border=BorderStroke(1.dp, Rule), modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(food.name, style=androidx.compose.material3.MaterialTheme.typography.titleLarge); Text("1${food.unit} ${food.unitNote.orEmpty()}", color=Muted) }
                        Text("${number(food.perKcal)} kcal　P ${number(food.perProtein)} g　糖質 ${number(food.perSugar)} g　脂質 ${number(food.perFat)} g")
                        val additives = additivesFromJson(food.additivesJson)
                        if (additives.isNotEmpty()) Text(additives.joinToString("・"), color=Muted)
                    }
                }
                Text("量（${food.unit}）", color=Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    LineButton("−", { amount=number((numericAmount-.5).coerceAtLeast(.5)) })
                    PaperField(amount, { amount=it }, Modifier.weight(1f))
                    LineButton("＋", { amount=number(numericAmount+.5) })
                }
                LineButton("時刻 ${timestamp.localTime().format(DateTimeFormatter.ofPattern("HH:mm"))}", { showTimePicker(context, timestamp) { timestamp=it } }, Modifier.fillMaxWidth())
                Text("この量で：${number(preview.kcal)} kcal・たんぱく質 ${number(preview.protein)} g・糖質 ${number(preview.sugar)} g・水分 ${number(preview.water)} ml")
                LineButton("保存", { vm.addEntry(food, numericAmount, timestamp); selected=null; amount="1" }, Modifier.fillMaxWidth(), numericAmount>0)
                TextButtonLike("選び直す", { selected=null })
            }
        }
    }
}

@Composable
private fun FoodCandidate(candidate: FoodWithCount, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=11.dp), horizontalArrangement=Arrangement.SpaceBetween) {
        Text(candidate.name)
        Text("1${candidate.unit} ${candidate.unitNote.orEmpty()}・${number(candidate.perKcal)} kcal", color=Muted)
    }
    HorizontalDivider(color=Rule)
}

@Composable private fun SectionTitle(text: String) { Text(text, color=Muted); Spacer(Modifier.height(6.dp)); HorizontalDivider(color=Rule) }

private fun showTimePicker(context: Context, current: Long, onPicked: (Long) -> Unit) {
    val local = Instant.ofEpochMilli(current).atZone(ZoneId.systemDefault()).toLocalDateTime()
    TimePickerDialog(context, { _, hour, minute ->
        onPicked(LocalDateTime.of(local.toLocalDate(), LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }, local.hour, local.minute, true).show()
}

@Composable
private fun FoodsScreen(vm: MainViewModel, onEdit: (FoodEntity) -> Unit, onNew: () -> Unit) {
    val foods by vm.foods.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp)) {
        item { PageHeader("食品ライブラリ", "＋追加", onNew); Text("一度登録すれば、次回からは名前を1文字だけで栄養情報がすべて入ります。", color=Muted, modifier=Modifier.padding(vertical=12.dp)) }
        items(foods, key={it.id}) { item ->
            Row(Modifier.fillMaxWidth().clickable { onEdit(item.food()) }.padding(vertical=11.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                Text(item.name)
                Text("1${item.unit}・${number(item.perKcal)} kcal・P ${number(item.perProtein)}g　${item.usageCount}回記録", color=if(item.usageCount>0) Accent else Muted)
            }
            HorizontalDivider(color=Rule)
        }
    }
}

@Composable
private fun FoodEditScreen(vm: MainViewModel, original: FoodEntity?, preset: String, onBack: () -> Unit) {
    var name by remember(original, preset) { mutableStateOf(original?.name ?: preset) }
    var unit by remember(original) { mutableStateOf(original?.unit ?: "個") }
    var note by remember(original) { mutableStateOf(original?.unitNote.orEmpty()) }
    var additives by remember(original) { mutableStateOf(original?.additivesJson?.let { additivesFromJson(it).joinToString("、") }.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val values = remember(original) { mutableStateMapOf<String,String>().apply {
        nutrientSpecs.forEach { spec -> put(spec.key, original?.nutrients()?.value(spec.key)?.let(::number)?.takeUnless { it == "0" }.orEmpty()) }
    } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeader(if(original == null) "食品を登録" else "食品を編集", "キャンセル", onBack) }
        item { PaperField(name, {name=it}, Modifier.fillMaxWidth(), label="名称") }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            PaperField(unit, {unit=it}, Modifier.weight(1f), label="量の単位")
            PaperField(note, {note=it}, Modifier.weight(1f), label="補足（目安量）")
        } }
        item { Text("栄養成分表示（1${unit.ifBlank { "単位" }}あたり）", color=Muted); HorizontalDivider(color=Rule) }
        items(nutrientSpecs.chunked(2)) { row -> Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            row.forEach { spec -> PaperField(values[spec.key].orEmpty(), { values[spec.key]=it.filterNumber() }, Modifier.weight(1f), label="${spec.label} (${spec.unit})") }
            if(row.size==1) Spacer(Modifier.weight(1f))
        } }
        item { PaperField(additives, {additives=it}, Modifier.fillMaxWidth(), label="添加物（「、」区切り）") }
        item { LineButton("保存", {
            val n = Nutrients(
                kcal=values.d("kcal"), protein=values.d("protein"), sugar=values.d("sugar"), fat=values.d("fat"),
                fiber=values.d("fiber"), salt=values.d("salt"), water=values.d("water"), vitC=values.d("vitC"),
                vitD=values.d("vitD"), vitB=values.d("vitB"), ca=values.d("ca"), fe=values.d("fe"), mg=values.d("mg"),
            )
            vm.saveFood(FoodEntity(
                id=original?.id ?: 0, name=name.trim(), unit=unit.trim(), unitNote=note.trim().ifBlank { null },
                lastAmount=original?.lastAmount ?: 1.0, additivesJson=additivesToJson(additives),
                perKcal=n.kcal, perProtein=n.protein, perSugar=n.sugar, perFat=n.fat, perFiber=n.fiber,
                perSalt=n.salt, perWater=n.water, perVitC=n.vitC, perVitD=n.vitD, perVitB=n.vitB,
                perCa=n.ca, perFe=n.fe, perMg=n.mg,
            )) { onBack() }
        }, Modifier.fillMaxWidth()) }
        if (original != null) item { LineButton("この食品と関連する記録を削除", {confirmDelete=true}, Modifier.fillMaxWidth()) }
    }
    if(confirmDelete && original != null) AlertDialog(
        onDismissRequest={confirmDelete=false}, title={Text("食品を削除しますか")},
        text={Text("「${original.name}」と、この食品に紐づくすべての食事記録が削除されます。")},
        confirmButton={TextButtonLike("削除", onClick={vm.deleteFood(original, onBack)})},
        dismissButton={TextButtonLike("キャンセル", onClick={confirmDelete=false})},
    )
}

@Composable
private fun GoalsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val values = remember(goals) { mutableStateMapOf<String,String>().apply {
        nutrientSpecs.forEach { spec -> put(spec.key, goals.firstOrNull {it.nutrientKey==spec.key}?.target?.let(::number).orEmpty()) }
    } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { PageHeader("目標設定", "完了", onBack); Text("1日あたりの目標値。空欄にすると目標なしになります。変更は即時保存されます。", color=Muted, modifier=Modifier.padding(vertical=12.dp)) }
        items(nutrientSpecs) { spec ->
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(spec.label, Modifier.weight(1f))
                PaperField(values[spec.key].orEmpty(), { value -> values[spec.key]=value.filterNumber(); vm.setGoal(spec.key, value.toDoubleOrNull()?.toString().orEmpty()) }, Modifier.width(120.dp), placeholder="—")
                Text("${spec.unit}/日", color=Muted, modifier=Modifier.width(62.dp))
            }
            HorizontalDivider(color=Rule)
        }
    }
}

@Composable
private fun HistoryScreen(vm: MainViewModel) {
    val entries by vm.history.collectAsStateWithLifecycle()
    val activities by vm.historyActivities.collectAsStateWithLifecycle()
    var nutrient by rememberSaveable { mutableStateOf("protein") }
    var days by rememberSaveable { mutableIntStateOf(7) }
    val end = LocalDate.now(); val dates = (days-1 downTo 0).map { end.minusDays(it.toLong()) }
    val values = dates.map { date -> Exporter.sum(entries.filter { it.timestamp.localDate()==date }).value(nutrient) }
    val spec = nutrientSpecs.first {it.key==nutrient}
    val goals by vm.goals.collectAsStateWithLifecycle()
    val goal = goals.firstOrNull { it.nutrientKey==nutrient }?.target
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { PageHeader("推移") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("protein","sugar","water","kcal","salt","fat").forEach { key ->
                val s=nutrientSpecs.first {it.key==key}; LineButton(s.label, {nutrient=key}, Modifier.weight(1f))
            }
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { LineButton("7日", {days=7}); LineButton("14日", {days=14}) } }
        item { BarChart(dates, values, goal); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("過去${days}日間・${spec.label} (${spec.unit})", color=Muted); Text("平均 ${number(values.average())} ${spec.unit}/日") }; goal?.let { Text("目標: ${number(it)} ${spec.unit}/日", color=Muted) } }
        item { SectionTitle("摂取 kcal と消費 kcal") }
        item {
            dates.forEach { date ->
                val intake=Exporter.sum(entries.filter {it.timestamp.localDate()==date}).kcal
                val burn=activities.firstOrNull {it.date==date.toString()}?.totalCaloriesKcal
                Row(Modifier.fillMaxWidth().padding(vertical=5.dp), horizontalArrangement=Arrangement.SpaceBetween) { Text("${date.monthValue}/${date.dayOfMonth}"); Text("摂取 ${number(intake)}　消費 ${burn?.let(::number) ?: "—"}") }
            }
        }
    }
}

@Composable
private fun BarChart(dates: List<LocalDate>, values: List<Double>, goal: Double?) {
    val maximum = maxOf(values.maxOrNull() ?: 1.0, goal ?: 0.0, 1.0)
    Column(Modifier.fillMaxWidth().height(240.dp).padding(top=10.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.Bottom) {
            values.forEachIndexed { index, value ->
                Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Bottom) {
                    Text(number(value), color=Muted)
                    Box(Modifier.fillMaxWidth(.75f).fillMaxHeight((value/maximum).toFloat().coerceIn(.02f,1f)).then(Modifier), contentAlignment=Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) { drawRect(if(index==values.lastIndex) Color(0xFFF2DFBB) else Color.Transparent); drawRect(Accent, style=Stroke(width=1.dp.toPx())) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) { dates.forEach { Text("${it.monthValue}/${it.dayOfMonth}", Modifier.weight(1f), textAlign=TextAlign.Center, color=Muted) } }
    }
}

@Composable
private fun ManualActivityDialog(existing: DailyActivityEntity?, onDismiss: () -> Unit, onSave: (DailyActivityEntity) -> Unit) {
    var total by remember { mutableStateOf(existing?.totalCaloriesKcal?.let(::number).orEmpty()) }
    var active by remember { mutableStateOf(existing?.activeCaloriesKcal?.let(::number).orEmpty()) }
    var steps by remember { mutableStateOf(existing?.steps?.toString().orEmpty()) }
    var exercise by remember { mutableStateOf(existing?.exerciseMinutes?.toString().orEmpty()) }
    var sleep by remember { mutableStateOf(existing?.sleepMinutes?.toString().orEmpty()) }
    var hr by remember { mutableStateOf(existing?.restingHr?.toString().orEmpty()) }
    AlertDialog(onDismissRequest=onDismiss, title={Text("活動データを手入力")}, text={
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("消費kcal" to total, "活動kcal" to active, "歩数" to steps, "運動分" to exercise, "睡眠分" to sleep, "安静時心拍" to hr).forEachIndexed { index, pair ->
                PaperField(pair.second, {v -> when(index){0->total=v;1->active=v;2->steps=v;3->exercise=v;4->sleep=v;else->hr=v}}, Modifier.fillMaxWidth(), label=pair.first)
            }
        }
    }, confirmButton={TextButtonLike("保存", onClick={ onSave(DailyActivityEntity(LocalDate.now().toString(), total.toDoubleOrNull(), active.toDoubleOrNull(), steps.toLongOrNull(), exercise.toIntOrNull(), sleep.toIntOrNull(), hr.toIntOrNull())) })}, dismissButton={TextButtonLike("キャンセル", onDismiss)})
}

private fun String.filterNumber() = filter { it.isDigit() || it=='.' || it=='-' }
private fun Map<String,String>.d(key: String) = this[key]?.toDoubleOrNull() ?: 0.0

@Composable
private fun HealthGuideScreen(vm: MainViewModel, onBack: () -> Unit, onPermission: () -> Unit) {
    val context = LocalContext.current
    val availability = remember { vm.health.availability() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        PageHeader("Garmin 連携設定", "完了", onBack)
        Text("データの流れ", color=Muted)
        Text("Venu 2 Plus → Garmin Connect → ヘルスコネクト → 私の栄養記録")
        Text("本アプリはGarminへ直接接続せず、端末内のヘルスコネクトを読み取ります。書き込み権限や外部通信は使用しません。")
        HorizontalDivider(color=Rule)
        GuideStep("1", "Androidの設定でヘルスコネクトを開く", "設定 → セキュリティとプライバシー → プライバシー → ヘルスコネクト")
        GuideStep("2", "Garmin Connectから共有する", "Garmin Connect → 設定 → 接続済みアプリ → Health Connect。歩数・カロリー・心拍・睡眠を許可します。")
        GuideStep("3", "本アプリへ読み取りを許可する", "下のボタンから、表示された7種類の読み取り権限を許可します。")
        Text("状態: ${when(availability){HealthAvailability.AVAILABLE->"利用可能";HealthAvailability.NEEDS_INSTALL->"更新が必要";HealthAvailability.NOT_SUPPORTED->"この端末では利用不可"}}", color=if(availability==HealthAvailability.AVAILABLE) Accent else Muted)
        LineButton("読み取り権限を設定", onPermission, Modifier.fillMaxWidth(), availability==HealthAvailability.AVAILABLE)
        LineButton("ヘルスコネクト設定を開く", {
            runCatching { context.startActivity(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")) }
        }, Modifier.fillMaxWidth())
        HorizontalDivider(color=Rule)
        Text("同期できない場合", style=androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text("Garmin Connect側の同期設定と最終同期時刻を確認してください。ヘルスコネクトを利用できない場合は、ホームの「手入力」または出力画面のGarmin CSV取込を使えます。", color=Muted)
    }
}

@Composable
private fun GuideStep(number: String, title: String, body: String) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(number, color=Accent, style=androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Column { Text(title, fontWeight=FontWeight.SemiBold); Text(body, color=Muted) }
    }
}

@Composable
private fun ExportScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var from by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(6)) }
    var to by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var markdown by rememberSaveable { mutableStateOf(true) }
    var target by rememberSaveable { mutableStateOf("すべて") }
    var preview by remember { mutableStateOf("") }
    var bytes by remember { mutableStateOf(ByteArray(0)) }
    var pending by remember { mutableStateOf<Pair<String,ByteArray>?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> out.write(pending?.second ?: bytes) } }
    }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { runCatching { context.contentResolver.openInputStream(it)!!.bufferedReader().use { r -> vm.restore(r.readText()) } } } }
    }
    val garminImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> vm.importGarminCsv(r.readText()) } }
    }
    LaunchedEffect(from, to, markdown, target) {
        if (!from.isAfter(to)) {
            val nutrition = vm.export(from,to,markdown)
            val workout = vm.exportWorkout(from,to,markdown)
            val result = when(target) {
                "栄養" -> nutrition
                "筋トレ" -> workout
                else -> {
                    if(markdown) {
                        val text = nutrition.first + "\n\n---\n\n" + workout.first
                        text to text.toByteArray()
                    } else workout
                }
            }
            preview=result.first; bytes=result.second
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { PageHeader("エクスポート"); Text("記録をファイルに出力し、チャットAIへ渡してフィードバックを得られます。", color=Muted, modifier=Modifier.padding(top=10.dp)) }
        item { SectionTitle("出力対象"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("栄養","筋トレ","すべて").forEach { item -> LineButton(item, {target=item; if(item=="すべて") markdown=true}, Modifier.weight(1f).height(44.dp)) }
        }; if(target=="すべて") Text("統合出力はMarkdown形式です。", color = Muted) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LineButton("開始日\n$from", { showDatePicker(context,from){from=it} }, Modifier.weight(1f))
            LineButton("終了日\n$to", { showDatePicker(context,to){to=it} }, Modifier.weight(1f))
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LineButton("Markdown (.md)", {markdown=true}, Modifier.weight(1f))
            LineButton("CSV (Excel用)", {if(target!="すべて") markdown=false}, Modifier.weight(1f), target!="すべて")
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("プレビュー", color=Muted); Text("${preview.lineSequence().count()}行", color=Muted) } }
        item { OutlinedCard(shape=RectangleShape, border=BorderStroke(1.dp,Rule), modifier=Modifier.fillMaxWidth().height(300.dp)) { Text(preview, Modifier.padding(12.dp).verticalScroll(rememberScrollState())) } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            LineButton("ファイル保存", { val name="健康記録_${target}_${from}_${to}.${if(markdown)"md" else "csv"}"; pending=name to bytes; saveLauncher.launch(name) }, Modifier.weight(1f))
            LineButton("共有", { shareBytes(context, "健康記録_${target}.${if(markdown)"md" else "csv"}", bytes, if(markdown)"text/markdown" else "text/csv") }, Modifier.weight(1f))
            LineButton("全文コピー", { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("栄養記録",preview)) }, Modifier.weight(1f))
        } }
        item { SectionTitle("バックアップと代替取込") }
        item { LineButton("全データをJSONで保存", {
            scope.launch { val data=vm.backup().toByteArray(); val name="eiyoapp-backup-${LocalDate.now()}.json"; pending=name to data; saveLauncher.launch(name) }
        }, Modifier.fillMaxWidth()) }
        item { LineButton("JSONバックアップを復元", {backupImport.launch(arrayOf("application/json","text/plain"))}, Modifier.fillMaxWidth()) }
        item { LineButton("Garmin CSVを取り込む", {garminImport.launch(arrayOf("text/csv","text/comma-separated-values","text/plain"))}, Modifier.fillMaxWidth()) }
        item { Text("対応列名: Date/日付、Total Calories/消費カロリー、Steps/歩数、Exercise Minutes/運動分、Sleep Duration/睡眠時間、Resting Heart Rate/安静時心拍。取込前にJSONバックアップを推奨します。", color=Muted) }
    }
}

private fun showDatePicker(context: Context, current: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(context, {_,y,m,d -> onPicked(LocalDate.of(y,m+1,d))}, current.year,current.monthValue-1,current.dayOfMonth).show()
}

private fun shareBytes(context: Context, name: String, bytes: ByteArray, mime: String) {
    val dir=File(context.cacheDir,"exports").apply {mkdirs()}; val file=File(dir,name); file.writeBytes(bytes)
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type=mime; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    },"栄養記録を共有"))
}
