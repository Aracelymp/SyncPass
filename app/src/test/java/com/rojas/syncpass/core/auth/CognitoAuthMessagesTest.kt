package com.rojas.syncpass.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CognitoAuthMessagesTest {

    @Test
    fun `cancellation has a clear message`() {
        val message = CognitoAuthMessages.authorizationMessage(
            cancelled = true,
            networkError = false
        )

        assertEquals(CognitoAuthMessages.CANCELLED, message)
        assertTrue(message.contains("cancelado", ignoreCase = true))
    }

    @Test
    fun `network failures are reported for authorization and token exchange`() {
        assertEquals(
            CognitoAuthMessages.NETWORK_ERROR,
            CognitoAuthMessages.authorizationMessage(cancelled = false, networkError = true)
        )
        assertEquals(
            CognitoAuthMessages.NETWORK_ERROR,
            CognitoAuthMessages.tokenExchangeMessage(networkError = true)
        )
        assertTrue(CognitoAuthMessages.NETWORK_ERROR.contains("conexión", ignoreCase = true))
    }

    @Test
    fun `other authorization failures use a generic message`() {
        assertEquals(
            CognitoAuthMessages.AUTHORIZATION_FAILED,
            CognitoAuthMessages.authorizationMessage(cancelled = false, networkError = false)
        )
        assertEquals(
            CognitoAuthMessages.TOKEN_EXCHANGE_FAILED,
            CognitoAuthMessages.tokenExchangeMessage(networkError = false)
        )
    }

    @Test
    fun `backend 403 explains the missing link`() {
        val message = CognitoAuthMessages.backendFailure(403)

        assertEquals(CognitoAuthMessages.IDENTITY_NOT_LINKED, message)
        assertTrue(message!!.contains("Cognito", ignoreCase = true))
        assertTrue(message.contains("vinculada", ignoreCase = true))
    }

    @Test
    fun `backend 401 reports an invalid token`() {
        assertEquals(CognitoAuthMessages.INVALID_TOKEN, CognitoAuthMessages.backendFailure(401))
    }

    @Test
    fun `other backend statuses keep the generic API message`() {
        assertNull(CognitoAuthMessages.backendFailure(500))
    }
}
