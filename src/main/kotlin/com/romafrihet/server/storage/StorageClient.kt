package com.romafrihet.server.storage

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** Supabase Storage REST API 클라이언트. 트랜잭션 밖에서만 호출한다. */
@Component
class StorageClient(private val properties: StorageProperties, builder: RestClient.Builder) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val baseUrl = "${properties.supabaseUrl.trimEnd('/')}/storage/v1"
    private val client =
        builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.serviceRoleKey}")
            .defaultHeader("apikey", properties.serviceRoleKey)
            .build()

    fun upload(path: String, bytes: ByteArray, contentType: MediaType) = execute {
        client
            .post()
            .uri("/object/{bucket}/{path}", properties.bucket, path)
            .contentType(contentType)
            .body(bytes)
            .retrieve()
            .toBodilessEntity()
    }

    fun delete(paths: List<String>) {
        if (paths.isEmpty()) return
        execute {
            client
                .method(HttpMethod.DELETE)
                .uri("/object/{bucket}", properties.bucket)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("prefixes" to paths))
                .retrieve()
                .toBodilessEntity()
        }
    }

    /** 이미지마다 호출하지 않도록 일괄 서명 API를 쓴다. 반환값은 경로별 서명 URL이다. */
    fun createSignedUrls(paths: Collection<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()
        val signed =
            execute {
                client
                    .post()
                    .uri("/object/sign/{bucket}", properties.bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("expiresIn" to properties.signedUrlTtlSeconds, "paths" to paths.distinct()))
                    .retrieve()
                    .body(object : ParameterizedTypeReference<List<SignedUrl>>() {})
            }.orEmpty()
        return signed.associate { it.path to resolve(it) }
    }

    private fun resolve(signed: SignedUrl): String {
        val url = signed.signedURL
        if (url.isNullOrBlank()) {
            logger.warn("서명 URL 발급 실패: {}", signed.error)
            throw BusinessException(ErrorCode.STORAGE_UNAVAILABLE)
        }
        return "$baseUrl$url"
    }

    private fun <T> execute(call: () -> T): T = try {
        call()
    } catch (e: RestClientException) {
        logger.warn("Storage 호출 실패", e)
        throw BusinessException(ErrorCode.STORAGE_UNAVAILABLE)
    }

    private data class SignedUrl(val path: String, val signedURL: String? = null, val error: String? = null)
}
