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

interface BusinessApi {
    /**
     * POST companies
     * Create company
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
    @POST("companies")
    suspend fun createCompany(@Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * POST companies/{companyId}/locations
     * Create company location
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
     * @param companyId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("companies/{companyId}/locations")
    suspend fun createCompanyLocation(@Path("companyId") companyId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * POST companies/{companyId}/teams
     * Create company team
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
     * @param companyId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @POST("companies/{companyId}/teams")
    suspend fun createCompanyTeam(@Path("companyId") companyId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * GET business/moments/{momentId}/actions
     * Business moment actions projection
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
    @GET("business/moments/{momentId}/actions")
    suspend fun getBusinessMomentActions(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET business/moments/{momentId}/finance
     * Business moment finance projection
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
    @GET("business/moments/{momentId}/finance")
    suspend fun getBusinessMomentFinance(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET business/moments/{momentId}/life
     * Business moment life projection
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
    @GET("business/moments/{momentId}/life")
    suspend fun getBusinessMomentLife(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET business/moments/{momentId}/memory
     * Business moment memory projection
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
    @GET("business/moments/{momentId}/memory")
    suspend fun getBusinessMomentMemory(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET business/moments/{momentId}/pulse
     * Business moment pulse projection
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
    @GET("business/moments/{momentId}/pulse")
    suspend fun getBusinessMomentPulse(@Path("momentId") momentId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET companies/{companyId}
     * Get company
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param companyId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("companies/{companyId}")
    suspend fun getCompany(@Path("companyId") companyId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET business/moments
     * Business moments list
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
    @GET("business/moments")
    suspend fun listBusinessMoments(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 20, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET companies
     * List authorized companies
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
    @GET("companies")
    suspend fun listCompanies(): Response<ProjectionEnvelope>

    /**
     * GET companies/{companyId}/locations
     * List company locations
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param companyId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("companies/{companyId}/locations")
    suspend fun listCompanyLocations(@Path("companyId") companyId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * GET companies/{companyId}/teams
     * List company teams
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 500: Internal server error
     *
     * @param companyId 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("companies/{companyId}/teams")
    suspend fun listCompanyTeams(@Path("companyId") companyId: java.util.UUID, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

    /**
     * PATCH companies/{companyId}
     * Update company
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 409: Optimistic concurrency version conflict
     *  - 500: Internal server error
     *
     * @param companyId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @PATCH("companies/{companyId}")
    suspend fun updateCompany(@Path("companyId") companyId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

    /**
     * PATCH companies/{companyId}/locations/{locationId}
     * Update company location
     * 
     * Responses:
     *  - 200: Success
     *  - 400: Validation error
     *  - 401: Unauthenticated
     *  - 403: Forbidden
     *  - 404: Not found
     *  - 409: Optimistic concurrency version conflict
     *  - 500: Internal server error
     *
     * @param companyId 
     * @param locationId 
     * @param idempotencyKey 
     * @param body 
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [CommandEnvelope]
     */
    @PATCH("companies/{companyId}/locations/{locationId}")
    suspend fun updateCompanyLocation(@Path("companyId") companyId: java.util.UUID, @Path("locationId") locationId: java.util.UUID, @Header("Idempotency-Key") idempotencyKey: kotlin.String, @Body body: kotlin.Any, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<CommandEnvelope>

}
