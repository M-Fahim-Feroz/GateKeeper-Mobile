package com.gatekeeper.mobile.data.repository

import com.gatekeeper.mobile.data.remote.AiApiService
import com.gatekeeper.mobile.data.remote.dto.ChatRequest
import com.gatekeeper.mobile.data.remote.dto.ChatResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatRepository @Inject constructor(
    private val api: AiApiService
) {
    private val sessionId = "mobile-${System.currentTimeMillis()}"

    suspend fun sendMessage(message: String): Result<ChatResponse> {
        return try {
            val response = api.sendMessage(ChatRequest(message, sessionId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Boolean {
        return try {
            val response = api.checkHealth()
            response.status == "healthy"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearHistory() {
        try {
            api.clearHistory(sessionId)
        } catch (_: Exception) { }
    }
}
