package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ProjectionEnvelope

interface ActivityApi {
    /**
     * GET moments/{momentId}/activity
     * Moment activity timeline
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
     * @param cursor  (optional)
     * @param limit  (optional, default to 20)
     * @param xCorrelationId Optional client correlation ID; echoed in responses. (optional)
     * @return [ProjectionEnvelope]
     */
    @GET("moments/{momentId}/activity")
    suspend fun getMomentActivity(@Path("momentId") momentId: java.util.UUID, @Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = 20, @Header("X-Correlation-Id") xCorrelationId: java.util.UUID? = null): Response<ProjectionEnvelope>

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

}
