package com.example.treedb.tree.UMWarszawaApi.infrastructure

import com.example.treedb.tree.Tree
import com.example.treedb.tree.UMWarszawaApi.UMWarszawaApiClient
import org.springframework.http.RequestEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.lang.Exception
import java.net.URI
import kotlin.math.ceil

internal class UMWarszawaApiHttpClient(
    private val umWarszawaApiProperties: UMWarszawaApiProperties,
    private val restTemplate: RestTemplate,
) : UMWarszawaApiClient {
    override fun fetchTrees(page: Int): List<Tree> {
        val uri = UriComponentsBuilder
            .fromUriString(umWarszawaApiProperties.url)
            .path("/api/action/datastore_search/")
            .queryParam("resource_id", umWarszawaApiProperties.resourceId)
            .queryParam("limit", umWarszawaApiProperties.pageSize)
            .queryParam("offset", umWarszawaApiProperties.pageSize * page)
            .toUriString()

        val request = RequestEntity.get(URI(uri)).build()
        return try {
            val response = restTemplate.exchange(request, UMWarszawaApiResponse::class.java)

            check(response.statusCode.is2xxSuccessful) { throw HttpClientErrorException(response.statusCode) }
            checkNotNull(response.body) { "Response body is null" }

            response.body!!.result.records.map { it.toTree() }
        } catch (e: HttpClientErrorException) {
            return listOf()
        } catch (e: Exception) {
            return listOf()
        }
    }

    override fun fetchPageCount(): Int {
        val uri = UriComponentsBuilder
            .fromUriString(umWarszawaApiProperties.url)
            .path("/api/action/datastore_search/")
            .queryParam("resource_id", umWarszawaApiProperties.resourceId)
            .queryParam("limit", 0)
            .toUriString()

        val request = RequestEntity.get(URI(uri)).build()
        return try {
            val response = restTemplate.exchange(request, UMWarszawaApiResponse::class.java)

            check(response.statusCode.is2xxSuccessful) { throw HttpClientErrorException(response.statusCode) }
            checkNotNull(response.body) { "Response body is null" }

            ceil(response.body!!.result.total.toDouble() / umWarszawaApiProperties.pageSize).toInt()
        } catch (e: HttpClientErrorException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }
}

data class UMWarszawaApiResponse(
    var result: UMWarszawaApiResult
)

data class UMWarszawaApiResult(
    var total: Int,
    var records: List<UMWarszawaApiTreeResponse>
)

data class UMWarszawaApiTreeResponse(
    var _id: Int,
    var x_wgs84: Double,
    var y_wgs84: Double,
    var x_pl2000: Double,
    var y_pl2000: Double,
    var numer_inw: String
) {
    fun toTree(): Tree {
        return Tree(_id, x_wgs84, y_wgs84, numer_inw)
    }
}