package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.CommandEnvelope
import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ExecuteActionProposal409Response

interface MediaApi {
    /**
     * POST media/uploads/{uploadId}/complete
     * Complete media upload
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
     * @param uploadId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("media/uploads/{uploadId}/complete")
    suspend fun completeMediaUpload(@Path("uploadId") uploadId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * POST media/uploads
     * Create signed upload intent
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
    @POST("media/uploads")
    suspend fun createMediaUpload(@Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

}
