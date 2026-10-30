package com.rojas.syncpass.data.remote

data class ProjectDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val responsible: String,
    val eventCount: Int,
    val createdAt: String?
)

data class ProjectRequest(
    val code: String,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val responsible: String
)

data class EventDto(
    val id: Long,
    val projectId: Long,
    val projectName: String,
    val title: String,
    val description: String,
    val location: String,
    val startsAt: String,
    val endsAt: String,
    val capacity: Int,
    val qrToken: String,
    val status: String,
    val enrolledCount: Int,
    val attendedCount: Int,
    val averageRating: Double?
)

data class EventRequest(
    val projectId: Long,
    val title: String,
    val description: String,
    val location: String,
    val startsAt: String,
    val endsAt: String,
    val capacity: Int,
    val qrToken: String?,
    val status: String
)

data class EnrollmentDto(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val userName: String,
    val status: String,
    val createdAt: String?
)

data class ParticipantDto(
    val userId: Long?,
    val fullName: String,
    val username: String?,
    val email: String?,
    val enrolled: Boolean,
    val attended: Boolean,
    val method: String?,
    val registeredAt: String?
)

data class EventDetailDto(
    val event: EventDto,
    val participants: List<ParticipantDto>,
    val surveySummary: SurveySummaryDto? = null,
    val enrolled: Boolean,
    val attended: Boolean,
    val surveyCompleted: Boolean
)

data class ScanRequest(val qrCode: String)

data class WalkInRequest(
    val participantName: String,
    val participantEmail: String?,
    val participantDocument: String?
)

data class AttendanceDto(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val userId: Long?,
    val participantName: String,
    val participantEmail: String?,
    val participantDocument: String?,
    val method: String,
    val status: String,
    val registeredAt: String,
    val registeredByName: String?,
    val clientId: String,
    val registeredByUserId: Long? = null
)

data class SurveyRequest(
    val rating: Int,
    val organizationRating: Int,
    val impactRating: Int,
    val comment: String?,
    val wouldRecommend: Boolean
)

data class SurveyDto(
    val id: Long,
    val eventId: Long,
    val userId: Long?,
    val participantName: String,
    val rating: Int,
    val organizationRating: Int,
    val impactRating: Int,
    val comment: String?,
    val wouldRecommend: Boolean,
    val submittedAt: String,
    val clientId: String,
    val attendanceId: Long? = null,
    val attendanceClientId: String? = null,
    val collectedByUserId: Long? = null,
    val collectedByName: String? = null
)

data class SurveyReceiptDto(
    val id: Long,
    val eventId: Long,
    val attendanceId: Long?,
    val attendanceClientId: String?,
    val submittedAt: String
)

data class SurveySummaryDto(
    val eventId: Long,
    val responseCount: Long,
    val averageRating: Double,
    val averageOrganization: Double,
    val averageImpact: Double,
    val recommendRate: Double,
    val comments: List<String>
)

data class EventStatDto(
    val eventId: Long,
    val title: String,
    val projectName: String,
    val startsAt: String,
    val enrolled: Long,
    val attended: Long,
    val attendanceRate: Double,
    val averageRating: Double
)

data class ImpactReportDto(
    val generatedAt: String,
    val totalProjects: Long,
    val totalEvents: Long,
    val totalEnrollments: Long,
    val totalAttendances: Long,
    val uniqueParticipants: Long,
    val attendanceRate: Double,
    val averageSatisfaction: Double,
    val recommendRate: Double,
    val events: List<EventStatDto>
)

data class SyncOperationDto(
    val clientId: String,
    val type: String,
    val eventId: Long,
    val attendanceClientId: String? = null,
    val attendanceServerId: Long? = null,
    val qrCode: String? = null,
    val participantName: String? = null,
    val participantEmail: String? = null,
    val participantDocument: String? = null,
    val rating: Int? = null,
    val organizationRating: Int? = null,
    val impactRating: Int? = null,
    val comment: String? = null,
    val wouldRecommend: Boolean? = null,
    val occurredAt: String? = null
)

data class SyncBatchRequest(val operations: List<SyncOperationDto>)

data class SyncResultDto(
    val clientId: String,
    val status: String,
    val message: String?,
    val serverId: Long?
)

data class SyncBatchResponse(
    val serverTime: String,
    val results: List<SyncResultDto>
)

data class SyncPullDto(
    val serverTime: String,
    val projects: List<ProjectDto>,
    val events: List<EventDto>,
    val enrollments: List<EnrollmentDto>,
    val attendances: List<AttendanceDto>,
    val surveys: List<SurveyDto>,
    val surveyReceipts: List<SurveyReceiptDto>? = emptyList()
)
