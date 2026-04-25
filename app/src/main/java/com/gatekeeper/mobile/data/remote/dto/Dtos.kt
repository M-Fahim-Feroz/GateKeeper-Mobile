package com.gatekeeper.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val message: String,
    @SerializedName("session_id") val sessionId: String = "mobile-session"
)

data class ChatResponse(
    val response: String,
    @SerializedName("tool_calls") val toolCalls: List<String> = emptyList(),
    val success: Boolean = true,
    @SerializedName("execution_steps") val executionSteps: List<Map<String, Any>>? = null,
    @SerializedName("operation_results") val operationResults: List<Map<String, Any>>? = null,
    @SerializedName("partial_failures") val partialFailures: List<Map<String, Any>>? = null,
    val warnings: List<String>? = null,
    val outcome: String? = null,
    @SerializedName("context_notes") val contextNotes: List<String>? = null,
    @SerializedName("trace_id") val traceId: String? = null
)

data class HealthResponse(
    val status: String
)
