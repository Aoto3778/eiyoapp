package jp.aoto.eiyoapp.domain

import jp.aoto.eiyoapp.data.DailyActivityEntity
import jp.aoto.eiyoapp.data.EntryWithFood
import jp.aoto.eiyoapp.data.GoalEntity
import jp.aoto.eiyoapp.data.Nutrients
import jp.aoto.eiyoapp.data.additivesFromJson
import jp.aoto.eiyoapp.data.localDate
import jp.aoto.eiyoapp.data.localTime
import jp.aoto.eiyoapp.data.nutrientSpecs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

data class ExportData(
    val entries: List<EntryWithFood>,
    val goals: List<GoalEntity>,
    val activities: List<DailyActivityEntity>,
)

object Exporter {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun markdown(from: LocalDate, to: LocalDate, data: ExportData): String = buildString {
        appendLine("# 栄養記録 ${from.format(dateFormat)} 〜 ${to.format(dateFormat)}")
        appendLine(); appendLine("## 目標（1日あたり）")
        if (data.goals.isEmpty()) appendLine("- 設定なし") else data.goals.forEach { goal ->
            val spec = nutrientSpecs.firstOrNull { it.key == goal.nutrientKey }
            appendLine("- ${spec?.label ?: goal.nutrientKey}: ${fmt(goal.target)} ${spec?.unit.orEmpty()}")
        }
        appendLine(); appendLine("## 日別サマリー")
        appendLine("| 日付 | ${nutrientSpecs.joinToString(" | ") { "${it.label}(${it.unit})" }} |")
        appendLine("|---|${nutrientSpecs.joinToString("") { "---|" }}")
        dates(from, to).forEach { date ->
            val total = sum(data.entries.filter { it.timestamp.localDate() == date })
            appendLine("| $date | ${nutrientSpecs.joinToString(" | ") { fmt(total.value(it.key)) }} |")
        }
        appendLine(); appendLine("## Garmin 活動データ")
        appendLine("| 日付 | 消費kcal | 歩数 | 運動分 | 睡眠 | 安静時心拍 |")
        appendLine("|---|---:|---:|---:|---:|---:|")
        dates(from, to).forEach { date ->
            val a = data.activities.firstOrNull { it.date == date.toString() }
            appendLine("| $date | ${a?.totalCaloriesKcal?.let(::fmt) ?: "—"} | ${a?.steps ?: "—"} | ${a?.exerciseMinutes ?: "—"} | ${a?.sleepMinutes?.let(::minutes) ?: "—"} | ${a?.restingHr ?: "—"} |")
        }
        appendLine(); appendLine("## 食事ログ")
        dates(from, to).forEach { date ->
            appendLine(); appendLine("### $date")
            val day = data.entries.filter { it.timestamp.localDate() == date }
            if (day.isEmpty()) appendLine("- 記録なし") else day.forEach { e ->
                appendLine("- ${e.timestamp.localTime().format(timeFormat)} ${timeBand(e.timestamp.localTime().hour)}：${e.name} ×${fmt(e.amount)}${e.unit}（${fmt(e.nutrients().kcal)} kcal / P ${fmt(e.nutrients().protein)} g）")
            }
        }
        appendLine(); appendLine("## 添加物の摂取回数")
        val additives = data.entries.flatMap { additivesFromJson(it.additivesJson) }.groupingBy { it }.eachCount()
        if (additives.isEmpty()) appendLine("- なし") else additives.toSortedMap().forEach { (name, count) -> appendLine("- $name: ${count}回") }
    }

    fun csv(data: ExportData): ByteArray {
        val header = listOf("日付","時刻","時間帯","名称","量","単位") + nutrientSpecs.map { "${it.label}(${it.unit})" } + "添加物"
        val lines = mutableListOf(header.joinToString(",", transform = ::csvCell))
        data.entries.forEach { e ->
            val n = e.nutrients()
            val row = listOf(e.timestamp.localDate().toString(), e.timestamp.localTime().format(timeFormat), timeBand(e.timestamp.localTime().hour), e.name, fmt(e.amount), e.unit) + nutrientSpecs.map { fmt(n.value(it.key)) } + additivesFromJson(e.additivesJson).joinToString("、")
            lines += row.joinToString(",", transform = ::csvCell)
        }
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + lines.joinToString("\r\n", postfix="\r\n").toByteArray(Charsets.UTF_8)
    }

    fun sum(entries: List<EntryWithFood>) = entries.fold(Nutrients()) { total, entry -> total + entry.nutrients() }
    fun timeBand(hour: Int) = when { hour < 5 -> "深夜"; hour < 10 -> "朝"; hour < 15 -> "昼"; hour < 18 -> "間食"; hour < 23 -> "夜"; else -> "深夜" }
    fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.roundToLong().toString() else "%.1f".format(value)
    private fun minutes(value: Int) = "%d:%02d".format(value / 60, value % 60)
    private fun dates(from: LocalDate, to: LocalDate) = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
    private fun csvCell(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
