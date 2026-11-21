package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.ProjectDao
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.remote.ProjectDto
import com.rojas.syncpass.data.remote.ProjectRequest
import com.rojas.syncpass.data.remote.SyncPassApiService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryTest {

    private lateinit var api: SyncPassApiService
    private lateinit var dao: ProjectDao
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        every { dao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        repository = ProjectRepository(api, dao)
    }

    private fun projectDto() = ProjectDto(
        id = 1L, code = "P1", name = "Proyecto", description = "Desc",
        startDate = "2026-01-01", endDate = "2026-12-31", status = "ACTIVO",
        responsible = "Docente", eventCount = 0, createdAt = null
    )

    @Test
    fun `projects exposes dao flow`() = runTest(UnconfinedTestDispatcher()) {
        val result = repository.projects.first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `project exposes dao flow by id`() = runTest(UnconfinedTestDispatcher()) {
        val entity = projectDto().let {
            ProjectEntity(
                id = it.id, code = it.code, name = it.name, description = it.description,
                startDate = it.startDate, endDate = it.endDate, status = it.status,
                responsible = it.responsible, eventCount = it.eventCount, createdAt = ""
            )
        }
        every { dao.observeById(1L) } returns flowOf(entity)

        val result = repository.project(1L).first()

        assertEquals(entity, result)
    }

    @Test
    fun `refresh upserts projects from api`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { api.projects() } returns listOf(projectDto())
        coEvery { dao.upsertAll(any()) } just Runs

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        coVerify { dao.upsertAll(any()) }
    }

    @Test
    fun `refresh returns failure on exception`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { api.projects() } throws RuntimeException("network")

        val result = repository.refresh()

        assertTrue(result.isFailure)
    }

    @Test
    fun `create stores and returns project`() = runTest(UnconfinedTestDispatcher()) {
        val request = ProjectRequest(
            code = "P1", name = "Proyecto", description = "Desc",
            startDate = "2026-01-01", endDate = "2026-12-31", status = "ACTIVO", responsible = "Docente"
        )
        coEvery { api.createProject(request) } returns projectDto()
        coEvery { dao.upsert(any()) } just Runs

        val result = repository.create(request)

        assertTrue(result.isSuccess)
        assertEquals("Proyecto", result.getOrThrow().name)
    }

    @Test
    fun `create returns RepositoryException on api error`() = runTest(UnconfinedTestDispatcher()) {
        val request = ProjectRequest(
            code = "P1", name = "Proyecto", description = "Desc",
            startDate = "2026-01-01", endDate = "2026-12-31", status = "ACTIVO", responsible = "Docente"
        )
        coEvery { api.createProject(request) } throws RuntimeException("error")

        val result = repository.create(request)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RepositoryException)
    }

    @Test
    fun `update stores and returns project`() = runTest(UnconfinedTestDispatcher()) {
        val request = ProjectRequest(
            code = "P1", name = "Proyecto Actualizado", description = "Desc",
            startDate = "2026-01-01", endDate = "2026-12-31", status = "ACTIVO", responsible = "Docente"
        )
        coEvery { api.updateProject(1L, request) } returns projectDto().copy(name = "Proyecto Actualizado")
        coEvery { dao.upsert(any()) } just Runs

        val result = repository.update(1L, request)

        assertTrue(result.isSuccess)
        assertEquals("Proyecto Actualizado", result.getOrThrow().name)
    }

    @Test
    fun `delete removes project locally`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { api.deleteProject(1L) } returns com.rojas.syncpass.data.remote.MessageResponse("Eliminado")
        coEvery { dao.deleteById(1L) } just Runs

        val result = repository.delete(1L)

        assertTrue(result.isSuccess)
        assertEquals("Eliminado", result.getOrThrow())
        coVerify { dao.deleteById(1L) }
    }

    @Test
    fun `get returns dao result`() = runTest(UnconfinedTestDispatcher()) {
        val entity = projectDto().let {
            ProjectEntity(
                id = it.id, code = it.code, name = it.name, description = it.description,
                startDate = it.startDate, endDate = it.endDate, status = it.status,
                responsible = it.responsible, eventCount = it.eventCount, createdAt = ""
            )
        }
        coEvery { dao.findById(1L) } returns entity

        val result = repository.get(1L)

        assertEquals(entity, result)
    }
}
