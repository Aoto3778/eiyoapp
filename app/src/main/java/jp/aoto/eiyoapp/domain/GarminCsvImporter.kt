package jp.aoto.eiyoapp.domain

import jp.aoto.eiyoapp.data.DailyActivityEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object GarminCsvImporter {
    private val dateNames = setOf("date", "日付", "calendar date")
    private val totalNames = setOf("total calories", "calories", "総消費カロリー", "消費カロリー", "total calories (kcal)")
    private val activeNames = setOf("active calories", "アクティブカロリー", "活動カロリー")
    private val stepNames = setOf("steps", "歩数")
    private val exerciseNames = setOf("exercise minutes", "intensity minutes", "運動時間", "運動分", "強度（分）")
    private val sleepNames = setOf("sleep minutes", "sleep duration", "睡眠時間", "睡眠分")
    private val heartNames = setOf("resting heart rate", "resting hr", "安静時心拍", "安静時心拍数")

    fun parse(text: String): List<DailyActivityEntity> {
        val lines = text.removePrefix("\uFEFF").lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size >= 2) { "データ行がありません" }
        val header = csvLine(lines.first()).map { it.trim().lowercase(Locale.ROOT) }
        fun index(names: Set<String>) = header.indexOfFirst { it in names }
        val dateIndex = index(dateNames)
        require(dateIndex >= 0) { "日付列を認識できません（対応: Date / 日付）" }
        val known = listOf(totalNames, activeNames, stepNames, exerciseNames, sleepNames, heartNames).map(::index)
        require(known.any { it >= 0 }) { "活動量の列を認識できません" }
        return lines.drop(1).mapNotNull { line ->
            val cells = csvLine(line)
            val date = cells.getOrNull(dateIndex)?.let(::parseDate) ?: return@mapNotNull null
            fun numberAt(i: Int) = cells.getOrNull(i)?.replace(",", "")?.replace("kcal", "", true)?.trim()?.toDoubleOrNull()
            DailyActivityEntity(
                date=date.toString(), totalCaloriesKcal=numberAt(known[0]), activeCaloriesKcal=numberAt(known[1]),
                steps=numberAt(known[2])?.toLong(), exerciseMinutes=numberAt(known[3])?.toInt(),
                sleepMinutes=numberAt(known[4])?.let { if(it < 24) (it*60).toInt() else it.toInt() },
                restingHr=numberAt(known[5])?.toInt(),
            )
        }
    }

    private fun parseDate(raw: String): LocalDate? {
        val value = raw.trim()
        val formats = listOf(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("M/d/yyyy"), DateTimeFormatter.ofPattern("yyyy/M/d"), DateTimeFormatter.ofPattern("M/d/yy"))
        return formats.firstNotNullOfOrNull { runCatching { LocalDate.parse(value, it) }.getOrNull() }
    }

    internal fun csvLine(line: String): List<String> {
        val result = mutableListOf<String>(); val cell = StringBuilder(); var quoted=false; var i=0
        while(i<line.length) {
            val c=line[i]
            when {
                c=='"' && quoted && i+1<line.length && line[i+1]=='"' -> { cell.append('"'); i++ }
                c=='"' -> quoted=!quoted
                c==',' && !quoted -> { result += cell.toString(); cell.clear() }
                else -> cell.append(c)
            }; i++
        }
        result += cell.toString(); return result
    }
}
