package com.example.geotreeapp.tree.api_um_waw

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import timber.log.Timber


internal interface UmWawService {
    @GET("api/3/action/datastore_search/")
    suspend fun getResponse(
        @QueryMap params : Map<String, String>
    ): Response<ResponsePayload>
}

internal object UmWawApi {
    private const val BASE_URL = "https://api.um.warszawa.pl/"
    private const val RESOURCE_ID = "ed6217dd-c8d0-4f7b-8bed-3b7eb81a95ba"
    private const val ID_FILTER = "{\"_id\":[%s]}"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
//        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    private val service: UmWawService by lazy {
        retrofit.create(UmWawService::class.java)
    }

    suspend fun fetchResponseWithNoTrees(): Response<ResponsePayload> {
        val params = mapOf(
            "resource_id" to RESOURCE_ID,
            "limit" to 0.toString()
        )
        return service.getResponse(params)
    }

    suspend fun fetchResponseWithTreeId(id: List<Long>): Response<ResponsePayload> {
        val params = mapOf(
            "resource_id" to RESOURCE_ID,
            "filters" to ID_FILTER.format(id.joinToString(separator = ","))
        )
        Timber.i(params.toString())
        return service.getResponse(params)
    }
}

data class ResponsePayload(
    private val result: ResultPayload
) {
    fun getSize() = result.total
    fun getTrees() = result.records
}

data class ResultPayload(
    var records: List<TreePayload>,
    val total: Long
)

class TreePayload(
    @Json(name = "_id") val id: Int,
    @Json(name = "x_wgs84") val x: Double,
    @Json(name = "y_wgs84") val y: Double,
    @Json(name = "numer_inw") val inv_number: String,
    @Json(name = "gatunek") val type: String,
)








