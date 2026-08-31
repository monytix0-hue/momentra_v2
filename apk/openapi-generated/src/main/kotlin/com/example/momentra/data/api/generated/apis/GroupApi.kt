package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ProjectionEnvelope

interface GroupApi {
    /**
     * GET group/moments/{momentId}/actions
     * Group moment actions projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param momentId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("group/moments/{momentId}/actions")
    suspend fun getGroupMomentActions(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET group/moments/{momentId}/finance
     * Group moment finance projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param momentId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("group/moments/{momentId}/finance")
    suspend fun getGroupMomentFinance(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET group/moments/{momentId}/life
     * Group moment life projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param momentId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("group/moments/{momentId}/life")
    suspend fun getGroupMomentLife(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET group/moments/{momentId}/memory
     * Group moment memory projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param momentId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("group/moments/{momentId}/memory")
    suspend fun getGroupMomentMemory(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET group/moments/{momentId}/pulse
     * Group moment pulse projection
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param momentId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("group/moments/{momentId}/pulse")
    suspend fun getGroupMomentPulse(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET group/moments
     * Group moments list
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
    @GET("group/moments")
    suspend fun listGroupMoments(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 20, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

}
