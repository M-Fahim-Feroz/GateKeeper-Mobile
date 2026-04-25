package com.gatekeeper.mobile.data.remote

import com.gatekeeper.mobile.data.remote.dto.ChatRequest
import com.gatekeeper.mobile.data.remote.dto.ChatResponse
import com.gatekeeper.mobile.data.remote.dto.HealthResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for communicating with the GateKeeper-Agent AI backend.
 * This connects to the existing FastAPI backend running on the Windows desktop.
 */
interface AiApiService {

    @GET("health")
    suspend fun checkHealth(): HealthResponse

    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse

    @POST("clear-history")
    suspend fun clearHistory(@Query("session_id") sessionId: String)
}
