package com.king.luna.data.repo

import com.king.luna.data.db.DayLogDao
import com.king.luna.data.db.PeriodDayDao
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.cycle.CyclePredictor
import com.king.luna.domain.insight.InsightStats
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// 数据出入口；上层只与此交互
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dayLogDao: DayLogDao,
    private val clock: () -> LocalDate = LocalDate::now
) : com.king.luna.ui.screen.log.LogRepoPort {

    // 经期日列表（按日期升序）
    fun observePeriodDays(): Flow<List<LocalDate>> =
        periodDayDao.observeAll().map { list -> list.map { it.date } }

    // 有日志的日期集合（用于日历标点）
    fun observeLoggedDates(): Flow<Set<LocalDate>> =
        dayLogDao.observeDates().map { it.toSet() }

    fun observeDayLogs(): Flow<List<DayLogEntity>> =
        dayLogDao.observeAll()

    // 预测：拼装最新经期 + 当前 today；精确判断今天是否在经期
    fun observePrediction(): Flow<CyclePrediction> =
        periodDayDao.observeAll().map { list ->
            val today = clock()
            val todayIsPeriod = list.any { it.date == today }
            CyclePredictor.predict(list.map { it.date }, today, todayIsPeriod)
        }

    // 洞察统计（V2.1）
    fun observeInsight(): Flow<InsightStats> =
        periodDayDao.observeAll().map { InsightStats.from(it) }

    // 当日的所有展示信息（经期标记 + 日志 + 预测），合并供 UI 一次性渲染
    data class DayBundle(
        val date: LocalDate,
        val period: PeriodDayEntity?,
        val log: DayLogEntity?,
        val prediction: CyclePrediction
    )

    fun observeDayBundle(date: LocalDate): Flow<DayBundle> =
        combine(
            periodDayDao.observeAll(),
            dayLogDao.observeDates(),
            observePrediction()
        ) { periods, _, prediction ->
            DayBundle(
                date = date,
                period = periods.firstOrNull { it.date == date },
                log = dayLogDao.get(date),
                prediction = prediction
            )
        }

    override suspend fun getPeriodDay(date: LocalDate): PeriodDayEntity? = periodDayDao.get(date)

    override suspend fun getDayLog(date: LocalDate): DayLogEntity? = dayLogDao.get(date)

    // 写入经期：NONE 视为删除该日记录
    override suspend fun setPeriod(date: LocalDate, flow: FlowLevel) {
        if (flow == FlowLevel.NONE) {
            periodDayDao.delete(date)
        } else {
            periodDayDao.upsert(PeriodDayEntity(date, flow))
        }
    }

    // 写入日志：全空（无 mood/symptom/note）则删除
    override suspend fun setLog(date: LocalDate, moods: Set<Mood>, symptoms: Set<Symptom>, note: String) {
        val noteTrim = note.trim()
        if (moods.isEmpty() && symptoms.isEmpty() && noteTrim.isEmpty()) {
            dayLogDao.delete(date)
            return
        }
        dayLogDao.upsert(
            DayLogEntity(
                date = date,
                moods = moods.joinToString(",") { it.name },
                symptoms = symptoms.joinToString(",") { it.name },
                note = noteTrim
            )
        )
    }
}

// 辅助：CSV <-> 集合
fun String.toMoodSet(): Set<Mood> =
    if (isBlank()) emptySet()
    else split(",").mapNotNull { runCatching { Mood.valueOf(it.trim()) }.getOrNull() }.toSet()

fun String.toSymptomSet(): Set<Symptom> =
    if (isBlank()) emptySet()
    else split(",").mapNotNull { runCatching { Symptom.valueOf(it.trim()) }.getOrNull() }.toSet()
