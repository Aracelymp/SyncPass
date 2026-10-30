package com.rojas.syncpass.data.local

import com.rojas.syncpass.data.remote.AttendanceDto
import com.rojas.syncpass.data.remote.EnrollmentDto
import com.rojas.syncpass.data.remote.EventDto
import com.rojas.syncpass.data.remote.ProjectDto
import com.rojas.syncpass.data.remote.SurveyDto
import com.rojas.syncpass.data.remote.UserDto
import com.rojas.syncpass.core.util.Hash

fun UserDto.toEntity(
    password: String? = null,
    loginAt: Long = 0L,
    existing: UserEntity? = null
) = UserEntity(
    id = id,
    username = username,
    email = email,
    fullName = fullName,
    role = role,
    // Password login recalculates the offline credential. Cognito login has no
    // password to derive it from, so it keeps the credential of the same local user.
    localPasswordHash = password?.let { Hash.localPassword(username, it) }
        ?: existing?.takeIf { it.id == id }?.localPasswordHash,
    lastLoginAt = loginAt
)

fun ProjectDto.toEntity() = ProjectEntity(
    id = id,
    code = code,
    name = name,
    description = description,
    startDate = startDate,
    endDate = endDate,
    status = status,
    responsible = responsible,
    eventCount = eventCount,
    createdAt = createdAt ?: ""
)

fun EventDto.toEntity() = EventEntity(
    id = id,
    projectId = projectId,
    projectName = projectName,
    title = title,
    description = description,
    location = location,
    startsAt = startsAt,
    endsAt = endsAt,
    capacity = capacity,
    qrToken = qrToken,
    status = status,
    enrolledCount = enrolledCount,
    attendedCount = attendedCount,
    averageRating = averageRating ?: 0.0
)

fun EnrollmentDto.toEntity() = EnrollmentEntity(
    id = id,
    eventId = eventId,
    userId = userId,
    userName = userName,
    status = status,
    createdAt = createdAt ?: ""
)

fun AttendanceDto.toEntity(existingLocalId: Long = 0L) = AttendanceEntity(
    localId = existingLocalId,
    serverId = id,
    eventId = eventId,
    eventTitle = eventTitle,
    userId = userId,
    participantName = participantName,
    participantEmail = participantEmail,
    participantDocument = participantDocument,
    method = method,
    status = status,
    registeredAt = registeredAt,
    registeredByName = registeredByName,
    clientId = clientId,
    syncState = SyncState.SYNCED.name,
    error = null,
    registeredByUserId = registeredByUserId
)

fun SurveyDto.toEntity(existingLocalId: Long = 0L) = SurveyEntity(
    localId = existingLocalId,
    serverId = id,
    eventId = eventId,
    userId = userId,
    participantName = participantName,
    rating = rating,
    organizationRating = organizationRating,
    impactRating = impactRating,
    comment = comment,
    wouldRecommend = wouldRecommend,
    submittedAt = submittedAt,
    clientId = clientId,
    syncState = SyncState.SYNCED.name,
    error = null,
    attendanceClientId = attendanceClientId,
    attendanceServerId = attendanceId,
    collectedByUserId = collectedByUserId,
    redacted = false
)
