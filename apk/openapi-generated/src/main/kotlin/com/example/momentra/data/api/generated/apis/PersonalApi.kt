package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.CommandEnvelope
import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ExecuteActionProposal409Response
import com.example.momentra.data.api.generated.models.ProjectionEnvelope

interface PersonalApi {
    /**
     * POST moments/{momentId}/future-items
     * Create future item
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
     * @param momentId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("moments/{momentId}/future-items")
    suspend fun createFutureItem(@Path("momentId") momentId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * POST moments/{momentId}/lifestyle-activities
     * Create lifestyle activity
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
     * @param momentId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("moments/{momentId}/lifestyle-activities")
    suspend fun createLifestyleActivity(@Path("momentId") momentId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * GET personal/activity
     * Personal activity timeline
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param cursor  (optional)
     * @param limit  (optional, default to 20)
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("personal/activity")
    suspend fun getPersonalActivity(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 20, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET personal/attention
     * Personal attention projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @return [ProjectionEnvelope]
     */
    @GET("personal/attention")
    suspend fun getPersonalAttention(): Response<ProjectionEnvelope>

    /**
     * GET personal/life
     * Personal life projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @return [ProjectionEnvelope]
     */
    @GET("personal/life")
    suspend fun getPersonalLife(): Response<ProjectionEnvelope>

    /**
     * GET personal/memory
     * Personal memory projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @return [ProjectionEnvelope]
     */
    @GET("personal/memory")
    suspend fun getPersonalMemory(): Response<ProjectionEnvelope>

    /**
     * GET personal/pulse
     * Personal pulse projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @return [ProjectionEnvelope]
     */
    @GET("personal/pulse")
    suspend fun getPersonalPulse(): Response<ProjectionEnvelope>

    /**
     * GET personal/moments
     * Personal moments list
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param cursor  (optional)
     * @param limit  (optional, default to 20)
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("personal/moments")
    suspend fun listPersonalMoments(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 20, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * POST moments/{momentId}/observations
     * Record observation
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
     * @param momentId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("moments/{momentId}/observations")
    suspend fun recordObservation(@Path("momentId") momentId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * POST moments/{momentId}/relationship-activities
     * Record relationship activity
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
     * @param momentId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("moments/{momentId}/relationship-activities")
    suspend fun recordRelationshipActivity(@Path("momentId") momentId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

}
