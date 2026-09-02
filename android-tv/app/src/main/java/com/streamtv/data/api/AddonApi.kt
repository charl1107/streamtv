package com.streamtv.data.api

import com.streamtv.data.dto.*
import retrofit2.http.GET
import retrofit2.http.Path

interface AddonApi {

    @GET("manifest.json")
    suspend fun getManifest(): ManifestDto

    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") catalogId: String
    ): CatalogResponseDto

    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String,
        @Path("id") metaId: String
    ): MetaResponseDto

    @GET("stream/{type}/{id}.json")
    suspend fun getStream(
        @Path("type") type: String,
        @Path("id") streamId: String
    ): StreamResponseDto
}
