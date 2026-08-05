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

private enum class Tab(val label: String) { TODAY("ä»Šæ—¥"), FOOD("é£Ÿäº‹"), WORKOUT("ç­‹ãƒˆãƒ¬"), ANALYSIS("åˆ†æ"), MORE("ãã®ä»–") }
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
            Text("ç§ã®ãŸã‚ã®æ „é¤Šè¨˜éŒ²", color=Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.Bottom) {
                Text("${today.monthValue}æœˆ${today.dayOfMonth}æ—¥", style=androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.JAPANESE), color=Muted)
            }
        }
        item { GoalProgress(state.goals, state.total, onGoals) }
        item { ActivityCard(state.total.kcal, state.activity, syncing, onSync={ vm.syncHealth(force=true) }, onEdit={ manual=true }, onGuide=onHealthGuide) }
        item {
            OutlinedCard(shape=RectangleShape, border=BorderStroke(1.dp, Accent), modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("ä»Šé€±ã®ç­‹ãƒˆãƒ¬", color=Accent)
                        val monday=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        Text("${sessions.count { it.completed && !LocalDate.parse(it.date).isBefore(monday) }} / 2å›", color=Accent)
                    }
                    Text(activeWorkout?.let { "å®Ÿè¡Œä¸­ã®ãƒ¯ãƒ¼ã‚¯ã‚¢ã‚¦ãƒˆãŒã‚ã‚Šã¾ã™" } ?: exercises.firstOrNull()?.let { "æ¬¡ã¯ ${it.name}" } ?: "æœ€åˆã®ç¨®ç›®ã‚’ç™»éŒ²ã—ã¾ã—ã‚‡ã†", color=Muted)
                    LineButton(if(activeWorkout!=null) "ãƒ¯ãƒ¼ã‚¯ã‚¢ã‚¦ãƒˆã«æˆ»ã‚‹" else "ç­‹ãƒˆãƒ¬ã‚’å§‹ã‚ã‚‹", {
                        if(activeWorkout!=null) onWorkout() else exercises.firstOrNull()?.let { vm.startWorkout(it.id,onWorkout) }
                    }, Modifier.fillMaxWidth(), activeWorkout!=null || exercises.isNotEmpty())
                }
            }
        }
        item {
            SectionTitle("ä»Šæ—¥ã®æ „é¤Šç´ ")
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
            SectionTitle("ä»Šæ—¥æ‘‚å–ã—ãŸæ·»åŠ ç‰©")
            val additives = state.entries.flatMap { additivesFromJson(it.additivesJson) }.groupingBy { it }.eachCount()
            if (additives.isEmpty()) Text("ãªã—", color=Muted) else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                additives.forEach { (name, count) -> AssistChip(onClick={}, label={ Text("$name Ã—$count") }) }
            }
        }
        item { SectionTitle("æœ¬æ—¥ã®è¨˜éŒ²") }
        if (state.entries.isEmpty()) item { Text("ã¾ã è¨˜éŒ²ãŒã‚ã‚Šã¾ã›ã‚“", color=Muted) }
        items(state.entries, key={ it.entryId }) { entry -> EntryRow(entry) { vm.deleteEntry(entry.entryId) } }
    }
    if (manual) ManualActivityDialog(state.activity, onDismiss={manual=false}, onSave={ vm.saveManualActivity(it); manual=false })
}

@Composable
private fun GoalProgress(goals: List<GoalEntity>, total: Nutrients, onEdit: () -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text("ç›®æ¨™ã®é”æˆåº¦", color=Muted); TextButtonLike("ç›®æ¨™ã‚’ç·¨é›†", onEdit)
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
                Text("æ‘‚å–ã¨æ¶ˆè²»"); Text("Garmin é€£æº", color=Muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                LabeledValue("æ‘‚å– kcal", number(intake), Modifier.weight(1f))
                LabeledValue("æ¶ˆè²» kcal", activity?.totalCaloriesKcal?.let(::number) ?: "â€”", Modifier.weight(1f))
                LabeledValue("åæ”¯", activity?.totalCaloriesKcal?.let { "%+d".format((intake-it).toInt()) } ?: "â€”", Modifier.weight(1f))
            }
            HorizontalDivider(color=Rule)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                LabeledValue("æ­©æ•°", activity?.steps?.toString() ?: "â€”", Modifier.weight(1f))
                LabeledValue("é‹å‹•", activity?.exerciseMinutes?.let { "${it}åˆ†" } ?: "â€”", Modifier.weight(1f))
                LabeledValue("ç¡çœ ", activity?.sleepMinutes?.let { "%d:%02d".format(it/60,it%60) } ?: "â€”", Modifier.weight(1f))
                LabeledValue("å®‰é™æ™‚å¿ƒæ‹", activity?.restingHr?.toString() ?: "â€”", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Text(activity?.syncedAt?.let { "æœ€çµ‚åŒæœŸ ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}" } ?: "æœªåŒæœŸ", color=Muted)
                Row { TextButtonLike("æ‰‹å…¥åŠ›", onEdit); TextButtonLike("è¨­å®š", onGuide); TextButtonLike(if(syncing) "åŒæœŸä¸­" else "åŒæœŸ", onSync, !syncing) }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: EntryWithFood, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.width(58.dp)) { Text(entry.timestamp.localTime().format(DateTimeFormatter.ofPattern("HH:mm"))); Text(Exporter.timeBand(entry.timestamp.localTime().hour), color=Muted) }
        Column(Modifier.weight(1f)) { Text(entry.name); Text("Ã—${number(entry.amount)}${entry.unit}  ${number(entry.nutrients().kcal)} kcal", color=Muted) }
        IconButton(onClick=onDelete) { Icon(Icons.OutlinßÍµ¶‰ËkºwµçAÑä ¤¤ô(€€€ôô(€€€1…éå½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤°½¹Ñ•¹ÑA…‘‘¥¹œõA…‘‘¥¹Y…±Õ•Ì ÈÀ¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄÀ¹‘À¤¤ì(€€€€€€€¥Ñ•´ìA…•!•…‘•È ‹n»š¢g¢¢·–ºhˆ°€‹–º3’êˆ°½¹	…¬¤ìQ•áĞ ˆÇš^—
+»n»š¢g–“¦ëš²¯g
/£n»š¢g«_¯«
+ûg–’'šnÓ¿–6Ïšf’şw–¶cW
3ûgˆ°½±½Èõ5ÕÑ•°µ½‘¥™¥•Èõ5½‘¥™¥•È¹Á…‘‘¥¹œ¡Ù•ÉÑ¥…°ôÄÈ¹‘À¤¤ô(€€€€€€€¥Ñ•µÌ¡¹ÕÑÉ¥•¹ÑMÁ•Ì¤ìÍÁ•Œ€´ø(€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°Ù•ÉÑ¥…±±¥¹µ•¹Ğõ±¥¹µ•¹Ğ¹•¹Ñ•ÉY•ÉÑ¥…±±ä°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄÈ¹‘À¤¤ì(€€€€€€€€€€€€€€€Q•áĞ¡ÍÁ•Œ¹±…‰•°°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€€€€€A…Á•É¥•±¡Ù…±Õ•ÍmÍÁ•Œ¹­•åt¹½ÉµÁÑä ¤°ìÙ…±Õ”€´øÙ…±Õ•ÍmÍÁ•Œ¹­•åtõÙ…±Õ”¹™¥±Ñ•É9Õµ‰•È ¤ìÙ´¹Í•Ñ½…°¡ÍÁ•Œ¹­•ä°Ù…±Õ”¹Ñ½½Õ‰±•=É9Õ±° ¤ü¹Ñ½MÑÉ¥¹œ ¤¹½ÉµÁÑä ¤¤ô°5½‘¥™¥•È¹İ¥‘Ñ  ÄÈÀ¹‘À¤°Á±…•¡½±‘•Èô‹ŠPˆ¤(€€€€€€€€€€€€€€€Q•áĞ ˆ‘íÍÁ•Œ¹Õ¹¥Ñô¿š^”ˆ°½±½Èõ5ÕÑ•°µ½‘¥™¥•Èõ5½‘¥™¥•È¹İ¥‘Ñ  ØÈ¹‘À¤¤(€€€€€€€€€€€ô(€€€€€€€€€€€!½É¥é½¹Ñ…±¥Ù¥‘•È¡½±½ÈõIÕ±”¤(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸!¥ÍÑ½ÉåMÉ••¸¡Ù´è5…¥¹Y¥•İ5½‘•°¤ì(€€€Ù…°•¹ÑÉ¥•Ì‰äÙ´¹¡¥ÍÑ½Éä¹½±±•ÑÍMÑ…Ñ•]¥Ñ¡1¥™•å±” ¤(€€€Ù…°…Ñ¥Ù¥Ñ¥•Ì‰äÙ´¹¡¥ÍÑ½ÉåÑ¥Ù¥Ñ¥•Ì¹½±±•ÑÍMÑ…Ñ•]¥Ñ¡1¥™•å±” ¤(€€€Ù…È¹ÕÑÉ¥•¹Ğ‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•MÑ…Ñ•=˜ ‰ÁÉ½Ñ•¥¸ˆ¤ô(€€€Ù…È‘…åÌ‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•%¹ÑMÑ…Ñ•=˜ Ü¤ô(€€€Ù…°•¹€ô1½…±…Ñ”¹¹½Ü ¤ìÙ…°‘…Ñ•Ì€ô€¡‘…åÌ´Ä‘½İ¹Q¼€À¤¹µ…Àì•¹¹µ¥¹ÕÍ…åÌ¡¥Ğ¹Ñ½1½¹œ ¤¤ô(€€€Ù…°Ù…±Õ•Ì€ô‘…Ñ•Ì¹µ…Àì‘…Ñ”€´øáÁ½ÉÑ•È¹ÍÕ´¡•¹ÑÉ¥•Ì¹™¥±Ñ•Èì¥Ğ¹Ñ¥µ•ÍÑ…µÀ¹±½…±…Ñ” ¤ôõ‘…Ñ”ô¤¹Ù…±Õ”¡¹ÕÑÉ¥•¹Ğ¤ô(€€€Ù…°ÍÁ•Œ€ô¹ÕÑÉ¥•¹ÑMÁ•Ì¹™¥ÉÍĞí¥Ğ¹­•äôõ¹ÕÑÉ¥•¹Ñô(€€€Ù…°½…±Ì‰äÙ´¹½…±Ì¹½±±•ÑÍMÑ…Ñ•]¥Ñ¡1¥™•å±” ¤(€€€Ù…°½…°€ô½…±Ì¹™¥ÉÍÑ=É9Õ±°ì¥Ğ¹¹ÕÑÉ¥•¹Ñ-•äôõ¹ÕÑÉ¥•¹Ğôü¹Ñ…É•Ğ(€€€1…éå½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤°½¹Ñ•¹ÑA…‘‘¥¹œõA…‘‘¥¹Y…±Õ•Ì ÈÀ¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄØ¹‘À¤¤ì(€€€€€€€¥Ñ•´ìA…•!•…‘•È ‹š:£ìˆ¤ô(€€€€€€€¥Ñ•´ìI½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä Ø¹‘À¤¤ì(€€€€€€€€€€€±¥ÍÑ=˜ ‰ÁÉ½Ñ•¥¸ˆ°‰ÍÕ…Èˆ°‰İ…Ñ•Èˆ°‰­…°ˆ°‰Í…±Ğˆ°‰™…Ğˆ¤¹™½É… ì­•ä€´ø(€€€€€€€€€€€€€€€Ù…°Ìõ¹ÕÑÉ¥•¹ÑMÁ•Ì¹™¥ÉÍĞí¥Ğ¹­•äôõ­•åôì1¥¹•	ÕÑÑ½¸¡Ì¹±…‰•°°í¹ÕÑÉ¥•¹Ğõ­•åô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€ô(€€€€€€€ôô(€€€€€€€¥Ñ•´ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä à¹‘À¤¤ì1¥¹•	ÕÑÑ½¸ ˆßš^”ˆ°í‘…åÌôİô¤ì1¥¹•	ÕÑÑ½¸ ˆÄÓš^”ˆ°í‘…åÌôÄÑô¤ôô(€€€€€€€¥Ñ•´ì	…É¡…ÉĞ¡‘…Ñ•Ì°Ù…±Õ•Ì°½…°¤ìI½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹MÁ…•	•Ñİ••¸¤ìQ•áĞ ‹¦;–:ì‘í‘…åÍ÷š^—¦ZOì‘íÍÁ•Œ¹±…‰•±ô€ ‘íÍÁ•Œ¹Õ¹¥Ñô¤ˆ°½±½Èõ5ÕÑ•¤ìQ•áĞ ‹–æÏ–v€‘í¹Õµ‰•È¡Ù…±Õ•Ì¹…Ù•É…” ¤¥ô€‘íÍÁ•Œ¹Õ¹¥Ñô¿š^”ˆ¤ôì½…°ü¹±•ĞìQ•áĞ ‹n»š¢dè€‘í¹Õµ‰•È¡¥Ğ¥ô€‘íÍÁ•Œ¹Õ¹¥Ñô¿š^”ˆ°½±½Èõ5ÕÑ•¤ôô(€€€€€€€¥Ñ•´ìM•Ñ¥½¹Q¥Ñ±” ‹šF–>X­…°ƒ£šÚ#¢Êì­…°ˆ¤ô(€€€€€€€¥Ñ•´ì(€€€€€€€€€€€‘…Ñ•Ì¹™½É… ì‘…Ñ”€´ø(€€€€€€€€€€€€€€€Ù…°¥¹Ñ…­”õáÁ½ÉÑ•È¹ÍÕ´¡•¹ÑÉ¥•Ì¹™¥±Ñ•Èí¥Ğ¹Ñ¥µ•ÍÑ…µÀ¹±½…±…Ñ” ¤ôõ‘…Ñ•ô¤¹­…°(€€€€€€€€€€€€€€€Ù…°‰ÕÉ¸õ…Ñ¥Ù¥Ñ¥•Ì¹™¥ÉÍÑ=É9Õ±°í¥Ğ¹‘…Ñ”ôõ‘…Ñ”¹Ñ½MÑÉ¥¹œ ¥ôü¹Ñ½Ñ…±…±½É¥•Í-…°(€€€€€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Á…‘‘¥¹œ¡Ù•ÉÑ¥…°ôÔ¹‘À¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹MÁ…•	•Ñİ••¸¤ìQ•áĞ ˆ‘í‘…Ñ”¹µ½¹Ñ¡Y…±Õ•ô¼‘í‘…Ñ”¹‘…å=™5½¹Ñ¡ôˆ¤ìQ•áĞ ‹šF–>X€‘í¹Õµ‰•È¡¥¹Ñ…­”¥÷šÚ#¢Êì€‘í‰ÕÉ¸ü¹±•Ğ èé¹Õµ‰•È¤€üè€‹ŠP‰ôˆ¤ô(€€€€€€€€€€€ô(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸	…É¡…ÉĞ¡‘…Ñ•Ìè1¥ÍĞñ1½…±…Ñ”ø°Ù…±Õ•Ìè1¥ÍĞñ½Õ‰±”ø°½…°è½Õ‰±”ü¤ì(€€€Ù…°µ…á¥µÕ´€ôµ…á=˜¡Ù…±Õ•Ì¹µ…á=É9Õ±° ¤€üè€Ä¸À°½…°€üè€À¸À°€Ä¸À¤(€€€½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ğ ÈĞÀ¹‘À¤¹Á…‘‘¥¹œ¡Ñ½ÀôÄÀ¹‘À¤¤ì(€€€€€€€I½Ü¡5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä Ø¹‘À¤°Ù•ÉÑ¥…±±¥¹µ•¹Ğõ±¥¹µ•¹Ğ¹	½ÑÑ½´¤ì(€€€€€€€€€€€Ù…±Õ•Ì¹™½É…¡%¹‘•á•ì¥¹‘•à°Ù…±Õ”€´ø(€€€€€€€€€€€€€€€½±Õµ¸¡5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤°¡½É¥é½¹Ñ…±±¥¹µ•¹Ğõ±¥¹µ•¹Ğ¹•¹Ñ•É!½É¥é½¹Ñ…±±ä°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹	½ÑÑ½´¤ì(€€€€€€€€€€€€€€€€€€€Q•áĞ¡¹Õµ‰•È¡Ù…±Õ”¤°½±½Èõ5ÕÑ•¤(€€€€€€€€€€€€€€€€€€€	½à¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¸ÜÕ˜¤¹™¥±±5…á!•¥¡Ğ ¡Ù…±Õ”½µ…á¥µÕ´¤¹Ñ½±½…Ğ ¤¹½•É•%¸ ¸ÀÉ˜°Å˜¤¤¹Ñ¡•¸¡5½‘¥™¥•È¤°½¹Ñ•¹Ñ±¥¹µ•¹Ğõ±¥¹µ•¹Ğ¹•¹Ñ•È¤ì(€€€€€€€€€€€€€€€€€€€€€€€…¹Ù…Ì¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤¤ì‘É…İI•Ğ¡¥˜¡¥¹‘•àôõÙ…±Õ•Ì¹±…ÍÑ%¹‘•à¤½±½È ÁáÉ	¤•±Í”½±½È¹QÉ…¹ÍÁ…É•¹Ğ¤ì‘É…İI•Ğ¡•¹Ğ°ÍÑå±”õMÑÉ½­”¡İ¥‘Ñ ôÄ¹‘À¹Ñ½Aà ¤¤¤ô(€€€€€€€€€€€€€€€€€€€ô(€€€€€€€€€€€€€€€ô(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä Ø¹‘À¤¤ì‘…Ñ•Ì¹™½É… ìQ•áĞ ˆ‘í¥Ğ¹µ½¹Ñ¡Y…±Õ•ô¼‘í¥Ğ¹‘…å=™5½¹Ñ¡ôˆ°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤°Ñ•áÑ±¥¸õQ•áÑ±¥¸¹•¹Ñ•È°½±½Èõ5ÕÑ•¤ôô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸5…¹Õ…±Ñ¥Ù¥Ñå¥…±½œ¡•á¥ÍÑ¥¹œè…¥±åÑ¥Ù¥Ñå¹Ñ¥Ñäü°½¹¥Íµ¥ÍÌè€ ¤€´øU¹¥Ğ°½¹M…Ù”è€¡…¥±åÑ¥Ù¥Ñå¹Ñ¥Ñä¤€´øU¹¥Ğ¤ì(€€€Ù…ÈÑ½Ñ…°‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹Ñ½Ñ…±…±½É¥•Í-…°ü¹±•Ğ èé¹Õµ‰•È¤¹½ÉµÁÑä ¤¤ô(€€€Ù…È…Ñ¥Ù”‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹…Ñ¥Ù•…±½É¥•Í-…°ü¹±•Ğ èé¹Õµ‰•È¤¹½ÉµÁÑä ¤¤ô(€€€Ù…ÈÍÑ•ÁÌ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹ÍÑ•ÁÌü¹Ñ½MÑÉ¥¹œ ¤¹½ÉµÁÑä ¤¤ô(€€€Ù…È•á•É¥Í”‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹•á•É¥Í•5¥¹ÕÑ•Ìü¹Ñ½MÑÉ¥¹œ ¤¹½ÉµÁÑä ¤¤ô(€€€Ù…ÈÍ±••À‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹Í±••Á5¥¹ÕÑ•Ìü¹Ñ½MÑÉ¥¹œ ¤¹½ÉµÁÑä ¤¤ô(€€€Ù…È¡È‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á¥ÍÑ¥¹œü¹É•ÍÑ¥¹!Èü¹Ñ½MÑÉ¥¹œ ¤¹½ÉµÁÑä ¤¤ô(€€€±•ÉÑ¥…±½œ¡½¹¥Íµ¥ÍÍI•ÅÕ•ÍĞõ½¹¥Íµ¥ÍÌ°Ñ¥Ñ±”õíQ•áĞ ‹šÒï–.Wó
ÿ
Kš&/–—–*lˆ¥ô°Ñ•áĞõì(€€€€€€€½±Õµ¸¡5½‘¥™¥•È¹Ù•ÉÑ¥…±MÉ½±°¡É•µ•µ‰•ÉMÉ½±±MÑ…Ñ” ¤¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä à¹‘À¤¤ì(€€€€€€€€€€€±¥ÍÑ=˜ ‹šÚ#¢Êí­…°ˆÑ¼Ñ½Ñ…°°€‹šÒï–.U­…°ˆÑ¼…Ñ¥Ù”°€‹š¶§šVÀˆÑ¼ÍÑ•ÁÌ°€‹¦/–.W–"ˆÑ¼•á•É¥Í”°€‹v‡rƒ–"ˆÑ¼Í±••À°€‹–º'¦vgšf–şš.4ˆÑ¼¡È¤¹™½É…¡%¹‘•á•ì¥¹‘•à°Á…¥È€´ø(€€€€€€€€€€€€€€€A…Á•É¥•±¡Á…¥È¹Í•½¹°íØ€´øİ¡•¸¡¥¹‘•à¥ìÀ´ùÑ½Ñ…°õØìÄ´ù…Ñ¥Ù”õØìÈ´ùÍÑ•ÁÌõØìÌ´ù•á•É¥Í”õØìĞ´ùÍ±••ÀõØí•±Í”´ù¡ÈõÙõô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°±…‰•°õÁ…¥È¹™¥ÉÍĞ¤(€€€€€€€€€€€ô(€€€€€€€ô(€€€ô°½¹™¥Éµ	ÕÑÑ½¸õíQ•áÑ	ÕÑÑ½¹1¥­” ‹’şw–¶`ˆ°½¹±¥¬õì½¹M…Ù”¡…¥±åÑ¥Ù¥Ñå¹Ñ¥Ñä¡1½…±…Ñ”¹¹½Ü ¤¹Ñ½MÑÉ¥¹œ ¤°Ñ½Ñ…°¹Ñ½½Õ‰±•=É9Õ±° ¤°…Ñ¥Ù”¹Ñ½½Õ‰±•=É9Õ±° ¤°ÍÑ•ÁÌ¹Ñ½1½¹=É9Õ±° ¤°•á•É¥Í”¹Ñ½%¹Ñ=É9Õ±° ¤°Í±••À¹Ñ½%¹Ñ=É9Õ±° ¤°¡È¹Ñ½%¹Ñ=É9Õ±° ¤¤¤ô¥ô°‘¥Íµ¥ÍÍ	ÕÑÑ½¸õíQ•áÑ	ÕÑÑ½¹1¥­” ‹
·Ï
ï¬ˆ°½¹¥Íµ¥ÍÌ¥ô¤)ô()ÁÉ¥Ù…Ñ”™Õ¸MÑÉ¥¹œ¹™¥±Ñ•É9Õµ‰•È ¤€ô™¥±Ñ•Èì¥Ğ¹¥Í¥¥Ğ ¤ñğ¥Ğôôœ¸œñğ¥Ğôôœ´œô)ÁÉ¥Ù…Ñ”™Õ¸5…ÀñMÑÉ¥¹œ±MÑÉ¥¹œø¹¡­•äèMÑÉ¥¹œ¤€ôÑ¡¥Ím­•åtü¹Ñ½½Õ‰±•=É9Õ±° ¤€üè€À¸À()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸!•…±Ñ¡Õ¥‘•MÉ••¸¡Ù´è5…¥¹Y¥•İ5½‘•°°½¹	…¬è€ ¤€´øU¹¥Ğ°½¹A•Éµ¥ÍÍ¥½¸è€ ¤€´øU¹¥Ğ¤ì(€€€Ù…°½¹Ñ•áĞ€ô1½…±½¹Ñ•áĞ¹ÕÉÉ•¹Ğ(€€€Ù…°…Ù…¥±…‰¥±¥Ñä€ôÉ•µ•µ‰•ÈìÙ´¹¡•…±Ñ ¹…Ù…¥±…‰¥±¥Ñä ¤ô(€€€½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤¹Ù•ÉÑ¥…±MÉ½±°¡É•µ•µ‰•ÉMÉ½±±MÑ…Ñ” ¤¤¹Á…‘‘¥¹œ ÈÀ¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä Äà¹‘À¤¤ì(€€€€€€€A…•!•…‘•È ‰…Éµ¥¸ƒ¦šBë¢¢·–ºhˆ°€‹–º3’êˆ°½¹	…¬¤(€€€€€€€Q•áĞ ‹ó
ÿ»šÖ
0ˆ°½±½Èõ5ÕÑ•¤(€€€€€€€Q•áĞ ‰Y•¹Ô€ÈA±ÕÌƒŠH…Éµ¥¸½¹¹•ĞƒŠHƒc¯
ç
Ï7
¿ ƒŠHƒ»š‚¦’+¢¢c¦2Èˆ¤(€€€€€€€Q•áĞ ‹šr³
‹_«½…Éµ¥»ãnÓš:—š:—Úkok®¿šr¯–»c¯
ç
Ï7
¿#
K¢ª·ÿ–>[
+ûgšnã7¢úóÿš¢§¦fC
–’[¦£¦k’ş‡¿’öÿR£_ûo
Oˆ¤(€€€€€€€!½É¥é½¹Ñ…±¥Ù¥‘•È¡½±½ÈõIÕ±”¤(€€€€€€€Õ¥‘•MÑ•À ˆÄˆ°€‰¹‘É½¥“»¢¢·–ºkŸc¯
ç
Ï7
¿#
K¦Z/<ˆ°€‹¢¢·–ºhƒŠHƒ
ï
·—«
£_§
“C
ßğƒŠHƒ_§
“C
ßğƒŠHƒc¯
ç
Ï7
¿ ˆ¤(€€€€€€€Õ¥‘•MÑ•À ˆÈˆ°€‰…Éµ¥¸½¹¹•Ó/
'–Çšr'g
,ˆ°€‰…Éµ¥¸½¹¹•ĞƒŠHƒ¢¢·–ºhƒŠHƒš:—Úkšâ#ÿ
‹_¨ƒŠH!•…±Ñ ½¹¹•Óš¶§šVÃï
¯·«óï–şš.7ïv‡rƒ
K¢¢Ç–>¿_ûgˆ¤(€€€€€€€Õ¥‘•MÑ•À ˆÌˆ°€‹šr³
‹_«ã¢ª·ÿ–>[
+
K¢¢Ç–>¿g
,ˆ°€‹’â/»s
ÿÏ/
'¢†£’ëW
3|ß¢»¦†{»¢ª·ÿ–>[
+š¢§¦fC
K¢¢Ç–>¿_ûgˆ¤(€€€€€€€Q•áĞ ‹*Ûš,è€‘íİ¡•¸¡…Ù…¥±…‰¥±¥Ñä¥í!•…±Ñ¡Ù…¥±…‰¥±¥Ñä¹Y%1	1´ø‹–"§R£–>¿¢ôˆí!•…±Ñ¡Ù…¥±…‰¥±¥Ñä¹9M}%9MQ10´ø‹šnÓšZÃ3–ş¢šˆí!•…±Ñ¡Ù…¥±…‰¥±¥Ñä¹9=Q}MUAA=IQ´ø‹O»®¿šr¯Ÿ¿–"§R£’â7–>¼‰õôˆ°½±½Èõ¥˜¡…Ù…¥±…‰¥±¥Ñäôõ!•…±Ñ¡Ù…¥±…‰¥±¥Ñä¹Y%1	1¤•¹Ğ•±Í”5ÕÑ•¤(€€€€€€€1¥¹•	ÕÑÑ½¸ ‹¢ª·ÿ–>[
+š¢§¦fC
K¢¢·–ºhˆ°½¹A•Éµ¥ÍÍ¥½¸°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°…Ù…¥±…‰¥±¥Ñäôõ!•…±Ñ¡Ù…¥±…‰¥±¥Ñä¹Y%1	1¤(€€€€€€€1¥¹•	ÕÑÑ½¸ ‹c¯
ç
Ï7
¿#¢¢·–ºk
K¦Z/<ˆ°ì(€€€€€€€€€€€ÉÕ¹…Ñ¡¥¹œì½¹Ñ•áĞ¹ÍÑ…ÉÑÑ¥Ù¥Ñä¡%¹Ñ•¹Ğ ‰…¹‘É½¥¹¡•…±Ñ ¹½¹¹•Ğ¹…Ñ¥½¸¹!1Q!}!=5}MQQ%9Lˆ¤¤ô(€€€€€€€ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤(€€€€€€€!½É¥é½¹Ñ…±¥Ù¥‘•È¡½±½ÈõIÕ±”¤(€€€€€€€Q•áĞ ‹–B3šrŸ7«–‚Ó–B ˆ°ÍÑå±”õ…¹‘É½¥‘à¹½µÁ½Í”¹µ…Ñ•É¥…°Ì¹5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹Ñ¥Ñ±•1…É”¤(€€€€€€€Q•áĞ ‰…Éµ¥¸½¹¹•Ó–Ó»–B3šr¢¢·–ºk£šrÖ–B3šršf–"ï
KŠë¢ª7_›?ƒWc¯
ç
Ï7
¿#
K–"§R£Ÿ7«–‚Ó–B#¿oóƒ»3š&/–—–*o7û¿–ë–*oRï¦v‹¹…Éµ¥¸M[–>[¢úó
K’öÿ#ûgˆ°½±½Èõ5ÕÑ•¤(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸Õ¥‘•MÑ•À¡¹Õµ‰•ÈèMÑÉ¥¹œ°Ñ¥Ñ±”èMÑÉ¥¹œ°‰½‘äèMÑÉ¥¹œ¤ì(€€€I½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄÈ¹‘À¤¤ì(€€€€€€€Q•áĞ¡¹Õµ‰•È°½±½Èõ•¹Ğ°ÍÑå±”õ…¹‘É½¥‘à¹½µÁ½Í”¹µ…Ñ•É¥…°Ì¹5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹Ñ¥Ñ±•1…É”¤(€€€€€€€½±Õµ¸ìQ•áĞ¡Ñ¥Ñ±”°™½¹Ñ]•¥¡Ğõ½¹Ñ]•¥¡Ğ¹M•µ¥	½±¤ìQ•áĞ¡‰½‘ä°½±½Èõ5ÕÑ•¤ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸áÁ½ÉÑMÉ••¸¡Ù´è5…¥¹Y¥•İ5½‘•°¤ì(€€€Ù…°½¹Ñ•áĞ€ô1½…±½¹Ñ•áĞ¹ÕÉÉ•¹Ğ(€€€Ù…°Í½Á”€ôÉ•µ•µ‰•É½É½ÕÑ¥¹•M½Á” ¤(€€€Ù…È™É½´‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•MÑ…Ñ•=˜¡1½…±…Ñ”¹¹½Ü ¤¹µ¥¹ÕÍ…åÌ Ø¤¤ô(€€€Ù…ÈÑ¼‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•MÑ…Ñ•=˜¡1½…±…Ñ”¹¹½Ü ¤¤ô(€€€Ù…Èµ…É­‘½İ¸‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•MÑ…Ñ•=˜¡ÑÉÕ”¤ô(€€€Ù…ÈÑ…É•Ğ‰äÉ•µ•µ‰•ÉM…Ù•…‰±”ìµÕÑ…‰±•MÑ…Ñ•=˜ ‹gç˜ˆ¤ô(€€€Ù…ÈÁÉ•Ù¥•Ü‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ô(€€€Ù…È‰åÑ•Ì‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡	åÑ•ÉÉ…ä À¤¤ô(€€€Ù…ÈÁ•¹‘¥¹œ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ñA…¥ÈñMÑÉ¥¹œ±	åÑ•ÉÉ…äøüø¡¹Õ±°¤ô(€€€Ù…°Í…Ù•1…Õ¹¡•È€ôÉ•µ•µ‰•É1…Õ¹¡•É½ÉÑ¥Ù¥ÑåI•ÍÕ±Ğ¡Ñ¥Ù¥ÑåI•ÍÕ±Ñ½¹ÑÉ…ÑÌ¹É•…Ñ•½Õµ•¹Ğ ˆ¨¼¨ˆ¤¤ìÕÉ¤€´ø(€€€€€€€ÕÉ¤ü¹±•Ğì½¹Ñ•áĞ¹½¹Ñ•¹ÑI•Í½±Ù•È¹½Á•¹=ÕÑÁÕÑMÑÉ•…´¡¥Ğ¤ü¹ÕÍ”ì½ÕĞ€´ø½ÕĞ¹İÉ¥Ñ”¡Á•¹‘¥¹œü¹Í•½¹€üè‰åÑ•Ì¤ôô(€€€ô(€€€Ù…°‰…­ÕÁ%µÁ½ÉĞ€ôÉ•µ•µ‰•É1…Õ¹¡•É½ÉÑ¥Ù¥ÑåI•ÍÕ±Ğ¡Ñ¥Ù¥ÑåI•ÍÕ±Ñ½¹ÑÉ…ÑÌ¹=Á•¹½Õµ•¹Ğ ¤¤ìÕÉ¤€´ø(€€€€€€€ÕÉ¤ü¹±•ĞìÍ½Á”¹±…Õ¹ ìÉÕ¹…Ñ¡¥¹œì½¹Ñ•áĞ¹½¹Ñ•¹ÑI•Í½±Ù•È¹½Á•¹%¹ÁÕÑMÑÉ•…´¡¥Ğ¤„„¹‰Õ™™•É•‘I•…‘•È ¤¹ÕÍ”ìÈ€´øÙ´¹É•ÍÑ½É”¡È¹É•…‘Q•áĞ ¤¤ôôôô(€€€ô(€€€Ù…°…Éµ¥¹%µÁ½ÉĞ€ôÉ•µ•µ‰•É1…Õ¹¡•É½ÉÑ¥Ù¥ÑåI•ÍÕ±Ğ¡Ñ¥Ù¥ÑåI•ÍÕ±Ñ½¹ÑÉ…ÑÌ¹=Á•¹½Õµ•¹Ğ ¤¤ìÕÉ¤€´ø(€€€€€€€ÕÉ¤ü¹±•Ğì½¹Ñ•áĞ¹½¹Ñ•¹ÑI•Í½±Ù•È¹½Á•¹%¹ÁÕÑMÑÉ•…´¡¥Ğ¤ü¹‰Õ™™•É•‘I•…‘•È ¤ü¹ÕÍ”ìÈ€´øÙ´¹¥µÁ½ÉÑ…Éµ¥¹ÍØ¡È¹É•…‘Q•áĞ ¤¤ôô(€€€ô(€€€1…Õ¹¡•‘™™•Ğ¡™É½´°Ñ¼°µ…É­‘½İ¸°Ñ…É•Ğ¤ì(€€€€€€€¥˜€ …™É½´¹¥Í™Ñ•È¡Ñ¼¤¤ì(€€€€€€€€€€€Ù…°¹ÕÑÉ¥Ñ¥½¸€ôÙ´¹•áÁ½ÉĞ¡™É½´±Ñ¼±µ…É­‘½İ¸¤(€€€€€€€€€€€Ù…°İ½É­½ÕĞ€ôÙ´¹•áÁ½ÉÑ]½É­½ÕĞ¡™É½´±Ñ¼±µ…É­‘½İ¸¤(€€€€€€€€€€€Ù…°É•ÍÕ±Ğ€ôİ¡•¸¡Ñ…É•Ğ¤ì(€€€€€€€€€€€€€€€€‹š‚¦’(ˆ€´ø¹ÕÑÉ¥Ñ¥½¸(€€€€€€€€€€€€€€€€‹¶/#°ˆ€´øİ½É­½ÕĞ(€€€€€€€€€€€€€€€•±Í”€´øì(€€€€€€€€€€€€€€€€€€€¥˜¡µ…É­‘½İ¸¤ì(€€€€€€€€€€€€€€€€€€€€€€€Ù…°Ñ•áĞ€ô¹ÕÑÉ¥Ñ¥½¸¹™¥ÉÍĞ€¬€‰q¹q¸´´µq¹q¸ˆ€¬İ½É­½ÕĞ¹™¥ÉÍĞ(€€€€€€€€€€€€€€€€€€€€€€€Ñ•áĞÑ¼Ñ•áĞ¹Ñ½	åÑ•ÉÉ…ä ¤(€€€€€€€€€€€€€€€€€€€ô•±Í”İ½É­½ÕĞ(€€€€€€€€€€€€€€€ô(€€€€€€€€€€€ô(€€€€€€€€€€€ÁÉ•Ù¥•ÜõÉ•ÍÕ±Ğ¹™¥ÉÍĞì‰åÑ•ÌõÉ•ÍÕ±Ğ¹Í•½¹(€€€€€€€ô(€€€ô(€€€1…éå½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤°½¹Ñ•¹ÑA…‘‘¥¹œõA…‘‘¥¹Y…±Õ•Ì ÈÀ¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄĞ¹‘À¤¤ì(€€€€€€€¥Ñ•´ìA…•!•…‘•È ‹
£
¿
çwó ˆ¤ìQ•áĞ ‹¢¢c¦2Ë
KW
‡
“¯¯–ë–*o_!'ãšâ‡_›W
ó'C
¿
K–ú_
'
3ûgˆ°½±½Èõ5ÕÑ•°µ½‘¥™¥•Èõ5½‘¥™¥•È¹Á…‘‘¥¹œ¡Ñ½ÀôÄÀ¹‘À¤¤ô(€€€€€€€¥Ñ•´ìM•Ñ¥½¹Q¥Ñ±” ‹–ë–*o–¾û¢Æ„ˆ¤ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä à¹‘À¤¤ì(€€€€€€€€€€€±¥ÍÑ=˜ ‹š‚¦’(ˆ°‹¶/#°ˆ°‹gç˜ˆ¤¹™½É… ì¥Ñ•´€´ø1¥¹•	ÕÑÑ½¸¡¥Ñ•´°íÑ…É•Ğõ¥Ñ•´ì¥˜¡¥Ñ•´ôô‹gç˜ˆ¤µ…É­‘½İ¸õÑÉÕ•ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¹¡•¥¡Ğ ĞĞ¹‘À¤¤ô(€€€€€€€ôì¥˜¡Ñ…É•Ğôô‹gç˜ˆ¤Q•áĞ ‹ÖÇ–B#–ë–*o½5…É­‘½İ»–ö‹–ò?Ÿgˆ°½±½È€ô5ÕÑ•¤ô(€€€€€€€¥Ñ•´ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄÀ¹‘À¤¤ì(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹¦Z/–/š^•q¸‘™É½´ˆ°ìÍ¡½İ…Ñ•A¥­•È¡½¹Ñ•áĞ±™É½´¥í™É½´õ¥Ñôô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹Ö’êš^•q¸‘Ñ¼ˆ°ìÍ¡½İ…Ñ•A¥­•È¡½¹Ñ•áĞ±Ñ¼¥íÑ¼õ¥Ñôô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€ôô(€€€€€€€¥Ñ•´ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä ÄÀ¹‘À¤¤ì(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‰5…É­‘½İ¸€ ¹µ¤ˆ°íµ…É­‘½İ¸õÑÉÕ•ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‰MX€¡á•³R ¤ˆ°í¥˜¡Ñ…É•Ğ„ô‹gç˜ˆ¤µ…É­‘½İ¸õ™…±Í•ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤°Ñ…É•Ğ„ô‹gç˜ˆ¤(€€€€€€€ôô(€€€€€€€¥Ñ•´ìI½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹MÁ…•	•Ñİ••¸¤ìQ•áĞ ‹_³O—ğˆ°½±½Èõ5ÕÑ•¤ìQ•áĞ ˆ‘íÁÉ•Ù¥•Ü¹±¥¹•M•ÅÕ•¹” ¤¹½Õ¹Ğ ¥÷¢†0ˆ°½±½Èõ5ÕÑ•¤ôô(€€€€€€€¥Ñ•´ì=ÕÑ±¥¹•‘…É¡Í¡…Á”õI•Ñ…¹±•M¡…Á”°‰½É‘•Èõ	½É‘•ÉMÑÉ½­” Ä¹‘À±IÕ±”¤°µ½‘¥™¥•Èõ5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ğ ÌÀÀ¹‘À¤¤ìQ•áĞ¡ÁÉ•Ù¥•Ü°5½‘¥™¥•È¹Á…‘‘¥¹œ ÄÈ¹‘À¤¹Ù•ÉÑ¥…±MÉ½±°¡É•µ•µ‰•ÉMÉ½±±MÑ…Ñ” ¤¤¤ôô(€€€€€€€¥Ñ•´ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹ĞõÉÉ…¹•µ•¹Ğ¹ÍÁ…•‘	ä à¹‘À¤¤ì(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹W
‡
“¯’şw–¶`ˆ°ìÙ…°¹…µ”ô‹–—–êß¢¢c¦2É|‘íÑ…É•Ñõ|‘í™É½µõ|‘íÑ½ô¸‘í¥˜¡µ…É­‘½İ¸¤‰µˆ•±Í”€‰ÍØ‰ôˆìÁ•¹‘¥¹œõ¹…µ”Ñ¼‰åÑ•ÌìÍ…Ù•1…Õ¹¡•È¹±…Õ¹ ¡¹…µ”¤ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹–Çšr$ˆ°ìÍ¡…É•	åÑ•Ì¡½¹Ñ•áĞ°€‹–—–êß¢¢c¦2É|‘íÑ…É•Ñô¸‘í¥˜¡µ…É­‘½İ¸¤‰µˆ•±Í”€‰ÍØ‰ôˆ°‰åÑ•Ì°¥˜¡µ…É­‘½İ¸¤‰Ñ•áĞ½µ…É­‘½İ¸ˆ•±Í”€‰Ñ•áĞ½ÍØˆ¤ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹–£šZ
ÏSğˆ°ì€¡½¹Ñ•áĞ¹•ÑMåÍÑ•µM•ÉÙ¥”¡½¹Ñ•áĞ¹1%A	=I}MIY%¤…Ì±¥Á‰½…É‘5…¹…•È¤¹Í•ÑAÉ¥µ…Éå±¥À¡±¥Á…Ñ„¹¹•İA±…¥¹Q•áĞ ‹š‚¦’+¢¢c¦2Èˆ±ÁÉ•Ù¥•Ü¤¤ô°5½‘¥™¥•È¹İ•¥¡Ğ Å˜¤¤(€€€€€€€ôô(€€€€€€€¥Ñ•´ìM•Ñ¥½¹Q¥Ñ±” ‹C
¿
‹_£’îšnÿ–>[¢úğˆ¤ô(€€€€€€€¥Ñ•´ì1¥¹•	ÕÑÑ½¸ ‹–£ó
ÿ
I)M=;Ÿ’şw–¶`ˆ°ì(€€€€€€€€€€€Í½Á”¹±…Õ¹ ìÙ…°‘…Ñ„õÙ´¹‰…­ÕÀ ¤¹Ñ½	åÑ•ÉÉ…ä ¤ìÙ…°¹…µ”ô‰•¥å½…ÁÀµ‰…­ÕÀ´‘í1½…±…Ñ”¹¹½Ü ¥ô¹©Í½¸ˆìÁ•¹‘¥¹œõ¹…µ”Ñ¼‘…Ñ„ìÍ…Ù•1…Õ¹¡•È¹±…Õ¹ ¡¹…µ”¤ô(€€€€€€€ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ô(€€€€€€€¥Ñ•´ì1¥¹•	ÕÑÑ½¸ ‰)M=;C
¿
‹_
K–ú§–ˆ°í‰…­ÕÁ%µÁ½ÉĞ¹±…Õ¹ ¡…ÉÉ…å=˜ ‰…ÁÁ±¥…Ñ¥½¸½©Í½¸ˆ°‰Ñ•áĞ½Á±…¥¸ˆ¤¥ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ô(€€€€€€€¥Ñ•´ì1¥¹•	ÕÑÑ½¸ ‰…Éµ¥¸M[
K–>[
+¢úó
 ˆ°í…Éµ¥¹%µÁ½ÉĞ¹±…Õ¹ ¡…ÉÉ…å=˜ ‰Ñ•áĞ½ÍØˆ°‰Ñ•áĞ½½µµ„µÍ•Á…É…Ñ•µÙ…±Õ•Ìˆ°‰Ñ•áĞ½Á±…¥¸ˆ¤¥ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ô(€€€€€€€¥Ñ•´ìQ•áĞ ‹–¾û–şs–"_–B4è…Ñ”¿š^—’îcQ½Ñ…°…±½É¥•Ì¿šÚ#¢Êï
¯·«óMÑ•ÁÌ¿š¶§šVÃá•É¥Í”5¥¹ÕÑ•Ì¿¦/–.W–"M±••ÀÕÉ…Ñ¥½¸¿v‡rƒšf¦ZOI•ÍÑ¥¹œ!•…ÉĞI…Ñ”¿–º'¦vgšf–şš.7–>[¢úó–&7­)M=;C
¿
‹_
Kš:£––£_ûgˆ°½±½Èõ5ÕÑ•¤ô(€€€ô)ô()ÁÉ¥Ù…Ñ”™Õ¸Í¡½İ…Ñ•A¥­•È¡½¹Ñ•áĞè½¹Ñ•áĞ°ÕÉÉ•¹Ğè1½…±…Ñ”°½¹A¥­•è€¡1½…±…Ñ”¤€´øU¹¥Ğ¤ì(€€€…Ñ•A¥­•É¥…±½œ¡½¹Ñ•áĞ°í|±ä±´±€´ø½¹A¥­•¡1½…±…Ñ”¹½˜¡ä±´¬Ä±¤¥ô°ÕÉÉ•¹Ğ¹å•…È±ÕÉÉ•¹Ğ¹µ½¹Ñ¡Y…±Õ”´Ä±ÕÉÉ•¹Ğ¹‘…å=™5½¹Ñ ¤¹Í¡½Ü ¤)ô()ÁÉ¥Ù…Ñ”™Õ¸Í¡…É•	åÑ•Ì¡½¹Ñ•áĞè½¹Ñ•áĞ°¹…µ”èMÑÉ¥¹œ°‰åÑ•Ìè	åÑ•ÉÉ…ä°µ¥µ”èMÑÉ¥¹œ¤ì(€€€Ù…°‘¥Èõ¥±”¡½¹Ñ•áĞ¹…¡•¥È°‰•áÁ½ÉÑÌˆ¤¹…ÁÁ±äíµ­‘¥ÉÌ ¥ôìÙ…°™¥±”õ¥±”¡‘¥È±¹…µ”¤ì™¥±”¹İÉ¥Ñ•	åÑ•Ì¡‰åÑ•Ì¤(€€€Ù…°ÕÉ¤õ¥±•AÉ½Ù¥‘•È¹•ÑUÉ¥½É¥±”¡½¹Ñ•áĞ°ˆ‘í½¹Ñ•áĞ¹Á…­…•9…µ•ô¹™¥±•Ìˆ±™¥±”¤(€€€½¹Ñ•áĞ¹ÍÑ…ÉÑÑ¥Ù¥Ñä¡%¹Ñ•¹Ğ¹É•…Ñ•¡½½Í•È¡%¹Ñ•¹Ğ¡%¹Ñ•¹Ğ¹Q%=9}M9¤¹…ÁÁ±äì(€€€€€€€ÑåÁ”õµ¥µ”ìÁÕÑáÑÉ„¡%¹Ñ•¹Ğ¹aQI}MQI4±ÕÉ¤¤ì…‘‘±…Ì¡%¹Ñ•¹Ğ¹1}I9Q}I}UI%}AI5%MM%=8¤(€€€ô°‹š‚¦’+¢¢c¦2Ë
K–Çšr$ˆ¤¤)ô(