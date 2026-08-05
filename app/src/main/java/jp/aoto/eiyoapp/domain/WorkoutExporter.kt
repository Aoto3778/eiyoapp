package jp.aoto.eiyoapp.domain

import jp.aoto.eiyoapp.data.WorkoutExportData
import jp.aoto.eiyoapp.data.WorkoutSetWithExercise
import jp.aoto.eiyoapp.data.localDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WorkoutExporter {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun markdown(
        from: LocalDate,
        to: LocalDate,
        workout: WorkoutExportData,
        nutrition: ExportData,
    ): String = buildString {
        val completed = workout.sessions.filter { it.completed }
        val totalVolume = workout.sets.sumOf { WorkoutMath.volume(it.weightKg, it.reps) }
        appendLine("# 筋トレ記録 — ${from.format(dateFormat)} 〜 ${to.format(dateFormat)}")
        appendLine("- 実施 ${completed.size}回 / 総ボリューム ${Exporter.fmt(totalVolume)} kg")
        appendLine("- 食事・睡眠は統合栄養記録から自動集計")

        completed.forEach { session ->
            val sessionSets = workout.sets.filter { it.sessionId == session.id }
            val duration = ((session.endedAt ?: session.startedAt) - session.startedAt).coerceAtLeast(0) / 60_000
            val date = LocalDate.parse(session.date)
            val protein = Exporter.sum(nutrition.entries.filter { it.timestamp.localDate() == date }).protein
            val sleep = nutrition.activities.firstOrNull { it.date == session.date }?.sleepMinutes
            appendLine(); appendLine("## ${session.date} (${duration}分)")
            val sleepText = sleep?.let { "${it / 60.0}h" } ?: "—"
            val conditionText = session.conditionNote.ifBlank { "—" }
            appendLine("睡眠 $sleepText / タンパク質 ${Exporter.fmt(protein)}g / 体調: $conditionText")
            sessionSets.groupBy { it.exerciseId }.forEach { (_, sets) ->
                val first = sets.first()
                val type = if (first.unit == "kg") "マシン / 刻み ${Exporter.fmt(first.stepKg)}kg" else if (first.unit == "sec") "自重 / 秒数" else "自重 / 重量なし"
                appendLine(); appendLine("### ${first.exerciseName} [${first.part}] $type")
                if (first.unit == "kg") {
                    appendLine("| set | 重量kg | 回数 | RPE | 前回 | 差分 |")
                    appendLine("|----:|------:|----:|:---:|:-----|:-----|")
                } else if (first.unit == "sec") {
                    appendLine("| set | 秒 | RPE | 前回 |")
                    appendLine("|----:|---:|:---:|:-----|")
                } else {
                    appendLine("| set | 回数 | RPE | 前回 |")
                    appendLine("|----:|----:|:---:|:-----|")
                }
                sets.sortedBy { it.setNo }.forEach { set ->
                    val previous = previousOf(workout.sets, set)
                    when (first.unit) {
                        "kg" -> {
                            val delta = WorkoutMath.volume(set.weightKg, set.reps) - WorkoutMath.volume(previous?.weightKg, previous?.reps)
                            appendLine("| ${set.setNo} | ${fmt(set.weightKg)} | ${display(set.reps)} | ${display(set.rpe)} | ${fmtPair(previous)} | ${signed(delta)}kg |")
                        }
                        "sec" -> appendLine("| ${set.setNo} | ${display(set.seconds)} | ${display(set.rpe)} | ${display(previous?.seconds)} |")
                        else -> appendLine("| ${set.setNo} | ${display(set.reps)} | ${display(set.rpe)} | ${display(previous?.reps)} |")
                    }
                }
                if (first.unit == "kg") {
                    val oneRm = sets.mapNotNull { WorkoutMath.estimatedOneRepMax(it.weightKg, it.reps) }.maxOrNull()
                    appendLine("推定1RM: ${oneRm?.let(Exporter::fmt) ?: "—"}kg / ボリューム ${Exporter.fmt(sets.sumOf { WorkoutMath.volume(it.weightKg, it.reps) })}kg")
                }
            }
        }

        val parts = workout.sets.groupingBy { it.part }.eachCount()
        appendLine(); appendLine("## 分析してほしいこと")
        appendLine("- 部位別セット数の偏り（${parts.entries.joinToString("・") { "${it.key}${it.value}" }}）")
        appendLine("- 睡眠・タンパク質と前回超えの関係")
        appendLine("- 次の2週間の重量と回数の伸ばし方")
    }

    fun csv(workout: WorkoutExportData, nutrition: ExportData): ByteArray {
        val header = "date,exercise,part,type,step_kg,set_no,weight_kg,reps,seconds,rpe,volume_kg,prev_weight_kg,prev_reps,sleep_h,protein_g,note"
        val lines = mutableListOf(header)
        workout.sets.forEach { set ->
            val session = workout.sessions.firstOrNull { it.id == set.sessionId }
            val date = session?.date.orEmpty()
            val previous = previousOf(workout.sets, set)
            val protein = nutrition.entries.filter { it.timestamp.localDate().toString() == date }
                .let(Exporter::sum).protein
            val sleep = nutrition.activities.firstOrNull { it.date == date }?.sleepMinutes?.div(60.0)
            val row = listOf(
                date, set.exerciseName, set.part, if (set.unit == "kg") "machine" else "bodyweight",
                set.stepKg.takeIf { set.unit == "kg" }?.let(Exporter::fmt).orEmpty(), set.setNo.toString(),
                set.weightKg?.let(Exporter::fmt).orEmpty(), set.reps?.toString().orEmpty(), set.seconds?.toString().orEmpty(),
                set.rpe?.toString().orEmpty(), set.weightKg?.let { Exporter.fmt(WorkoutMath.volume(it, set.reps)) }.orEmpty(),
                previous?.weightKg?.let(Exporter::fmt).orEmpty(), previous?.reps?.toString().orEmpty(),
                sleep?.let(Exporter::fmt).orEmpty(), Exporter.fmt(protein), session?.conditionNote.orEmpty(),
            )
            lines += row.joinToString(",") { csvCell(it) }
        }
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            lines.joinToString("\r\n", postfix = "\r\n").toByteArray(Charsets.UTF_8)
    }

    private fun previousOf(all: List<WorkoutSetWithExercise>, current: WorkoutSetWithExercise): WorkoutSetWithExercise? =
        all.asSequence().filter {
            it.exerciseId == current.exerciseId && it.sessionId != current.sessionId &&
                it.recordedAt < current.recordedAt && it.setNo == current.setNo
        }.maxByOrNull { it.recordedAt }
            ?: all.asSequence().filter {
                it.exerciseId == current.exerciseId && it.sessionId != current.sessionId && it.recordedAt < current.recordedAt
            }.maxByOrNull { it.recordedAt }

    private fun fmt(value: Double?) = value?.let(Exporter::fmt) ?: "—"
    private fun fmtPair(set: WorkoutSetWithExercise?) = if (set == null) "—" else "${fmt(set.weightKg)}×${display(set.reps)}"
    private fun display(value: Any?) = value?.toString() ?: "—"
    private fun signed(value: Double) = when {
        value > 0 -> "+${Exporter.fmt(value)}"
        value < 0 -> Exporter.fmt(value)
        else -> "±0"
    }
    private fun csvCell(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
