package com.rojas.syncpass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE OR email = :username COLLATE NOCASE LIMIT 1")
    suspend fun findByUsernameOrEmail(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("DELETE FROM users WHERE username = :username COLLATE NOCASE")
    suspend fun deleteByUsername(username: String)

    @Query("UPDATE users SET localPasswordHash = :hash, lastLoginAt = :at WHERE id = :id")
    suspend fun updateCredential(id: Long, hash: String, at: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY startDate DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProjectEntity?

    @Upsert
    suspend fun upsertAll(projects: List<ProjectEntity>)

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    fun observeCount(): Flow<Int>
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE projectId = :projectId ORDER BY startsAt ASC")
    fun observeByProject(projectId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): EventEntity?

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM events")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM events ORDER BY startsAt ASC")
    suspend fun getAll(): List<EventEntity>

    @Query("DELETE FROM events")
    suspend fun clear()
}

@Dao
interface EnrollmentDao {
    @Query("SELECT * FROM enrollments WHERE eventId = :eventId ORDER BY userName ASC")
    fun observeByEvent(eventId: Long): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE eventId = :eventId AND userId = :userId AND status = 'INSCRITO' LIMIT 1")
    fun observeEnrolled(eventId: Long, userId: Long): Flow<EnrollmentEntity?>

    @Query("SELECT * FROM enrollments WHERE eventId = :eventId AND userId = :userId AND status = 'INSCRITO' LIMIT 1")
    suspend fun findEnrolled(eventId: Long, userId: Long): EnrollmentEntity?

    @Upsert
    suspend fun upsertAll(enrollments: List<EnrollmentEntity>)

    @Upsert
    suspend fun upsert(enrollment: EnrollmentEntity)

    @Query("SELECT COUNT(*) FROM enrollments WHERE eventId = :eventId AND status = 'INSCRITO'")
    suspend fun countEnrolled(eventId: Long): Int

    @Query("DELETE FROM enrollments")
    suspend fun clear()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE eventId = :eventId ORDER BY registeredAt DESC")
    fun observeByEvent(eventId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE userId = :userId ORDER BY registeredAt DESC")
    fun observeByUser(userId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY registeredAt DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    suspend fun findForUser(eventId: Long, userId: Long): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE clientId = :clientId LIMIT 1")
    suspend fun findByClientId(clientId: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE clientId = :clientId LIMIT 1")
    fun observeByClientId(clientId: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM attendance WHERE serverId = :serverId LIMIT 1")
    suspend fun findByServerId(serverId: Long): AttendanceEntity?

    @Query("SELECT COUNT(*) FROM attendance")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE syncState = 'PENDING' OR syncState = 'ERROR'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM attendance WHERE syncState = 'PENDING' OR syncState = 'ERROR'")
    suspend fun getPending(): List<AttendanceEntity>

    @Upsert
    suspend fun upsert(attendance: AttendanceEntity)

    @Upsert
    suspend fun upsertAll(attendance: List<AttendanceEntity>)

    @Query("UPDATE attendance SET syncState = :state, serverId = COALESCE(:serverId, serverId), error = :error WHERE clientId = :clientId")
    suspend fun updateSyncState(clientId: String, state: String, serverId: Long?, error: String?)

    @Query("UPDATE attendance SET syncState = :state, serverId = COALESCE(:serverId, serverId), error = :error WHERE clientId = :clientId")
    suspend fun markSynced(clientId: String, state: String = "SYNCED", serverId: Long?, error: String? = null)

    @Query("DELETE FROM attendance")
    suspend fun clear()
}

@Dao
interface SurveyDao {
    @Query("SELECT * FROM surveys WHERE eventId = :eventId ORDER BY submittedAt DESC")
    fun observeByEvent(eventId: Long): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys ORDER BY submittedAt DESC")
    fun observeAll(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    fun observeForUser(eventId: Long, userId: Long): Flow<SurveyEntity?>

    @Query("SELECT * FROM surveys WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    suspend fun findForUser(eventId: Long, userId: Long): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE clientId = :clientId LIMIT 1")
    suspend fun findByClientId(clientId: String): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE attendanceClientId = :attendanceClientId LIMIT 1")
    suspend fun findByAttendanceClientId(attendanceClientId: String): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE attendanceServerId = :attendanceServerId LIMIT 1")
    suspend fun findByAttendanceServerId(attendanceServerId: Long): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE serverId = :serverId LIMIT 1")
    suspend fun findByServerId(serverId: Long): SurveyEntity?

    @Query("SELECT attendanceClientId FROM surveys WHERE eventId = :eventId AND attendanceClientId IS NOT NULL")
    fun observeSurveyedAttendanceIds(eventId: Long): Flow<List<String>>

    @Query("SELECT * FROM surveys WHERE syncState = 'PENDING' OR syncState = 'ERROR'")
    suspend fun getPending(): List<SurveyEntity>

    @Query("SELECT * FROM surveys")
    suspend fun getAll(): List<SurveyEntity>

    @Upsert
    suspend fun upsert(survey: SurveyEntity)

    @Upsert
    suspend fun upsertAll(surveys: List<SurveyEntity>)

    @Query("UPDATE surveys SET syncState = :state, serverId = COALESCE(:serverId, serverId), error = :error WHERE clientId = :clientId")
    suspend fun updateSyncState(clientId: String, state: String, serverId: Long?, error: String?)

    @Query("""
        UPDATE surveys
        SET syncState = 'SYNCED', serverId = COALESCE(:serverId, serverId), error = NULL,
            redacted = 1, rating = 0, organizationRating = 0, impactRating = 0,
            comment = NULL, wouldRecommend = 0, participantName = 'Participante'
        WHERE clientId = :clientId
    """)
    suspend fun markSyncedAndRedact(clientId: String, serverId: Long?)

    @Query("""
        UPDATE surveys
        SET syncState = 'SYNCED', serverId = COALESCE(:serverId, serverId), error = NULL,
            redacted = 1, rating = 0, organizationRating = 0, impactRating = 0,
            comment = NULL, wouldRecommend = 0, participantName = 'Participante'
        WHERE attendanceClientId = :attendanceClientId
    """)
    suspend fun markSyncedAndRedactByAttendance(attendanceClientId: String, serverId: Long?)

    @Query("DELETE FROM surveys")
    suspend fun clear()
}

@Dao
interface PendingOperationDao {
    @Query("""
        SELECT * FROM pending_operations
        ORDER BY CASE WHEN type = 'ATTENDANCE_QR' OR type = 'ATTENDANCE_WALK_IN' THEN 0 ELSE 1 END,
                 createdAt ASC, id ASC
        LIMIT :limit
    """)
    suspend fun first(limit: Int): List<PendingOperationEntity>

    @Query("""
        SELECT * FROM pending_operations
        ORDER BY CASE WHEN type = 'ATTENDANCE_QR' OR type = 'ATTENDANCE_WALK_IN' THEN 0 ELSE 1 END,
                 createdAt ASC, id ASC
    """)
    fun observeAll(): Flow<List<PendingOperationEntity>>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: PendingOperationEntity): Long

    @Query("UPDATE pending_operations SET attempts = attempts + 1, lastError = :error WHERE clientId = :clientId")
    suspend fun markError(clientId: String, error: String?)

    @Query("DELETE FROM pending_operations WHERE clientId = :clientId")
    suspend fun delete(clientId: String)

    @Query("DELETE FROM pending_operations")
    suspend fun clear()
}
