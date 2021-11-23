package com.example.treedb.base

import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
data class ExecutorProperties(
    val corePoolSize: Int,
    val maxPoolSize: Int
)
