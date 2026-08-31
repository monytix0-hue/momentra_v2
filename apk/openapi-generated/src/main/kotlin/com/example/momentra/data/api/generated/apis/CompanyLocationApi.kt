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

interface CompanyLocationApi {
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
