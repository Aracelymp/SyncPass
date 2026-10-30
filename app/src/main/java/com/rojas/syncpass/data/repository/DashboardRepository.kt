package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.local.AttendanceDao
import com.rojas.syncpass.data.local.EventDao
import com.rojas.syncpass.data.local.ProjectDao
import com.rojas.syncpass.data.local.SurveyDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

data class DashboardStats(
    val projectCount: Int,
    val eventCount: Int,
    val attendanceCount: Int,
    val pendingCount: Int,
    val upcomingEvents: List<EventEntity>,
    val todayEvents: List<EventEntity>
)

class DashboardRepository(
    projectDao: ProjectDao,
    eventDao: EventDao,
    attendanceDao: AttendanceDao,
    pendingDao: com.rojas.syncpass.data.local.PendingOperationDao
) {
    val stats: Flow<DashboardStats> = combine(
        projectDao.observeCount(),
        eventDao.observeAll(),
        attendanceDao.observeAll()
    ) { projects, events, attendance ->
        Triple(projects, events, attendance)
    }.combine(pendingDao.observeCount()) { (projects, events, attendance), pending ->
        DashboardStats(
            projectCount = projects,
            eventCount = events.size,
            attendanceCount = attendance.size,
            pendingCount = pending,
            upcomingEvents = events.filter { Dates.isUpcoming(it.startsAt) }
                .sortedBy { it.startsAt }
                .take(4),
            todayEvents = events.filter { Dates.isToday(it.startsAt) }
        )
    }

    val projects: Flow<List<ProjectEntity>> = projectDao.observeAll()
}

enum class ReportPeriod { ALL, THIS_MONTH, LAST_30_DAYS }

data class EventImpact(
    val eventId: Long,
    val title: String,
    val projectName: String,
    val startsAt: String,
    val enrolled: Int,
    val attended: Int,
    val attendanceRate: Double,
    val averageRating: Double
)

data class ImpactMetrics(
    val totalProjects: Int,
    val totalEvents: Int,
    val totalEnrollments: Int,
    val totalAttendances: Int,
    val uniqueParticipants: Int,
    val attendanceRate: Double,
    val averageSatisfaction: Double,
    val recommendRate: Double,
    val events: List<EventImpact>
)

class ReportRepository(
    private val projectDao: ProjectDao,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val surveyDao: SurveyDao
) {
    fun observe(period: ReportPeriod): Flow<ImpactMetrics> = combine(
        projectDao.observeAll(),
        eventDao.observeAll(),
        attendanceDao.observeAll(),
        surveyDao.observeAll()
    ) { projects: List<ProjectEntity>, events: List<EventEntity>, attendance: List<AttendanceEntity>, surveys: List<SurveyEntity> ->
        val now = LocalDateTime.now()
        val filteredEvents = events.filter { event ->
            when (period) {
                ReportPeriod.ALL -> true
                ReportPeriod.THIS_MONTH -> {
                    val date = Dates.parseDateTime(event.startsAt)
                    date != null && date.year == now.year && date.month == now.month
                }
                ReportPeriod.LAST_30_DAYS -> {
                    val date = Dates.parseDateTime(event.startsAt)
                    date != null && date.isAfter(now.minusDays(30))
                }
            }
        }
        val filteredAttendance = attendance.filter { row -> filteredEvents.any { it.id == row.eventId } }
        val filteredSurveys = surveys.filter { row -> filteredEvents.any { it.id == row.eventId } }

        val eventImpacts = filteredEvents.map { event ->
            val attended = filteredAttendance.count { it.eventId == event.id }
            val enrolled = event.enrolledCount
            val rate = if (enrolled == 0) {
                if (attended > 0) 100.0 else 0.0
            } else {
                ((attended * 100.0) / enrolled).coerceAtMost(100.0)
            }
            val ratings = filteredSurveys.filter { it.eventId == event.id }
            EventImpact(
                eventId = event.id,
                title = event.title,
                projectName = event.projectName,
                startsAt = event.startsAt,
                enrolled = enrolled,
                attended = attended,
                attendanceRate = rate,
                averageRating = if (ratings.isEmpty()) 0.0 else ratings.map { it.rating }.average()
            )
        }.sortedByDescending { it.startsAt }

        val totalEnrollments = filteredEvents.sumOf { it.enrolledCount }
        val unique = filteredAttendance.map {
            it.userId?.let { id -> "u:$id" } ?: "g:${it.participantEmail ?: it.participantName}"
        }.toSet()
        val satisfaction = filteredSurveys.map { it.rating }
        val recommend = filteredSurveys.count { it.wouldRecommend }

        ImpactMetrics(
            totalProjects = if (period == ReportPeriod.ALL) projects.size
            else projects.count { project -> filteredEvents.any { it.projectId == project.id } },
            totalEvents = filteredEvents.size,
            totalEnrollments = totalEnrollments,
            totalAttendances = filteredAttendance.size,
            uniqueParticipants = unique.size,
            attendanceRate = if (totalEnrollments == 0) 0.0
            else (filteredAttendance.size * 100.0 / totalEnrollments).coerceAtMost(100.0),
            averageSatisfaction = if (satisfaction.isEmpty()) 0.0 else satisfaction.average(),
            recommendRate = if (filteredSurveys.isEmpty()) 0.0
            else recommend * 100.0 / filteredSurveys.size,
            events = eventImpacts
        )
    }
}
