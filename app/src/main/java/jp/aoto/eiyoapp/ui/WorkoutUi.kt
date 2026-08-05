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
            SmallModeButton("ãƒ›ãƒ¼ãƒ ", page == "home", { page = "home" }, Modifier.weight(1f))
            SmallModeButton("ç¨®ç›®", page == "library", { page = "library" }, Modifier.weight(1f))
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
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("Mæœˆdæ—¥ E", Locale.JAPANESE)), color = Accent, fontSize = 11.sp)
            Text("ä»Šæ—¥ã‚‚ã€éŽåŽ»ã®è‡ªåˆ†ã‚’å°‘ã—ã ã‘è¶…ãˆã‚‹ã€‚", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp))
        }
        if (active != null) item {
            BrassCard {
                Text("å®Ÿè¡Œä¸­ã®ãƒ¯ãƒ¼ã‚¯ã‚¢ã‚¦ãƒˆ", color = Accent, fontSize = 11.sp)
                Text("ä¸­æ–­ã—ãŸã¨ã“ã‚ã‹ã‚‰å†é–‹ã§ãã¾ã™", modifier = Modifier.padding(vertical = 8.dp))
                LineButton("ãƒ¯ãƒ¼ã‚¯ã‚¢ã‚¦ãƒˆã«æˆ»ã‚‹", onOpenSession, Modifier.fillMaxWidth())
            }
        }
        item {
            BrassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column { Text("ä»Šé€±ã®ç›®æ¨™", color = Muted); Row(verticalAlignment = Alignment.Bottom) {
                        Text(weekDone.toString(), color = Accent, fontSize = 58.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                        Text(" / $weekGoal å›ž", color = Muted, modifier = Modifier.padding(bottom = 11.dp))
                    } }
                    Text("æ®‹ã‚Š ${(weekGoal - weekDone).coerceAtLeast(0)}å›ž", color = Muted)
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
                Text("$streak é€±é€£ç¶šã§é”æˆä¸­", color = Accent, fontSize = 20.sp)
                Text("1å›žä¼‘ã‚“ã§ã‚‚é€£ç¶šã¯é€”åˆ‡ã‚Œã¾ã›ã‚“ã€‚æœªé”ã®é€±ã¯ãƒªã‚«ãƒãƒªãƒ¼é€±ã¨ã—ã¦æ•°ãˆã¾ã™ã€‚", color = Muted, fontSize = 11.sp)
            }
        }
        item { WorkoutSectionTitle("æ¬¡ã«ã‚„ã‚‹ã¹ãç¨®ç›®") }
        if (exercises.isEmpty()) item {
            Text("ã¾ã ç¨®ç›®ãŒã‚ã‚Šã¾ã›ã‚“ã€‚ãƒžã‚·ãƒ³ã®å‰ã§15ç§’ç™»éŒ²ã‹ã‚‰å§‹ã‚ã¾ã—ã‚‡ã†ã€‚", color = Muted)
        }
        items(exercises.take(3), key = { it.id }) { exercise ->
            val last = lastSets.firstOrNull { it.exerciseId == exercise.id }
            ExerciseStartCard(exercise, last) {
                vm.startWorkout(exercise.id) { onOpenSession() }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineButton("ï¼‹ æ–°ã—ã„ç¨®ç›®ã‚’ç™»éŒ²", { addOpen = true }, Modifier.weight(1f).height(54.dp))
                LineButton("ä»Šæ—¥ã¯ã“ã‚Œã ã‘", {
                    exercises.firstOrNull()?.let { vm.startWorkout(it.id) { onOpenSession() } }
                    if (exercises.isEmpty()) addOpen = true
                }, Modifier.weight(1f).height(54.dp))
            }
        }
        item {
            val xp = settings.firstOrNull { it.key == "xp" }?.value?.toIntOrNull() ?: sessions.size * 10
            BrassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lv.${xp / 100 + 1}  ç¶™ç¶šã®ç¿’æ…£")
                    Text("${xp % 100} / 100", color = Accent)
                }
                ProgressLine((xp % 100) / 100f)
                Text("éŽåŽ»ã®è‡ªåˆ†ã¨ã®æ¯”è¼ƒã ã‘ã‚’è¡¨ç¤ºã—ã¾ã™ã€‚", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
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
    var filter by rememberSaveable { mutableStateOf("ã™ã¹ã¦") }
    var addOpen by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<WorkoutExerciseEntity?>(null) }
    var merge by remember { mutableStateOf<WorkoutExerciseEntity?>(null) }
    val filtered = exercises.filter { filter == "ã™ã¹ã¦" || (filter == "è‡ªé‡" && it.unit != "kg") || it.part == filter }

    Column(Modifier.fillMaxSize()) {
        Text("ç¨®ç›®ãƒ©ã‚¤ãƒ–ãƒ©ãƒª", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(listOf("ã™ã¹ã¦", "èƒ¸", "èƒŒä¸­", "è„š", "è‚©", "è…•", "ä½“å¹¹", "è‡ªé‡")) { part ->
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
                                    if (exercise.provisional) Text("ä»®å", color = Accent, fontSize = 10.sp)
                                }
                                Text("${exercise.part} Â· ${if (exercise.unit == "kg") "${Exporter.fmt(exercise.stepKg)}kgåˆ»ã¿" else if (exercise.unit == "sec") "ç§’æ•°" else "è‡ªé‡"}", color = Muted, fontSize = 12.sp)
                            }
                            TextButton(onClick = { vm.startWorkout(exercise.id) { onOpenSession() } }) { Text("é–‹å§‹", color = Accent) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallModeButton("ãƒªãƒãƒ¼ãƒ ", false, { rename = exercise }, Modifier.weight(1f))
                            SmallModeButton("çµ±åˆ", false, { merge = exercise }, Modifier.weight(1f), enabled = exercises.size > 1)
                        }
                    }
                }
            }
            item { LineButton("ï¼‹ ãƒžã‚·ãƒ³ã®å‰ã§15ç§’ç™»éŒ²", { addOpen = true }, Modifier.fillMaxWidth().height(58.dp)) }
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ãƒ¯ãƒ¼ã‚¯ã‚¢ã‚¦ãƒˆã‚’èª­ã¿è¾¼ã¿ä¸­â€¦") }
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
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.creatÛm=¶‰žËkºwµç}ÉåM•ÑÌ¹½±±•ÑÍMÑ…Ñ” ¤(€€€Ù…°µ•ÑÉ¥Ì‰äÙ´¹‰½‘å5•ÑÉ¥Ì¹½±±•ÑÍMÑ…Ñ” ¤(€€€Ù…È‰½‘å=Á•¸‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡™…±Í”¤ô(€€€Ù…°‰åA…ÉÐ€ôÍ•ÑÌ¹É½ÕÁ¥¹	äì¥Ð¹Á…ÉÐô¹•…¡½Õ¹Ð ¤(€€€Ù…°±…Ñ•ÍÑá•É¥Í”€ôÍ•ÑÌ¹±…ÍÑ=É9Õ±°ì¥Ð¹Õ¹¥Ð€ôô€‰­œˆôü¹•á•É¥Í•%(€€€Ù…°½¹•I´€ôÍ•ÑÌ¹™¥±Ñ•Èì¥Ð¹•á•É¥Í•%€ôô±…Ñ•ÍÑá•É¥Í”ô¹µ…Á9½Ñ9Õ±°ì]½É­½ÕÑ5…Ñ ¹•ÍÑ¥µ…Ñ•‘=¹•I•Á5…à¡¥Ð¹Ý•¥¡Ñ-œ°¥Ð¹É•ÁÌ¤ô¹µ…á=É9Õ±° ¤(€€€1…éå½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤°½¹Ñ•¹ÑA…‘‘¥¹œ€ôA…‘‘¥¹Y…±Õ•Ì Äà¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä ÄØ¹‘À¤¤ì(€€€€€€€¥Ñ•´ìQ•áÐ ‹¦Ëš6\ˆ°ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹¡•…‘±¥¹•1…É”¤ìQ•áÐ ‹š¾SŽçŽ
/žnãš&/Ž¿Ž¦;–:ïŽ»¢«–"ŽƒŽGŽˆ°½±½È€ô5ÕÑ•¤ô(€€€€€€€¥Ñ•´ì	É…ÍÍ…ÉìQ•áÐ ‹š:£–ºhÅI4ˆ°½±½È€ô5ÕÑ•¤ìQ•áÐ¡½¹•I´ü¹±•Ðì€ˆ‘íáÁ½ÉÑ•È¹™µÐ¡¥Ð¥ô­œˆô€üè€‹¢¢c¦2Ë–úŽ„ˆ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€ÌÐ¹ÍÀ¤ìQ•áÐ ‰Á±•ç–ò?Žïž¢»žn»ŽSŽ£Ž»šr–’Ÿ–ˆ°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÄ¹ÍÀ¤ôô(€€€€€€€¥Ñ•´ì	É…ÍÍ…ÉìQ•áÐ ‹žÞ?ŽsŽ«Ž—ŽóŽ€ˆ°½±½È€ô5ÕÑ•¤ìQ•áÐ ˆ‘íáÁ½ÉÑ•È¹™µÐ¡Í•ÑÌ¹ÍÕµ=˜ì]½É­½ÕÑ5…Ñ ¹Ù½±Õµ”¡¥Ð¹Ý•¥¡Ñ-œ°¥Ð¹É•ÁÌ¤ô¥ô­œˆ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€ÌÈ¹ÍÀ¤ôô(€€€€€€€¥Ñ•´ì]½É­½ÕÑM•Ñ¥½¹Q¥Ñ±” ‹¦£’ö7–"—Ž
ïŽŽ#šVÀˆ¤ô(€€€€€€€¥Ñ•µÌ¡±¥ÍÑ=˜ ‹¢àˆ°€‹¢3’â´ˆ°€‹¢hˆ°€‹¢
¤ˆ°€‹¢Tˆ°€‹’öO–æäˆ¤¤ìÁ…ÉÐ€´ø(€€€€€€€€€€€Ù…°½Õ¹Ð€ô‰åA…ÉÑmÁ…ÉÑt€üè€À(€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä¤ì(€€€€€€€€€€€€€€€Q•áÐ¡Á…ÉÐ°5½‘¥™¥•È¹Ý¥‘Ñ  Ðà¹‘À¤°½±½È€ô5ÕÑ•¤(€€€€€€€€€€€€€€€	½à¡5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤¹¡•¥¡Ð ÄÈ¹‘À¤¹‰…­É½Õ¹¡IÕ±”°I½Õ¹‘•‘½É¹•ÉM¡…Á” È¹‘À¤¤¤ì(€€€€€€€€€€€€€€€€€€€	½à¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¡½Õ¹Ð€¼€ÄÙ˜¤¹½•É•%¸ Á˜°€Å˜¤¤¹¡•¥¡Ð ÄÈ¹‘À¤¹‰…­É½Õ¹¡¥˜€¡½Õ¹Ð¥¸€Ø¸¸ÄÈ¤•¹Ð•±Í”5ÕÑ•°I½Õ¹‘•‘½É¹•ÉM¡…Á” È¹‘À¤¤¤(€€€€€€€€€€€€€€€ô(€€€€€€€€€€€€€€€Q•áÐ¡½Õ¹Ð¹Ñ½MÑÉ¥¹œ ¤°5½‘¥™¥•È¹Ý¥‘Ñ  ÌØ¹‘À¤°Ñ•áÑ±¥¸€ôQ•áÑ±¥¸¹¹°½±½È€ô¥˜€¡½Õ¹Ð¥¸€Ø¸¸ÄÈ¤•¹Ð•±Í”5ÕÑ•¤(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€¥Ñ•´ì	É…ÍÍ…ÉìQ•áÐ ‹Ž/Ž
'ŽƒŽ»¢¢c¦2Èˆ°½±½È€ô5ÕÑ•¤ìÙ…°±…Ñ•ÍÐ€ôµ•ÑÉ¥Ì¹±…ÍÑ=É9Õ±° ¤ìQ•áÐ¡±…Ñ•ÍÐü¹Ý•¥¡Ñ-œü¹±•Ðì€‹’öO¦4€‘íáÁ½ÉÑ•È¹™µÐ¡¥Ð¥õ­œˆô€üè€‹ŽûŽƒ¢¢c¦2ËŽ3ŽŽ
+ŽûŽoŽ
Lˆ¤ì1¥¹•	ÕÑÑ½¸ ‹’î+š^—Ž»’öO¦7Žï–F£–úŽ
K¢¢c¦2Èˆ°ì‰½‘å=Á•¸€ôÑÉÕ”ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Á…‘‘¥¹œ¡Ñ½À€ô€ÄÀ¹‘À¤¤ôô(€€€ô(€€€¥˜€¡‰½‘å=Á•¸¤	½‘å5•ÑÉ¥¥…±½œ¡Ù´¤ì‰½‘å=Á•¸€ô™…±Í”ô)ô()½µÁ½Í…‰±”)™Õ¸]½É­½ÕÑM•ÑÑ¥¹ÍMÉ••¸¡Ù´è5…¥¹Y¥•Ý5½‘•°°½¹!•…±Ñ¡Õ¥‘”è€ ¤€´øU¹¥Ð°½¹½…±Ìè€ ¤€´øU¹¥Ð¤ì(€€€Ù…°Í•ÑÑ¥¹Ì‰äÙ´¹Ý½É­½ÕÑM•ÑÑ¥¹Ì¹½±±•ÑÍMÑ…Ñ” ¤(€€€Ù…°¡½µ”‰äÙ´¹¡½µ”¹½±±•ÑÍMÑ…Ñ” ¤(€€€™Õ¸Í•ÑÑ¥¹œ¡­•äèMÑÉ¥¹œ°™…±±‰…¬èMÑÉ¥¹œ¤€ôÍ•ÑÑ¥¹Ì¹™¥ÉÍÑ=É9Õ±°ì¥Ð¹­•ä€ôô­•äôü¹Ù…±Õ”€üè™…±±‰…¬(€€€Ù…È½…°‰äÉ•µ•µ‰•È¡Í•ÑÑ¥¹Ì¤ìµÕÑ…‰±•MÑ…Ñ•=˜¡Í•ÑÑ¥¹œ ‰Ý••­½…°ˆ°€ˆÈˆ¤¤ô(€€€Ù…ÈÉ•ÍÐ‰äÉ•µ•µ‰•È¡Í•ÑÑ¥¹Ì¤ìµÕÑ…‰±•MÑ…Ñ•=˜¡Í•ÑÑ¥¹œ ‰É•ÍÑM•½¹‘Ìˆ°€ˆäÀˆ¤¤ô(€€€Ù…ÈÍÑ•À‰äÉ•µ•µ‰•È¡Í•ÑÑ¥¹Ì¤ìµÕÑ…‰±•MÑ…Ñ•=˜¡Í•ÑÑ¥¹œ ‰‘•™…Õ±ÑMÑ•Á-œˆ°€ˆÈ¸Ôˆ¤¤ô(€€€1…éå½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…áM¥é” ¤°½¹Ñ•¹ÑA…‘‘¥¹œ€ôA…‘‘¥¹Y…±Õ•Ì Äà¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä ÄÐ¹‘À¤¤ì(€€€€€€€¥Ñ•´ìQ•áÐ ‹¢¢·–ºkŽï¢¢c¦2Èˆ°ÍÑå±”€ô5…Ñ•É¥…±Q¡•µ”¹ÑåÁ½É…Á¡ä¹¡•…‘±¥¹•1…É”¤ô(€€€€€€€¥Ñ•´ì	É…ÍÍ…ÉìQ•áÐ ‹’î+š^—Ž»Ž
ÏŽÏŽŽ
Ž
ßŽŸŽÌˆ°½±½È€ô5ÕÑ•¤ìQ•áÐ ‹žv‡žr€€€‘í¡½µ”¹…Ñ¥Ù¥Ñäü¹Í±••Á5¥¹ÕÑ•Ìü¹±•Ðì€ˆ‘í¥Ð€¼€ØÁôè‘ì¡¥Ð€”€ØÀ¤¹Ñ½MÑÉ¥¹œ ¤¹Á…‘MÑ…ÉÐ È°€œÀœ¥ôˆô€üè€‹ŠP‰ôˆ°™½¹ÑM¥é”€ô€ÈÀ¹ÍÀ¤ìQ•áÐ ‹Ž
ÿŽÏŽGŽ
¿¢Î¨€€‘íáÁ½ÉÑ•È¹™µÐ¡¡½µ”¹Ñ½Ñ…°¹ÁÉ½Ñ•¥¸¥õœˆ°™½¹ÑM¥é”€ô€ÈÀ¹ÍÀ¤ìQ•áÐ ‹žv‡žrƒŽ½!•…±Ñ ½¹¹•ÓŽŽ
ÿŽÏŽGŽ
¿¢Î«Ž¿¦Ž’ê/¢¢c¦2ËŽ/Ž
'¢«–.W¦n¢¢#Ž_ŽûŽgŽˆ°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÄ¹ÍÀ¤ôô(€€€€€€€¥Ñ•´ìM•ÑÑ¥¹9Õµ‰•È ‹¦ÇŽ»žn»š¢g–n{šVÀˆ°½…°°€‹–nxˆ¤ì½…°€ô¥ÐìÙ´¹ÕÁ‘…Ñ•]½É­½ÕÑM•ÑÑ¥¹œ ‰Ý••­½…°ˆ°¥Ð¤ôô(€€€€€€€¥Ñ•´ìM•ÑÑ¥¹9Õµ‰•È ‹Ž
“ŽÏŽ
ÿŽóŽCŽ¯–"wšr–ˆ°É•ÍÐ°€‹žžHˆ¤ìÉ•ÍÐ€ô¥ÐìÙ´¹ÕÁ‘…Ñ•]½É­½ÕÑM•ÑÑ¥¹œ ‰É•ÍÑM•½¹‘Ìˆ°¥Ð¤ôô(€€€€€€€¥Ñ•´ìM•ÑÑ¥¹9Õµ‰•È ‹¦7¦?Ž»–"ïŽÿ¾ò#š^‹–ºk¾ò$ˆ°ÍÑ•À°€‰­œˆ¤ìÍÑ•À€ô¥ÐìÙ´¹ÕÁ‘…Ñ•]½É­½ÕÑM•ÑÑ¥¹œ ‰‘•™…Õ±ÑMÑ•Á-œˆ°¥Ð¤ôô(€€€€€€€¥Ñ•´ì1¥¹•	ÕÑÑ½¸ ‹š‚¦’+žn»š¢gŽ
KžÞ£¦nˆ°½¹½…±Ì°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ð ÔÈ¹‘À¤¤ô(€€€€€€€¥Ñ•´ì1¥¹•	ÕÑÑ½¸ ‰!•…±Ñ ½¹¹•Ó¢¢·–ºhˆ°½¹!•…±Ñ¡Õ¥‘”°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ð ÔÈ¹‘À¤¤ìQ•áÐ ‹’î[’êëŽ£Ž»š¾S¢òŽïŽ§ŽÏŽ
·ŽÏŽ
ÃŽ¿¢†3ŽŽûŽoŽ
OŽˆ°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÄ¹ÍÀ°µ½‘¥™¥•È€ô5½‘¥™¥•È¹Á…‘‘¥¹œ¡Ñ½À€ô€à¹‘À¤¤ô(€€€ô)ô()=ÁÑ%¸¡áÁ•É¥µ•¹Ñ…±5…Ñ•É¥…°ÍÁ¤èé±…ÍÌ¤)½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸9•Ýá•É¥Í•M¡••Ð¡Ù´è5…¥¹Y¥•Ý5½‘•°°½¹¥Íµ¥ÍÌè€ ¤€´øU¹¥Ð°½¹M…Ù•è€¡1½¹œ¤€´øU¹¥Ð¤ì(€€€Ù…È¹…µ”‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ô(€€€Ù…ÈÁ…ÉÐ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ‹¢àˆ¤ô(€€€Ù…ÈÕ¹¥Ð‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ‰­œˆ¤ô(€€€Ù…ÈÍÑ•À‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ È¸Ô¤ô(€€€Ù…È¹½Ñ”‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ô(€€€Ù…È•±…ÁÍ•‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•1½¹MÑ…Ñ•=˜ À¤ô(€€€1…Õ¹¡•‘™™•Ð¡U¹¥Ð¤ìÝ¡¥±”€¡ÑÉÕ”¤ì‘•±…ä Å|ÀÀÀ¤ì•±…ÁÍ•¬¬ôô(€€€5½‘…±	½ÑÑ½µM¡••Ð¡½¹¥Íµ¥ÍÍI•ÅÕ•ÍÐ€ô½¹¥Íµ¥ÍÌ°½¹Ñ…¥¹•É½±½È€ô½±½È ÁáÅÄàÄÔ¤¤ì(€€€€€€€½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Ù•ÉÑ¥…±MÉ½±°¡É•µ•µ‰•ÉMÉ½±±MÑ…Ñ” ¤¤¹Á…‘‘¥¹œ Äà¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä ÄÈ¹‘À¤¤ì(€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹MÁ…•	•ÑÝ••¸¤ìQ•áÐ ‹šZÃŽ_Žž¢»žn¸ˆ°™½¹ÑM¥é”€ô€Èà¹ÍÀ¤ìQ•áÐ ˆ‘í•±…ÁÍ•‘÷žžKžÖ3¦8ˆ°½±½È€ô•¹Ð¤ô(€€€€€€€€€€€Q•áÐ ‹–B7–&7Ž3–"Ž/Ž
'Ž«Ž?Ž™=/ŽŽŽ£ŽŸžnÓŽoŽûŽgŽˆ°½±½È€ô5ÕÑ•¤(€€€€€€€€€€€=ÕÑ±¥¹•‘Q•áÑ¥•±¡¹…µ”°ì¹…µ”€ô¥Ðô°Á±…•¡½±‘•È€ôìQ•áÐ ‹ž¢»žn»–B4ˆ¤ô°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤(€€€€€€€€€€€I½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä Ø¹‘À¤¤ì±¥ÍÑ=˜ ‹¢ãŽ»Ž{Ž
ßŽÍˆ°€‹¢3’â·Ž»Ž{Ž
ßŽÍˆ°€‹¢kŽ»Ž{Ž
ßŽÍˆ¤¹™½É… ì±…‰•°€´ø¥±Ñ•É¡¥À¡™…±Í”°ì¹…µ”€ô±…‰•°ìÁ…ÉÐ€ô±…‰•°¹Ñ…­•]¡¥±”ì¥Ð€„ô€ŸŽ¸œôô°ìQ•áÐ¡±…‰•°°™½¹ÑM¥é”€ô€ÄÀ¹ÍÀ¤ô¤ôô(€€€€€€€€€€€Q•áÐ ‹¦£’ö7Ž
ÿŽ
Àˆ°½±½È€ô5ÕÑ•¤(€€€€€€€€€€€I½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä Ô¹‘À¤¤ì±¥ÍÑ=˜ ‹¢àˆ°€‹¢3’â´ˆ°€‹¢hˆ°€‹¢
¤ˆ°€‹¢Tˆ°€‹’öO–æäˆ¤¹™½É… ì±…‰•°€´ø¥±Ñ•É¡¥À¡Á…ÉÐ€ôô±…‰•°°ìÁ…ÉÐ€ô±…‰•°ô°ìQ•áÐ¡±…‰•°¤ô¤ôô(€€€€€€€€€€€Q•áÐ ‹¢¢c¦2ËŽ
ÿŽ
“Ž\€¼ƒ¦7¦?Ž»–"ïŽüˆ°½±½È€ô5ÕÑ•¤(€€€€€€€€€€€I½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä Ô¹‘À¤¤ì(€€€€€€€€€€€€€€€±¥ÍÑ=˜ Ä¸À°€È¸Ô°€Ô¸À¤¹™½É… ìÙ…±Õ”€´ø¥±Ñ•É¡¥À¡Õ¹¥Ð€ôô€‰­œˆ€˜˜ÍÑ•À€ôôÙ…±Õ”°ìÕ¹¥Ð€ô€‰­œˆìÍÑ•À€ôÙ…±Õ”ô°ìQ•áÐ ˆ‘íáÁ½ÉÑ•È¹™µÐ¡Ù…±Õ”¥õ­œˆ¤ô¤ô(€€€€€€€€€€€€€€€¥±Ñ•É¡¥À¡Õ¹¥Ð€ôô€‰‰Üˆ°ìÕ¹¥Ð€ô€‰‰Üˆô°ìQ•áÐ ‹¢«¦4ˆ¤ô¤(€€€€€€€€€€€€€€€¥±Ñ•É¡¥À¡Õ¹¥Ð€ôô€‰Í•Œˆ°ìÕ¹¥Ð€ô€‰Í•Œˆô°ìQ•áÐ ‹žžKšVÀˆ¤ô¤(€€€€€€€€€€€ô(€€€€€€€€€€€=ÕÑ±¥¹•‘Q•áÑ¥•±¡¹½Ñ”°ì¹½Ñ”€ô¥Ðô°Á±…•¡½±‘•È€ôìQ•áÐ ‹Ž‡Ž‹¾ò#Ž
ßŽóŽ#’ö7žö»ŽŽSŽÏ’ö7žö»Ž«Ž§¾ò$ˆ¤ô°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ð äÀ¹‘À¤¤(€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹žfï¦2ËŽ_Ž›ŽŽgŽC¦Z/–ž,ˆ°ìÙ´¹…‘‘]½É­½ÕÑá•É¥Í”¡¹…µ”°Á…ÉÐ°Õ¹¥Ð°ÍÑ•À°¹½Ñ”°½¹M…Ù•¤ô°5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹¡•¥¡Ð ØÐ¹‘À¤¤(€€€€€€€€€€€Q•áÑ	ÕÑÑ½¸¡½¹±¥¬€ô½¹¥Íµ¥ÍÌ°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ìQ•áÐ ‹Ž
Ž
Ž
,ˆ¤ô(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸I•¹…µ•¥…±½œ¡•á•É¥Í”è]½É­½ÕÑá•É¥Í•¹Ñ¥Ñä°½¹¥Íµ¥ÍÌè€ ¤€´øU¹¥Ð°½¹M…Ù”è€¡MÑÉ¥¹œ¤€´øU¹¥Ð¤ì(€€€Ù…ÈÙ…±Õ”‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜¡•á•É¥Í”¹¹…µ”¤ô(€€€±•ÉÑ¥…±½œ¡½¹¥Íµ¥ÍÍI•ÅÕ•ÍÐ€ô½¹¥Íµ¥ÍÌ°Ñ¥Ñ±”€ôìQ•áÐ ‹–B7–&7Ž
K–’'šnÐˆ¤ô°Ñ•áÐ€ôì½±Õµ¸ìQ•áÐ ‹¦;–:ïŽ»¢¢c¦2ËŽ
ŽgŽçŽ›šZÃŽ_Ž–B7–&7Ž¯–òWŽ7žÚgŽ3Ž
3ŽûŽgŽˆ°½±½È€ô5ÕÑ•¤ì=ÕÑ±¥¹•‘Q•áÑ¥•±¡Ù…±Õ”°ìÙ…±Õ”€ô¥Ðô°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ôô°½¹™¥Éµ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ôì½¹M…Ù”¡Ù…±Õ”¤ô¤ìQ•áÐ ‹–’'šnÓŽgŽ
,ˆ¤ôô°‘¥Íµ¥ÍÍ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ô½¹¥Íµ¥ÍÌ¤ìQ•áÐ ‹Ž
·ŽŽÏŽ
ïŽ¬ˆ¤ôô¤)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸5•É•¥…±½œ¡Í½ÕÉ”è]½É­½ÕÑá•É¥Í•¹Ñ¥Ñä°Ñ…É•ÑÌè1¥ÍÐñ]½É­½ÕÑá•É¥Í•¹Ñ¥Ñäø°½¹¥Íµ¥ÍÌè€ ¤€´øU¹¥Ð°½¹5•É”è€¡1½¹œ¤€´øU¹¥Ð¤ì(€€€Ù…ÈÑ…É•Ð‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ñ1½¹œüø¡¹Õ±°¤ô(€€€±•ÉÑ¥…±½œ¡½¹¥Íµ¥ÍÍI•ÅÕ•ÍÐ€ô½¹¥Íµ¥ÍÌ°Ñ¥Ñ±”€ôìQ•áÐ ‹ž¢»žn»Ž
KžÖÇ–B ˆ¤ô°Ñ•áÐ€ôì½±Õµ¸ìQ•áÐ ˆ‘íÍ½ÕÉ”¹¹…µ•÷Ž
KŽ’â/Ž»Ž§Ž
3Ž/Ž¯–B#Ž
?ŽoŽûŽgŽˆ°½±½È€ô5ÕÑ•¤ìÑ…É•ÑÌ¹™½É… ì¥Ñ•´€´ø¥±Ñ•É¡¥À¡Ñ…É•Ð€ôô¥Ñ•´¹¥°ìÑ…É•Ð€ô¥Ñ•´¹¥ô°ìQ•áÐ ˆ‘í¥Ñ•´¹¹…µ•÷Žì‘í¥Ñ•´¹Á…ÉÑôˆ¤ô°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ôôô°½¹™¥Éµ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ôìÑ…É•Ðü¹±•Ð¡½¹5•É”¤ô°•¹…‰±•€ôÑ…É•Ð€„ô¹Õ±°¤ìQ•áÐ ‹žÖÇ–B#ŽgŽ
,ˆ¤ôô°‘¥Íµ¥ÍÍ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ô½¹¥Íµ¥ÍÌ¤ìQ•áÐ ‹Ž
·ŽŽÏŽ
ïŽ¬ˆ¤ôô¤)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸	½‘å5•ÑÉ¥¥…±½œ¡Ù´è5…¥¹Y¥•Ý5½‘•°°½¹¥Íµ¥ÍÌè€ ¤€´øU¹¥Ð¤ì(€€€Ù…ÈÝ•¥¡Ð‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ôìÙ…È™…Ð‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ôìÙ…È…É´‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ôìÙ…È¡•ÍÐ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ôìÙ…ÈÝ…¥ÍÐ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ôìÙ…ÈÑ¡¥ ‰äÉ•µ•µ‰•ÈìµÕÑ…‰±•MÑ…Ñ•=˜ ˆˆ¤ô(€€€±•ÉÑ¥…±½œ¡½¹¥Íµ¥ÍÍI•ÅÕ•ÍÐ€ô½¹¥Íµ¥ÍÌ°Ñ¥Ñ±”€ôìQ•áÐ ‹Ž/Ž
'ŽƒŽ»¢¢c¦2Èˆ¤ô°Ñ•áÐ€ôì½±Õµ¸¡5½‘¥™¥•È¹Ù•ÉÑ¥…±MÉ½±°¡É•µ•µ‰•ÉMÉ½±±MÑ…Ñ” ¤¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä Ø¹‘À¤¤ì±¥ÍÑ=˜ ‹’öO¦5­œˆÑ¼Ý•¥¡Ð°€‹’öO¢¢
«ž:”ˆÑ¼™…Ð°€‹¢U´ˆÑ¼…É´°€‹¢á´ˆÑ¼¡•ÍÐ°€‹¢ç–nÉ´ˆÑ¼Ý…¥ÍÐ°€‹–’«Ž
Ž
	´ˆÑ¼Ñ¡¥ ¤¹™½É…¡%¹‘•á•ì¥¹‘•à°Á…¥È€´ø=ÕÑ±¥¹•‘Q•áÑ¥•±¡Á…¥È¹Í•½¹°ìØ€´øÝ¡•¸¡¥¹‘•à¥ìÀ´ùÝ•¥¡ÐõØìÄ´ù™…ÐõØìÈ´ù…É´õØìÌ´ù¡•ÍÐõØìÐ´ùÝ…¥ÍÐõØí•±Í”´ùÑ¡¥ õÙôô°±…‰•°€ôìQ•áÐ¡Á…¥È¹™¥ÉÍÐ¤ô°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ôôô°½¹™¥Éµ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ôìÙ´¹Í…Ù•	½‘å5•ÑÉ¥Œ¡	½‘å5•ÑÉ¥¹Ñ¥Ñä¡1½…±…Ñ”¹¹½Ü ¤¹Ñ½MÑÉ¥¹œ ¤°Ý•¥¡Ð¹Ñ½½Õ‰±•=É9Õ±° ¤°™…Ð¹Ñ½½Õ‰±•=É9Õ±° ¤°…É´¹Ñ½½Õ‰±•=É9Õ±° ¤°¡•ÍÐ¹Ñ½½Õ‰±•=É9Õ±° ¤°Ý…¥ÍÐ¹Ñ½½Õ‰±•=É9Õ±° ¤°Ñ¡¥ ¹Ñ½½Õ‰±•=É9Õ±° ¤¤¤ì½¹¥Íµ¥ÍÌ ¤ô¤ìQ•áÐ ‹’þw–¶`ˆ¤ôô°‘¥Íµ¥ÍÍ	ÕÑÑ½¸€ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ô½¹¥Íµ¥ÍÌ¤ìQ•áÐ ‹Ž
·ŽŽÏŽ
ïŽ¬ˆ¤ôô¤)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸M•ÑQ…‰±”¡•á•É¥Í”è]½É­½ÕÑá•É¥Í•¹Ñ¥Ñä°Í•ÑÌè1¥ÍÐñ]½É­½ÕÑM•Ñ¹Ñ¥Ñäø°ÁÉ•Ù¥½ÕÌèá•É¥Í•1…ÍÑM•Ðü¤ì(€€€½±Õµ¸ì(€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Á…‘‘¥¹œ à¹‘À¤¤ìQ•áÐ ‰MPˆ°5½‘¥™¥•È¹Ý¥‘Ñ  ÐÐ¹‘À¤°½±½È€ô5ÕÑ•¤ìQ•áÐ ‹–&7–nxˆ°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°½±½È€ô5ÕÑ•¤ìQ•áÐ ‹’î+–nxˆ°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°½±½È€ô5ÕÑ•¤ìQ•áÐ ‰IAˆ°5½‘¥™¥•È¹Ý¥‘Ñ  ÐÔ¹‘À¤°½±½È€ô5ÕÑ•¤ô(€€€€€€€Í•ÑÌ¹™½É… ìÍ•Ð€´ø(€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹‰…­É½Õ¹¡¥˜€¡Í•Ð¹¥ÍAÈ¤•¹Ð¹½Áä¡…±Á¡„€ô€¸Àá˜¤•±Í”½±½È¹QÉ…¹ÍÁ…É•¹Ð¤¹Á…‘‘¥¹œ¡Ù•ÉÑ¥…°€ô€ÄÈ¹‘À°¡½É¥é½¹Ñ…°€ô€à¹‘À¤°Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä¤ì(€€€€€€€€€€€€€€€Q•áÐ¡Í•Ð¹Í•Ñ9¼¹Ñ½MÑÉ¥¹œ ¤°5½‘¥™¥•È¹Ý¥‘Ñ  ÐÐ¹‘À¤°½±½È€ô¥˜€¡Í•Ð¹¥ÍAÈ¤•¹Ð•±Í”%¹¬¤(€€€€€€€€€€€€€€€Q•áÐ¡ÁÉ•Ù¥½ÕÍQ•áÐ¡ÁÉ•Ù¥½ÕÌ°•á•É¥Í”¹Õ¹¥Ð¤°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÈ¹ÍÀ¤(€€€€€€€€€€€€€€€Q•áÐ¡Í•ÑQ•áÐ¡Í•Ð°•á•É¥Í”¹Õ¹¥Ð¤°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤°½±½È€ô¥˜€¡Í•Ð¹¥ÍAÈ¤•¹Ð•±Í”%¹¬¤(€€€€€€€€€€€€€€€Q•áÐ¡Í•Ð¹ÉÁ”ü¹Ñ½MÑÉ¥¹œ ¤€üè€‹ŠPˆ°5½‘¥™¥•È¹Ý¥‘Ñ  ÐÔ¹‘À¤°Ñ•áÑ±¥¸€ôQ•áÑ±¥¸¹¹°½±½È€ô5ÕÑ•¤(€€€€€€€€€€€ô(€€€€€€€€€€€!½É¥é½¹Ñ…±¥Ù¥‘•È¡½±½È€ôIÕ±”¤(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸MÑ•ÁÁ•ÉA…¹•°¡±…‰•°èMÑÉ¥¹œ°Ù…±Õ”èMÑÉ¥¹œ°¡•±ÀèMÑÉ¥¹œ°µ¥¹ÕÌè€ ¤€´øU¹¥Ð°Á±ÕÌè€ ¤€´øU¹¥Ð°µ½‘¥™¥•Èè5½‘¥™¥•È¤ì(€€€=ÕÑ±¥¹•‘…É¡µ½‘¥™¥•È°‰½É‘•È€ô	½É‘•ÉMÑÉ½­” Ä¹‘À°IÕ±”¤°½±½ÉÌ€ô…É‘•™…Õ±ÑÌ¹½ÕÑ±¥¹•‘…É‘½±½ÉÌ¡½¹Ñ…¥¹•É½±½È€ô½±½È¹QÉ…¹ÍÁ…É•¹Ð¤°Í¡…Á”€ô]½É­½ÕÑM¡…Á”¤ì(€€€€€€€½±Õµ¸¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Á…‘‘¥¹œ ÄÀ¹‘À¤°¡½É¥é½¹Ñ…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•É!½É¥é½¹Ñ…±±ä¤ì(€€€€€€€€€€€Q•áÐ¡±…‰•°°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÄ¹ÍÀ¤(€€€€€€€€€€€Q•áÐ¡Ù…±Õ”°™½¹ÑM¥é”€ô€ÔÈ¹ÍÀ°™½¹Ñ…µ¥±ä€ô…¹‘É½¥‘à¹½µÁ½Í”¹Õ¤¹Ñ•áÐ¹™½¹Ð¹½¹Ñ…µ¥±ä¹M•É¥˜¤(€€€€€€€€€€€Q•áÐ¡¡•±À°½±½È€ô5ÕÑ•°™½¹ÑM¥é”€ô€ÄÀ¹ÍÀ¤(€€€€€€€€€€€I½Ü¡5½‘¥™¥•È¹Á…‘‘¥¹œ¡Ñ½À€ô€ÄÀ¹‘À¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä à¹‘À¤¤ì(€€€€€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹Š"Hˆ°µ¥¹ÕÌ°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤¹¡•¥¡Ð ÔØ¹‘À¤¤(€€€€€€€€€€€€€€€1¥¹•	ÕÑÑ½¸ ‹¾ò,ˆ°Á±ÕÌ°5½‘¥™¥•È¹Ý•¥¡Ð Å˜¤¹¡•¥¡Ð ÔØ¹‘À¤¤(€€€€€€€€€€€ô(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸M•ÑÑ¥¹9Õµ‰•È¡Ñ¥Ñ±”èMÑÉ¥¹œ°Ù…±Õ”èMÑÉ¥¹œ°ÍÕ™™¥àèMÑÉ¥¹œ°½¹M…Ù”è€¡MÑÉ¥¹œ¤€´øU¹¥Ð¤ì(€€€	É…ÍÍ…ÉìQ•áÐ¡Ñ¥Ñ±”¤ìI½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹MÁ…•	•ÑÝ••¸°Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä¤ìQ•áÐ ˆ‘Ù…±Õ”‘ÍÕ™™¥àˆ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€ÈÐ¹ÍÀ¤ìI½ÜìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ôì½¹M…Ù”  ¡Ù…±Õ”¹Ñ½½Õ‰±•=É9Õ±° ¤€üè€Ä¸À¤€´¥˜€¡ÍÕ™™¥à€ôô€‰­œˆ¤€¸Ô•±Í”€Ä¸À¤¹½•É•Ñ1•…ÍÐ Ä¸À¤¹±•Ð¡áÁ½ÉÑ•Èèé™µÐ¤¤ô¤ìQ•áÐ ‹Š"Hˆ¤ôìQ•áÑ	ÕÑÑ½¸¡½¹±¥¬€ôì½¹M…Ù”  ¡Ù…±Õ”¹Ñ½½Õ‰±•=É9Õ±° ¤€üè€Ä¸À¤€¬¥˜€¡ÍÕ™™¥à€ôô€‰­œˆ¤€¸Ô•±Í”€Ä¸À¤¹±•Ð¡áÁ½ÉÑ•Èèé™µÐ¤¤ô¤ìQ•áÐ ‹¾ò,ˆ¤ôôôô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸á•É¥Í•MÑ…ÉÑ…É¡•á•É¥Í”è]½É­½ÕÑá•É¥Í•¹Ñ¥Ñä°±…ÍÐèá•É¥Í•1…ÍÑM•Ðü°½¹±¥¬è€ ¤€´øU¹¥Ð¤ì(€€€=ÕÑ±¥¹•‘…É¡µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹±¥­…‰±”¡½¹±¥¬€ô½¹±¥¬¤°‰½É‘•È€ô	½É‘•ÉMÑÉ½­” Ä¹‘À°IÕ±”¤°½±½ÉÌ€ô…É‘•™…Õ±ÑÌ¹½ÕÑ±¥¹•‘…É‘½±½ÉÌ¡½¹Ñ…¥¹•É½±½È€ô½±½È¹QÉ…¹ÍÁ…É•¹Ð¤°Í¡…Á”€ô]½É­½ÕÑM¡…Á”¤ì(€€€€€€€I½Ü¡5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¹Á…‘‘¥¹œ ÄÔ¹‘À¤°¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹MÁ…•	•ÑÝ••¸°Ù•ÉÑ¥…±±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•ÉY•ÉÑ¥…±±ä¤ì(€€€€€€€€€€€½±Õµ¸ìI½Ü¡¡½É¥é½¹Ñ…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä à¹‘À¤¤ìQ•áÐ¡•á•É¥Í”¹¹…µ”°™½¹ÑM¥é”€ô€ÄÜ¹ÍÀ¤ìQ•áÐ¡•á•É¥Í”¹Á…ÉÐ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€ÄÄ¹ÍÀ¤ôìQ•áÐ ‹–&7–nx€‘íÁÉ•Ù¥½ÕÍQ•áÐ¡±…ÍÐ°•á•É¥Í”¹Õ¹¥Ð¥ôˆ°½±½È€ô5ÕÑ•¤ô(€€€€€€€€€€€	½à¡5½‘¥™¥•È¹Í¥é” Ðà¹‘À¤¹‰…­É½Õ¹¡½±½È¹QÉ…¹ÍÁ…É•¹Ð°¥É±•M¡…Á”¤°½¹Ñ•¹Ñ±¥¹µ•¹Ð€ô±¥¹µ•¹Ð¹•¹Ñ•È¤ìQ•áÐ ‹ŠZØˆ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€Äà¹ÍÀ¤ô(€€€€€€€ô(€€€ô)ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸	É…ÍÍ…É¡½¹Ñ•¹Ðè½µÁ½Í…‰±”½±Õµ¹M½Á”¸ ¤€´øU¹¥Ð¤€ô=ÕÑ±¥¹•‘…É¡‰½É‘•È€ô	½É‘•ÉMÑÉ½­” Ä¹‘À°IÕ±”¤°½±½ÉÌ€ô…É‘•™…Õ±ÑÌ¹½ÕÑ±¥¹•‘…É‘½±½ÉÌ¡½¹Ñ…¥¹•É½±½È€ô½±½È¹QÉ…¹ÍÁ…É•¹Ð¤°Í¡…Á”€ô]½É­½ÕÑM¡…Á”°µ½‘¥™¥•È€ô5½‘¥™¥•È¹™¥±±5…á]¥‘Ñ  ¤¤ì½±Õµ¸¡5½‘¥™¥•È¹Á…‘‘¥¹œ ÄÐ¹‘À¤°Ù•ÉÑ¥…±ÉÉ…¹•µ•¹Ð€ôÉÉ…¹•µ•¹Ð¹ÍÁ…•‘	ä Ø¹‘À¤°½¹Ñ•¹Ð€ô½¹Ñ•¹Ð¤ô()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸]½É­½ÕÑM•Ñ¥½¹Q¥Ñ±”¡Ñ•áÐèMÑÉ¥¹œ¤€ôQ•áÐ¡Ñ•áÐ°½±½È€ô•¹Ð°™½¹ÑM¥é”€ô€ÄÈ¹ÍÀ°™½¹Ñ]•¥¡Ð€ô½¹Ñ]•¥¡Ð¹	½±¤()½µÁ½Í…‰±”)ÁÉ¥Ù…Ñ”™Õ¸Mµ…±±5½‘•	ÕÑÑ½¸¡Ñ•áÐèMÑÉ¥¹œ°Í•±•Ñ•è	½½±•…¸°½¹±¥¬è€ ¤€´øU¹¥Ð°µ½‘¥™¥•Èè5½‘¥™¥•È°•¹…‰±•è	½½±•…¸€ôÑÉÕ”¤€ô=ÕÑ±¥¹•‘	ÕÑÑ½¸¡½¹±¥¬€ô½¹±¥¬°µ½‘¥™¥•È€ôµ½‘¥™¥•È¹¡•¥¡Ð ÐÐ¹‘À¤°•¹…‰±•€ô•¹…‰±•°Í¡…Á”€ô]½É­½ÕÑM¡…Á”°‰½É‘•È€ô	½É‘•ÉMÑÉ½­” Ä¹‘À°¥˜€¡Í•±•Ñ•¤•¹Ð•±Í”IÕ±”¤°½±½ÉÌ€ô	ÕÑÑ½¹•™…Õ±ÑÌ¹½ÕÑ±¥¹•‘	ÕÑÑ½¹½±½ÉÌ¡½¹Ñ…¥¹•É½±½È€ô¥˜€¡Í•±•Ñ•¤•¹Ð¹½Áä¡…±Á¡„€ô€¸ÄÉ˜¤•±Í”½±½È¹QÉ…¹ÍÁ…É•¹Ð°½¹Ñ•¹Ñ½±½È€ô¥˜€¡Í•±•Ñ•¤•¹Ð•±Í”%¹¬¤¤ìQ•áÐ¡Ñ•áÐ¤ô()ÁÉ¥Ù…Ñ”™Õ¸ÁÉ•Ù¥½ÕÍQ•áÐ¡ÁÉ•Ù¥½ÕÌèá•É¥Í•1…ÍÑM•Ðü°Õ¹¥ÐèMÑÉ¥¹œ¤€ôÝ¡•¸€¡Õ¹¥Ð¤ì€‰­œˆ€´ø¥˜€¡ÁÉ•Ù¥½ÕÌü¹Ý•¥¡Ñ-œ€ôô¹Õ±°¤€‹Ž¿ŽcŽ
Ž›Ž»¢¢c¦2ËŽŸŽdˆ•±Í”€ˆ‘íáÁ½ÉÑ•È¹™µÐ¡ÁÉ•Ù¥½ÕÌ¹Ý•¥¡Ñ-œ¥õ­œƒ\€‘íÁÉ•Ù¥½ÕÌ¹É•ÁÌ€üè€‹ŠP‰ôˆì€‰Í•Œˆ€´øÁÉ•Ù¥½ÕÌü¹Í•½¹‘Ìü¹±•Ðì€ˆ‘í¥Ñ÷žžHˆô€üè€‹Ž¿ŽcŽ
Ž›Ž»¢¢c¦2ËŽŸŽdˆì•±Í”€´øÁÉ•Ù¥½ÕÌü¹É•ÁÌü¹±•Ðì€‹¢«¦4ƒ\€‘¥Ðˆô€üè€‹Ž¿ŽcŽ
Ž›Ž»¢¢c¦2ËŽŸŽdˆô)ÁÉ¥Ù…Ñ”™Õ¸ÁÉ•Ù¥½ÕÍY…±Õ”¡ÁÉ•Ù¥½ÕÌèá•É¥Í•1…ÍÑM•Ðü°Õ¹¥ÐèMÑÉ¥¹œ¤€ô¥˜€¡Õ¹¥Ð€ôô€‰Í•Œˆ¤ÁÉ•Ù¥½ÕÌü¹Í•½¹‘Ìü¹Ñ½MÑÉ¥¹œ ¤€üè€‹ŠPˆ•±Í”ÁÉ•Ù¥½ÕÌü¹É•ÁÌü¹Ñ½MÑÉ¥¹œ ¤€üè€‹ŠPˆ)ÁÉ¥Ù…Ñ”™Õ¸Í•ÑQ•áÐ¡Í•Ðè]½É­½ÕÑM•Ñ¹Ñ¥Ñä°Õ¹¥ÐèMÑÉ¥¹œ¤€ôÝ¡•¸€¡Õ¹¥Ð¤ì€‰­œˆ€´ø€ˆ‘íÍ•Ð¹Ý•¥¡Ñ-œü¹±•Ð¡áÁ½ÉÑ•Èèé™µÐ¤€üè€‹ŠP‰õ­œƒ\€‘íÍ•Ð¹É•ÁÌ€üè€‹ŠP‰ôˆì€‰Í•Œˆ€´ø€ˆ‘íÍ•Ð¹Í•½¹‘Ì€üè€‹ŠP‰÷žžHˆì•±Í”€´ø€‹¢«¦4ƒ\€‘íÍ•Ð¹É•ÁÌ€üè€‹ŠP‰ôˆô