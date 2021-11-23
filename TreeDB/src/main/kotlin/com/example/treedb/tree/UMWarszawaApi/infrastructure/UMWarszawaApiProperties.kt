package com.example.treedb.tree.UMWarszawaApi.infrastructure

import com.example.treedb.base.ExecutorProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConstructorBinding
@ConfigurationProperties(prefix = "um-warszawa-api", ignoreUnknownFields = false)
data class UMWarszawaApiProperties(
    val url: String,
    val resourceId: String,
    val pageSize: Int,
    @NestedConfigurationProperty
    val executor: ExecutorProperties
)