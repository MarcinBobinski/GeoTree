package com.example.treedb.tree.UMWarszawaApi.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "um-warszawa-api", ignoreInvalidFields = false)
data class UMWarszawaApiProperties(
    val url: String
)