package com.rojas.syncpass.data.remote

import retrofit2.http.*

interface SyncPassApiService {
    @GET("api/projects")
    suspend fun projects(): List<ProjectDto>

    @POST("api/projects")
    suspend fun createProject(@Body body: ProjectRequest): ProjectDto

    @PUT("api/projects/{id}")
    suspend fun updateProject(@Path("id") id: Long, @Body body: ProjectRequest): ProjectDto

    @DELETE("api/projects/{id}")
    suspend fun deleteProject(@Path("id") id: Long): MessageResponse

    // Admin users
    @GET("api/admin/users")
    suspend fun adminUsers(): List<AdminUserDto>

    @POST("api/admin/users")
    suspend fun createAdminUser(@Body body: AdminUserCreateRequest): AdminUserDto

    @PATCH("api/admin/users/{id}/active")
    suspend fun setAdminUserActive(@Path("id") id: Long, @Body body: UserActiveRequest): AdminUserDto

    // Admin Cognito link requests
    @GET("api/admin/cognito-link-requests")
    suspend fun pendingCognitoLinkRequests(): List<CognitoLinkRequestDto>

    @POST("api/admin/cognito-link-requests/{id}/approve")
    suspend fun approveCognitoLinkRequest(
        @Path("id") id: Long,
        @Body body: CognitoLinkApproveRequest
    ): CognitoLinkRequestDto

    @POST("api/admin/cognito-link-requests/{id}/reject")
    suspend fun rejectCognitoLinkRequest(
        @Path("id") id: Long,
        @Body body: CognitoLinkRejectRequest
    ): CognitoLinkRequestDto

    @GET("api/events")
    suspend fun events(@Query("projectId") projectId: Long? = null): List<EventDto>

    @GET("api/events/{id}")
    suspend fun event(@Path("id") id: Long): EventDto

    @GET("api/events/{id}/detail")
    suspend fun eventDetail(@Path("id") id: Long): EventDetailDto

    @POST("api/events")
    suspend fun createEvent(@Body body: EventRequest): EventDto

    @PUT("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: Long, @Body body: EventRequest): EventDto

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): MessageResponse

    @POST("api/events/{id}/attendance/scan")
    suspend fun scan(@Path("id") id: Long, @Body body: ScanRequest): AttendanceDto

    @POST("api/events/{id}/attendance/walk-in")
    suspend fun walkIn(@Path("id") id: Long, @Body body: WalkInRequest): AttendanceDto

    @POST("api/events/{id}/enroll")
    suspend fun enroll(@Path("id") id: Long): EnrollmentDto

    @DELETE("api/events/{id}/enroll")
    suspend fun cancelEnrollment(@Path("id") id: Long): MessageResponse

    @POST("api/events/{id}/survey")
    suspend fun submitSurvey(@Path("id") id: Long, @Body body: SurveyRequest): SurveyDto

    @GET("api/events/{id}/survey/summary")
    suspend fun surveySummary(@Path("id") id: Long): SurveySummaryDto

    @GET("api/reports/impact")
    suspend fun impactReport(): ImpactReportDto

    @POST("api/sync/batch")
    suspend fun syncBatch(@Body body: SyncBatchRequest): SyncBatchResponse

    @GET("api/sync/pull")
    suspend fun syncPull(): SyncPullDto
}
