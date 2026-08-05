package jp.aoto.eiyoapp.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.aoto.eiyoapp.MainViewModel
import jp.aoto.eiyoapp.data.BodyMetricEntity
import jp.aoto.eiyoapp.data.ExerciseLastSet
import jp.aoto.eiyoapp.data.WorkoutExerciseEntity
import jp.aoto.eiyoapp.data.WorkoutSetEntity
import jp.aoto.eiyoapp.domain.Exporter
import jp.aoto.eiyoapp.domain.WorkoutMath
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val WorkoutShape = RoundedCornerShape(6.dp)

@Composable
fun WorkoutHubScreen(vm: MainViewModel, onOpenSession: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("home") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallModeButton("ホーム", page == "home", { page = "home" }, Modifier.weight(1f))
            SmallModeButton("種目", page == "library", { page = "library" }, Modifier.weight(1f))
        }
        if (page == "home") WorkoutHomeScreen(vm, onOpenSession)
        else ExerciseLibraryScreen(vm, onOpenSession)
    }
}

@Composable
private fun WorkoutHomeScreen(vm: MainViewModel, onOpenSession: () -> Unit) {
    val exercises by vm.workoutExercises.collectAsState()
    val sessions by vm.workoutSessions.collectAsState()
    val active by vm.activeWorkout.collectAsState()
    val lastSets by vm.workoutLastSets.collectAsState()
    val settings by vm.workoutSettings.collectAsState()
    var addOpen by remember { mutableStateOf(false) }
    val weekGoal = settings.firstOrNull { it.key == "weekGoal" }?.value?.toIntOrNull() ?: 2
    val recovery = settings.firstOrNull { it.key == "recoveryAllowance" }?.value?.toIntOrNull() ?: 1
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDone = sessions.count { it.completed && !LocalDate.parse(it.date).isBefore(monday) }
    val streak = WorkoutMath.streakWeeks(sessions.filter { it.completed }.map { LocalDate.parse(it.date) }, weekGoal, recovery)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 E", Locale.JAPANESE)), color = Accent, fontSize = 11.sp)
            Text("今日も、過去の自分を少しだけ超える。", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp))
        }
        if (active != null) item {
            BrassCard {
                Text("実行中のワークアウト", color = Accent, fontSize = 11.sp)
                Text("中断したところから再開できます", modifier = Modifier.padding(vertical = 8.dp))
                LineButton("ワークアウトに戻る", onOpenSession, Modifier.fillMaxWidth())
            }
        }
        item {
            BrassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column { Text("今週の目標", color = Muted); Row(verticalAlignment = Alignment.Bottom) {
                        Text(weekDone.toString(), color = Accent, fontSize = 58.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                        Text(" / $weekGoal 回", color = Muted, modifier = Modifier.padding(bottom = 11.dp))
                    } }
                    Text("残り ${(weekGoal - weekDone).coerceAtLeast(0)}回", color = Muted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        val done = sessions.any { it.completed && LocalDate.parse(it.date) == monday.plusDays(index.toLong()) }
                        Box(
                            Modifier.weight(1f).height(34.dp).background(if (done) Accent else Color.Transparent, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(day.getDisplayName(TextStyle.NARROW, Locale.JAPANESE), color = if (done) Paper else Muted, fontSize = 10.sp) }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Rule)
                Text("$streak 週連続で達成中", color = Accent, fontSize = 20.sp)
                Text("1回休んでも連続は途切れません。未達の週はリカバリー週として数えます。", color = Muted, fontSize = 11.sp)
            }
        }
        item { WorkoutSectionTitle("次にやるべき種目") }
        if (exercises.isEmpty()) item {
            Text("まだ種目がありません。マシンの前で15秒登録から始めましょう。", color = Muted)
        }
        items(exercises.take(3), key = { it.id }) { exercise ->
            val last = lastSets.firstOrNull { it.exerciseId == exercise.id }
            ExerciseStartCard(exercise, last) {
                vm.startWorkout(exercise.id) { onOpenSession() }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineButton("＋ 新しい種目を登録", { addOpen = true }, Modifier.weight(1f).height(54.dp))
                LineButton("今日はこれだけ", {
                    exercises.firstOrNull()?.let { vm.startWorkout(it.id) { onOpenSession() } }
                    if (exercises.isEmpty()) addOpen = true
                }, Modifier.weight(1f).height(54.dp))
            }
        }
        item {
            val xp = settings.firstOrNull { it.key == "xp" }?.value?.toIntOrNull() ?: sessions.size * 10
            BrassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lv.${xp / 100 + 1}  継続の習慣")
                    Text("${xp % 100} / 100", color = Accent)
                }
                ProgressLine((xp % 100) / 100f)
                Text("過去の自分との比較だけを表示します。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
            }
        }
    }
    if (addOpen) NewExerciseSheet(vm, onDismiss = { addOpen = false }) { id ->
        addOpen = false
        vm.startWorkout(id) { onOpenSession() }
    }
}

@Composable
private fun ExerciseLibraryScreen(vm: MainViewModel, onOpenSession: () -> Unit) {
    val exercises by vm.workoutExercises.collectAsState()
    var filter by rememberSaveable { mutableStateOf("すべて") }
    var addOpen by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<WorkoutExerciseEntity?>(null) }
    var merge by remember { mutableStateOf<WorkoutExerciseEntity?>(null) }
    val filtered = exercises.filter { filter == "すべて" || (filter == "自重" && it.unit != "kg") || it.part == filter }

    Column(Modifier.fillMaxSize()) {
        Text("種目ライブラリ", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(listOf("すべて", "胸", "背中", "脚", "肩", "腕", "体幹", "自重")) { part ->
                FilterChip(selected = filter == part, onClick = { filter = part }, label = { Text(part) })
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { exercise ->
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Rule), shape = WorkoutShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(exercise.name, fontSize = 17.sp)
                                    if (exercise.provisional) Text("仮名", color = Accent, fontSize = 10.sp)
                                }
                                Text("${exercise.part} · ${if (exercise.unit == "kg") "${Exporter.fmt(exercise.stepKg)}kg刻み" else if (exercise.unit == "sec") "秒数" else "自重"}", color = Muted, fontSize = 12.sp)
                            }
                            TextButton(onClick = { vm.startWorkout(exercise.id) { onOpenSession() } }) { Text("開始", color = Accent) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallModeButton("リネーム", false, { rename = exercise }, Modifier.weight(1f))
                            SmallModeButton("統合", false, { merge = exercise }, Modifier.weight(1f), enabled = exercises.size > 1)
                        }
                    }
                }
            }
            item { LineButton("＋ マシンの前で15秒登録", { addOpen = true }, Modifier.fillMaxWidth().height(58.dp)) }
        }
    }
    if (addOpen) NewExerciseSheet(vm, { addOpen = false }) { id ->
        addOpen = false; vm.startWorkout(id) { onOpenSession() }
    }
    rename?.let { exercise -> RenameDialog(exercise, { rename = null }) { vm.renameWorkoutExercise(exercise.id, it); rename = null } }
    merge?.let { source -> MergeDialog(source, exercises.filter { it.id != source.id }, { merge = null }) { target -> vm.mergeWorkoutExercises(source.id, target); merge = null } }
}

@Composable
fun WorkoutSessionScreen(vm: MainViewModel, onBack: () -> Unit) {
    val session by vm.activeWorkout.collectAsState()
    val exercises by vm.workoutExercises.collectAsState()
    val sets by vm.activeWorkoutSets.collectAsState()
    val lastSets by vm.workoutLastSets.collectAsState()
    val settings by vm.workoutSettings.collectAsState()
    val lastRecorded by vm.lastRecordedSet.collectAsState()
    val current = exercises.firstOrNull { it.id == session?.activeExerciseId }
    if (session == null || current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ワークアウトを読み込み中…") }
        return
    }
    val currentSets = sets.filter { it.exerciseId == current.id }
    val previous = lastSets.firstOrNull { it.exerciseId == current.id && currentSets.none { s -> s.recordedAt == it.recordedAt } }
    var weight by rememberSaveable(current.id) { mutableStateOf("0") }
    var reps by rememberSaveable(current.id) { mutableStateOf("10") }
    var seconds by rememberSaveable(current.id) { mutableStateOf("30") }
    var rest by rememberSaveable { mutableIntStateOf(0) }
    var restTotal by rememberSaveable { mutableIntStateOf(settings.firstOrNull { it.key == "restSeconds" }?.value?.toIntOrNull() ?: 90) }
    var condition by rememberSaveable { mutableStateOf("") }
    var summary by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(current.id, previous?.recordedAt, currentSets.size) {
        if (currentSets.isEmpty() && previous != null) {
            previous.weightKg?.let { weight = Exporter.fmt(it) }
            previous.reps?.let { reps = it.toString() }
            previous.seconds?.let { seconds = it.toString() }
        }
    }
    LaunchedEffect(rest) {
        if (rest > 0) { delay(1_000); rest-- }
        else if (rest == 0 && currentSets.isNotEmpty()) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vibrator?.vibrate(250)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.size(44.dp)) { Text("←", fontSize = 24.sp) }
                Column(Modifier.weight(1f)) {
                    Text(current.name, fontSize = 22.sp)
                    Text("${current.part}・${if (current.unit == "kg") "マシン" else "自重"}", color = Muted)
                }
            }
        }
        item {
            BrassCard {
                Text("前回", color = Muted, fontSize = 10.sp)
                Text(previousText(previous, current.unit), fontSize = 18.sp)
            }
        }
        if (lastRecorded?.set?.isPr == true) item {
            OutlinedCard(border = BorderStroke(1.dp, Accent), colors = CardDefaults.outlinedCardColors(containerColor = Accent.copy(alpha = .12f)), shape = WorkoutShape) {
                Column(Modifier.padding(16.dp)) {
                    Text("↗  前回超え", color = Accent, fontSize = 28.sp)
                    Text("過去の自分を超えました。ちゃんと強くなっています。", color = Muted)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (current.unit == "kg") StepperPanel("重量 KG", weight, "刻み ${Exporter.fmt(current.stepKg)}kg", {
                    weight = ((weight.toDoubleOrNull() ?: 0.0) - current.stepKg).coerceAtLeast(0.0).let(Exporter::fmt)
                }, { weight = ((weight.toDoubleOrNull() ?: 0.0) + current.stepKg).let(Exporter::fmt) }, Modifier.weight(1f))
                StepperPanel(if (current.unit == "sec") "秒数" else "回数", if (current.unit == "sec") seconds else reps, "前回 ${previousValue(previous, current.unit)}", {
                    if (current.unit == "sec") seconds = ((seconds.toIntOrNull() ?: 0) - 5).coerceAtLeast(0).toString()
                    else reps = ((reps.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString()
                }, {
                    if (current.unit == "sec") seconds = ((seconds.toIntOrNull() ?: 0) + 5).toString()
                    else reps = ((reps.toIntOrNull() ?: 0) + 1).toString()
                }, Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    vm.recordWorkoutSet(current, weight.toDoubleOrNull(), reps.toIntOrNull(), seconds.toIntOrNull())
                    restTotal = settings.firstOrNull { it.key == "restSeconds" }?.value?.toIntOrNull() ?: 90
                    rest = restTotal
                },
                border = BorderStroke(1.dp, Accent), shape = WorkoutShape,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Accent.copy(alpha = .12f), contentColor = Accent),
                modifier = Modifier.fillMaxWidth().height(76.dp),
            ) { Text("✓  セット${currentSets.size + 1}を記録", fontSize = 20.sp) }
        }
        if (rest > 0) item {
            BrassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("インターバル", color = Muted)
                    Text("${rest / 60}:${(rest % 60).toString().padStart(2, '0')}", color = Accent, fontSize = 34.sp)
                }
                ProgressLine(if (restTotal == 0) 0f else rest / restTotal.toFloat())
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LineButton("＋30秒", { rest += 30; restTotal += 30 }, Modifier.weight(1f).height(46.dp))
                    LineButton("スキップ", { rest = 0 }, Modifier.weight(1f).height(46.dp))
                }
            }
        }
        if (lastRecorded != null) item {
            BrassCard {
                Text("追い込み度（任意・1タップ）", color = Muted, fontSize = 11.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7 to "まだ余裕", 8 to "ちょうど", 9 to "限界").forEach { (rpe, label) ->
                        OutlinedButton(onClick = { vm.setWorkoutRpe(lastRecorded!!.set, rpe) }, modifier = Modifier.weight(1f).height(54.dp), shape = WorkoutShape) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontSize = 11.sp); Text("RPE $rpe", color = Accent, fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
        item { SetTable(current, currentSets, previous) }
        if (exercises.size > 1) item {
            Text("種目を追加", color = Muted)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(exercises.filter { it.id != current.id }) { exercise ->
                    FilterChip(selected = false, onClick = { vm.switchWorkoutExercise(exercise.id) }, label = { Text(exercise.name) })
                }
            }
        }
        item {
            OutlinedTextField(condition, { condition = it }, label = { Text("体調メモ") }, placeholder = { Text("肩が少し重い / よく眠れた など") }, modifier = Modifier.fillMaxWidth())
            LineButton("終了して保存", { vm.completeWorkout(condition) { summary = true } }, Modifier.fillMaxWidth().height(58.dp))
            Text("1種目3セットでも、記録は残ります。", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
    if (summary) AlertDialog(
        onDismissRequest = {},
        title = { Text("今日の記録を保存しました") },
        text = {
            val volume = sets.sumOf { WorkoutMath.volume(it.weightKg, it.reps) }
            Column { Text("総ボリューム ${Exporter.fmt(volume)} kg", color = Accent); Text("前回超え ${sets.count { it.isPr }} セット"); Text("過去の自分との比較を積み重ねました。", color = Muted) }
        },
        confirmButton = { TextButton(onClick = onBack) { Text("ホームへ") } },
    )
}

@Composable
fun WorkoutProgressScreen(vm: MainViewModel) {
    val sets by vm.workoutHistorySets.collectAsState()
    val metrics by vm.bodyMetrics.collectAsState()
    var bodyOpen by remember { mutableStateOf(false) }
    val byPart = sets.groupingBy { it.part }.eachCount()
    val latestExercise = sets.lastOrNull { it.unit == "kg" }?.exerciseId
    val oneRm = sets.filter { it.exerciseId == latestExercise }.mapNotNull { WorkoutMath.estimatedOneRepMax(it.weightKg, it.reps) }.maxOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("進捗", style = MaterialTheme.typography.headlineLarge); Text("比べる相手は、過去の自分だけ。", color = Muted) }
        item { BrassCard { Text("推定1RM", color = Muted); Text(oneRm?.let { "${Exporter.fmt(it)} kg" } ?: "記録待ち", color = Accent, fontSize = 34.sp); Text("Epley式・種目ごとの最大値", color = Muted, fontSize = 11.sp) } }
        item { BrassCard { Text("総ボリューム", color = Muted); Text("${Exporter.fmt(sets.sumOf { WorkoutMath.volume(it.weightKg, it.reps) })} kg", color = Accent, fontSize = 32.sp) } }
        item { WorkoutSectionTitle("部位別セット数") }
        items(listOf("胸", "背中", "脚", "肩", "腕", "体幹")) { part ->
            val count = byPart[part] ?: 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(part, Modifier.width(48.dp), color = Muted)
                Box(Modifier.weight(1f).height(12.dp).background(Rule, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth((count / 16f).coerceIn(0f, 1f)).height(12.dp).background(if (count in 6..12) Accent else Muted, RoundedCornerShape(2.dp)))
                }
                Text(count.toString(), Modifier.width(36.dp), textAlign = TextAlign.End, color = if (count in 6..12) Accent else Muted)
            }
        }
        item { BrassCard { Text("からだの記録", color = Muted); val latest = metrics.lastOrNull(); Text(latest?.weightKg?.let { "体重 ${Exporter.fmt(it)}kg" } ?: "まだ記録がありません"); LineButton("今日の体重・周径を記録", { bodyOpen = true }, Modifier.fillMaxWidth().padding(top = 10.dp)) } }
    }
    if (bodyOpen) BodyMetricDialog(vm) { bodyOpen = false }
}

@Composable
fun WorkoutSettingsScreen(vm: MainViewModel, onHealthGuide: () -> Unit, onGoals: () -> Unit) {
    val settings by vm.workoutSettings.collectAsState()
    val home by vm.home.collectAsState()
    fun setting(key: String, fallback: String) = settings.firstOrNull { it.key == key }?.value ?: fallback
    var goal by remember(settings) { mutableStateOf(setting("weekGoal", "2")) }
    var rest by remember(settings) { mutableStateOf(setting("restSeconds", "90")) }
    var step by remember(settings) { mutableStateOf(setting("defaultStepKg", "2.5")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("設定・記録", style = MaterialTheme.typography.headlineLarge) }
        item { BrassCard { Text("今日のコンディション", color = Muted); Text("睡眠  ${home.activity?.sleepMinutes?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "—"}", fontSize = 20.sp); Text("タンパク質  ${Exporter.fmt(home.total.protein)}g", fontSize = 20.sp); Text("睡眠はHealth Connect、タンパク質は食事記録から自動集計します。", color = Muted, fontSize = 11.sp) } }
        item { SettingNumber("週の目標回数", goal, "回") { goal = it; vm.updateWorkoutSetting("weekGoal", it) } }
        item { SettingNumber("インターバル初期値", rest, "秒") { rest = it; vm.updateWorkoutSetting("restSeconds", it) } }
        item { SettingNumber("重量の刻み（既定）", step, "kg") { step = it; vm.updateWorkoutSetting("defaultStepKg", it) } }
        item { LineButton("栄養目標を編集", onGoals, Modifier.fillMaxWidth().height(52.dp)) }
        item { LineButton("Health Connect設定", onHealthGuide, Modifier.fillMaxWidth().height(52.dp)); Text("他人との比較・ランキングは行いません。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewExerciseSheet(vm: MainViewModel, onDismiss: () -> Unit, onSaved: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var part by remember { mutableStateOf("胸") }
    var unit by remember { mutableStateOf("kg") }
    var step by remember { mutableStateOf(2.5) }
    var note by remember { mutableStateOf("") }
    var elapsed by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); elapsed++ } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1815)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("新しい種目", fontSize = 28.sp); Text("${elapsed}秒経過", color = Accent) }
            Text("名前が分からなくてOK。あとで直せます。", color = Muted)
            OutlinedTextField(name, { name = it }, placeholder = { Text("種目名") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("胸のマシンA", "背中のマシンB", "脚のマシンC").forEach { label -> FilterChip(false, { name = label; part = label.takeWhile { it != 'の' } }, { Text(label, fontSize = 10.sp) }) } }
            Text("部位タグ", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("胸", "背中", "脚", "肩", "腕", "体幹").forEach { label -> FilterChip(part == label, { part = label }, { Text(label) }) } }
            Text("記録タイプ / 重量の刻み", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(1.0, 2.5, 5.0).forEach { value -> FilterChip(unit == "kg" && step == value, { unit = "kg"; step = value }, { Text("${Exporter.fmt(value)}kg") }) }
                FilterChip(unit == "bw", { unit = "bw" }, { Text("自重") })
                FilterChip(unit == "sec", { unit = "sec" }, { Text("秒数") })
            }
            OutlinedTextField(note, { note = it }, placeholder = { Text("メモ（シート位置、ピン位置など）") }, modifier = Modifier.fillMaxWidth().height(90.dp))
            LineButton("登録して、すぐ開始", { vm.addWorkoutExercise(name, part, unit, step, note, onSaved) }, Modifier.fillMaxWidth().height(64.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("やめる") }
        }
    }
}

@Composable
private fun RenameDialog(exercise: WorkoutExerciseEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(exercise.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("名前を変更") }, text = { Column { Text("過去の記録もすべて新しい名前に引き継がれます。", color = Muted); OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(onClick = { onSave(value) }) { Text("変更する") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } })
}

@Composable
private fun MergeDialog(source: WorkoutExerciseEntity, targets: List<WorkoutExerciseEntity>, onDismiss: () -> Unit, onMerge: (Long) -> Unit) {
    var target by remember { mutableStateOf<Long?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("種目を統合") }, text = { Column { Text("${source.name}を、下のどれかに合わせます。", color = Muted); targets.forEach { item -> FilterChip(target == item.id, { target = item.id }, { Text("${item.name}・${item.part}") }, modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { TextButton(onClick = { target?.let(onMerge) }, enabled = target != null) { Text("統合する") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } })
}

@Composable
private fun BodyMetricDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }; var fat by remember { mutableStateOf("") }; var arm by remember { mutableStateOf("") }; var chest by remember { mutableStateOf("") }; var waist by remember { mutableStateOf("") }; var thigh by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("からだの記録") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf("体重kg" to weight, "体脂肪率%" to fat, "腕cm" to arm, "胸cm" to chest, "腹囲cm" to waist, "太ももcm" to thigh).forEachIndexed { index, pair -> OutlinedTextField(pair.second, { v -> when(index){0->weight=v;1->fat=v;2->arm=v;3->chest=v;4->waist=v;else->thigh=v} }, label = { Text(pair.first) }, modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { TextButton(onClick = { vm.saveBodyMetric(BodyMetricEntity(LocalDate.now().toString(), weight.toDoubleOrNull(), fat.toDoubleOrNull(), arm.toDoubleOrNull(), chest.toDoubleOrNull(), waist.toDoubleOrNull(), thigh.toDoubleOrNull())); onDismiss() }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } })
}

@Composable
private fun SetTable(exercise: WorkoutExerciseEntity, sets: List<WorkoutSetEntity>, previous: ExerciseLastSet?) {
    Column {
        Row(Modifier.fillMaxWidth().padding(8.dp)) { Text("SET", Modifier.width(44.dp), color = Muted); Text("前回", Modifier.weight(1f), color = Muted); Text("今回", Modifier.weight(1f), color = Muted); Text("RPE", Modifier.width(45.dp), color = Muted) }
        sets.forEach { set ->
            Row(Modifier.fillMaxWidth().background(if (set.isPr) Accent.copy(alpha = .08f) else Color.Transparent).padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(set.setNo.toString(), Modifier.width(44.dp), color = if (set.isPr) Accent else Ink)
                Text(previousText(previous, exercise.unit), Modifier.weight(1f), color = Muted, fontSize = 12.sp)
                Text(setText(set, exercise.unit), Modifier.weight(1f), color = if (set.isPr) Accent else Ink)
                Text(set.rpe?.toString() ?: "—", Modifier.width(45.dp), textAlign = TextAlign.End, color = Muted)
            }
            HorizontalDivider(color = Rule)
        }
    }
}

@Composable
private fun StepperPanel(label: String, value: String, help: String, minus: () -> Unit, plus: () -> Unit, modifier: Modifier) {
    OutlinedCard(modifier, border = BorderStroke(1.dp, Rule), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent), shape = WorkoutShape) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text(value, fontSize = 52.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
            Text(help, color = Muted, fontSize = 10.sp)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineButton("−", minus, Modifier.weight(1f).height(56.dp))
                LineButton("＋", plus, Modifier.weight(1f).height(56.dp))
            }
        }
    }
}

@Composable
private fun SettingNumber(title: String, value: String, suffix: String, onSave: (String) -> Unit) {
    BrassCard { Text(title); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("$value$suffix", color = Accent, fontSize = 24.sp); Row { TextButton(onClick = { onSave(((value.toDoubleOrNull() ?: 1.0) - if (suffix == "kg") .5 else 1.0).coerceAtLeast(1.0).let(Exporter::fmt)) }) { Text("−") }; TextButton(onClick = { onSave(((value.toDoubleOrNull() ?: 1.0) + if (suffix == "kg") .5 else 1.0).let(Exporter::fmt)) }) { Text("＋") } } } }
}

@Composable
private fun ExerciseStartCard(exercise: WorkoutExerciseEntity, last: ExerciseLastSet?, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), border = BorderStroke(1.dp, Rule), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent), shape = WorkoutShape) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(exercise.name, fontSize = 17.sp); Text(exercise.part, color = Accent, fontSize = 11.sp) }; Text("前回 ${previousText(last, exercise.unit)}", color = Muted) }
            Box(Modifier.size(48.dp).background(Color.Transparent, CircleShape), contentAlignment = Alignment.Center) { Text("▶", color = Accent, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun BrassCard(content: @Composable ColumnScope.() -> Unit) = OutlinedCard(border = BorderStroke(1.dp, Rule), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent), shape = WorkoutShape, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }

@Composable
private fun WorkoutSectionTitle(text: String) = Text(text, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)

@Composable
private fun SmallModeButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) = OutlinedButton(onClick = onClick, modifier = modifier.height(44.dp), enabled = enabled, shape = WorkoutShape, border = BorderStroke(1.dp, if (selected) Accent else Rule), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Accent.copy(alpha = .12f) else Color.Transparent, contentColor = if (selected) Accent else Ink)) { Text(text) }

private fun previousText(previous: ExerciseLastSet?, unit: String) = when (unit) { "kg" -> if (previous?.weightKg == null) "はじめての記録です" else "${Exporter.fmt(previous.weightKg)}kg × ${previous.reps ?: "—"}"; "sec" -> previous?.seconds?.let { "${it}秒" } ?: "はじめての記録です"; else -> previous?.reps?.let { "自重 × $it" } ?: "はじめての記録です" }
private fun previousValue(previous: ExerciseLastSet?, unit: String) = if (unit == "sec") previous?.seconds?.toString() ?: "—" else previous?.reps?.toString() ?: "—"
private fun setText(set: WorkoutSetEntity, unit: String) = when (unit) { "kg" -> "${set.weightKg?.let(Exporter::fmt) ?: "—"}kg × ${set.reps ?: "—"}"; "sec" -> "${set.seconds ?: "—"}秒"; else -> "自重 × ${set.reps ?: "—"}" }
