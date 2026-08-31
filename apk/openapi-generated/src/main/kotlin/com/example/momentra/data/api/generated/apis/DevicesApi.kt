package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.CommandEnvelope
import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ExecuteActionProposal409Response

interface DevicesApi {
    /**
     * POST me/devices
     * Register push device
     * 
     * Responses:
     *  - 201: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 409: Optimistic concurrency version conflict
     *  - 500: Internal server error
     *
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("me/devices")
    suspend fun registerDevice(@Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * DELETE me/devices/{deviceId}
     * Revoke push device
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param deviceId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @DELETE("me/devices/{deviceId}")
    suspend fun revokeDevice(@Path("deviceId") deviceId: kotlin.String, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

}
