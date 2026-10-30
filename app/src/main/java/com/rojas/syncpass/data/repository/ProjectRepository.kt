package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.ProjectDao
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.local.toEntity
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.ProjectRequest
import com.rojas.syncpass.data.remote.SyncPassApiService
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val api: SyncPassApiService,
    private val dao: ProjectDao
) {
    val projects: Flow<List<ProjectEntity>> = dao.observeAll()

    fun project(id: Long): Flow<ProjectEntity?> = dao.observeById(id)

    suspend fun refresh(): Result<Unit> = runCatching {
        dao.upsertAll(api.projects().map { it.toEntity() })
    }

    suspend fun create(request: ProjectRequest): Result<ProjectEntity> = runCatching {
        val project = api.createProject(request).toEntity()
        dao.upsert(project)
        project
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun update(id: Long, request: ProjectRequest): Result<ProjectEntity> = runCatching {
        val project = api.updateProject(id, request).toEntity()
        dao.upsert(project)
        project
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun delete(id: Long): Result<String> = runCatching {
        val response = api.deleteProject(id)
        dao.deleteById(id)
        response.message
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun get(id: Long): ProjectEntity? = dao.findById(id)
}

class RepositoryException(message: String) : Exception(message)
