package com.rojas.syncpass.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache + outbox. Server ids are reused as primary keys for catalogue data;
 * records created offline use auto-generated local ids and keep the server id when known.
 */

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val localPasswordHash: String? = null,
    val lastLoginAt: Long = 0L
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Long,
    val code: String,
    val name: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val responsible: String,
    val eventCount: Int,
    val createdAt: String
)

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["qrToken"], unique = true)
    ]
)
data class EventEntity(
    @PrimaryKey val id: Long,
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
    val averageRating: Double
)

@Entity(
    tableName = "enrollments",
    indices = [Index(value = ["eventId"]), Index(value = ["userId"])]
)
data class EnrollmentEntity(
    @PrimaryKey val id: Long,
    val eventId: Long,
    val userId: Long,
    val userName: String,
    val status: String,
    val createdAt: String
)

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["userId"]),
        Index(value = ["registeredByUserId"]),
        Index(value = ["clientId"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,
    val eventId: Long,
    val eventTitle: String,
    val userId: Long?,
    val participantName: String,
    val participantEmail: String?,
    val participantDocument: String?,
    val method: String,
    val status: String = "PRESENTE",
    val registeredAt: String,
    val registeredByName: String?,
    val clientId: String,
    val syncState: String = SyncState.PENDING.name,
    val error: String? = null,
    val registeredByUserId: Long? = null
)

@Entity(
    tableName = "surveys",
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["clientId"], unique = true),
        Index(value = ["attendanceClientId"], unique = true),
        Index(value = ["collectedByUserId"])
    ]
)
data class SurveyEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,
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
    val syncState: String = SyncState.PENDING.name,
    val error: String? = null,
    val attendanceClientId: String? = null,
    val attendanceServerId: Long? = null,
    val collectedByUserId: Long? = null,
    @ColumnInfo(defaultValue = "0") val redacted: Boolean = false
)

@Entity(tableName = "pending_operations", indices = [Index(value = ["clientId"], unique = true)])
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val type: String,
    val eventId: Long,
    val qrCode: String? = null,
    val participantName: String? = null,
    val participantEmail: String? = null,
    val participantDocument: String? = null,
    val rating: Int? = null,
    val organizationRating: Int? = null,
    val impactRating: Int? = null,
    val comment: String? = null,
    val wouldRecommend: Boolean? = null,
    val occurredAt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    val attendanceClientId: String? = null,
    val attendanceServerId: Long? = null
)

enum class SyncState { PENDING, SYNCED, ERROR }

object OperationType {
    const val ATTENDANCE_QR = "ATTENDANCE_QR"
    const val ATTENDANCE_WALK_IN = "ATTENDANCE_WALK_IN"
    const val SURVEY = "SURVEY"
}
