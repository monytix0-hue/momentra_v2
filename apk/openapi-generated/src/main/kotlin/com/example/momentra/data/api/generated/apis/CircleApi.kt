package com.example.momentra.data.api.generated.apis

import com.example.momentra.data.api.generated.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.example.momentra.data.api.generated.models.ErrorEnvelope
import com.example.momentra.data.api.generated.models.ProjectionEnvelope

interface CircleApi {
    /**
     * GET life360
     * Circle (Life360) projection read
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
    @GET("life360")
    suspend fun getLife360(): Response<ProjectionEnvelope>

}
