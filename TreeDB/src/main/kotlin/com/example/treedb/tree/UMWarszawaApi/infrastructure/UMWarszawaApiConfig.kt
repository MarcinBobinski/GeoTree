package com.example.treedb.tree.UMWarszawaApi.infrastructure

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@EnableConfigurationProperties(UMWarszawaApiProperties::class)
class UMWarszawaApiConfig(
    val properties: UMWarszawaApiProperties
) {
    @PostConstruct
    fun test(){
        println(properties)
    }
}