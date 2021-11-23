package com.example.treedb.tree.UMWarszawaApi.infrastructure

import com.example.treedb.tree.UMWarszawaApi.UMWarszawaApiClient
import com.example.treedb.tree.UMWarszawaApi.UMWarszawaApiService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.task.TaskExecutorBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(UMWarszawaApiProperties::class)
internal class UMWarszawaApiConfig(
    private val properties: UMWarszawaApiProperties,
    private val restTemplateBuilder: RestTemplateBuilder
) {
    @Bean
    fun umWarszawaApiService(): UMWarszawaApiService {
        return UMWarszawaApiService(
            umWarszawaApiClient(),
            executor()
        )
    }


    fun umWarszawaApiClient(): UMWarszawaApiClient {
        return UMWarszawaApiHttpClient(
            properties,
            restTemplate()
        )
    }


    fun restTemplate(): RestTemplate {
        return restTemplateBuilder.build()
    }

    fun executor(): AsyncTaskExecutor {
        val executor = TaskExecutorBuilder()
            .corePoolSize(properties.executor.corePoolSize)
            .maxPoolSize(properties.executor.maxPoolSize)
            .build()
        executor.initialize()
        return executor
    }
}