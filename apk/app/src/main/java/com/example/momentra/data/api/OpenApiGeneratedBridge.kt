package com.example.momentra.data.api

import com.example.momentra.data.api.generated.infrastructure.ApiClient as GeneratedApiClient

/** Compile-time bridge to the OpenAPI-generated Retrofit client (SUPP-009). */
object OpenApiGeneratedBridge {
    const val CONTRACT_VERSION: String = "momentra-v1-generated"

    fun createClient(baseUrl: String): GeneratedApiClient = GeneratedApiClient(baseUrl = baseUrl)
}
