package com.example.momentra.data.api

import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorsTest {
    @Test
    fun maps401ToUnauthenticated() {
        val e = mapHttpFailure(401, "UNAUTHORIZED", null)
        assertTrue(e is ApiResultException.Unauthenticated)
    }

    @Test
    fun maps403ToForbiddenNotLogout() {
        val e = mapHttpFailure(403, "GOVERNANCE_DENIED", "denied")
        assertTrue(e is ApiResultException.Forbidden)
    }

    @Test
    fun maps404NotFound() {
        assertTrue(mapHttpFailure(404, null, null) is ApiResultException.NotFound)
    }

    @Test
    fun maps409Conflict() {
        assertTrue(mapHttpFailure(409, "VERSION_CONFLICT", "x") is ApiResultException.Conflict)
    }

    @Test
    fun maps422Validation() {
        assertTrue(mapHttpFailure(422, "VALIDATION_FAILED", "bad") is ApiResultException.Validation)
    }

    @Test
    fun maps429RateLimited() {
        assertTrue(mapHttpFailure(429, null, null) is ApiResultException.RateLimited)
    }

    @Test
    fun maps5xxServer() {
        assertTrue(mapHttpFailure(503, "INFRASTRUCTURE_UNAVAILABLE", null) is ApiResultException.Server)
    }
}
