package com.example.treedb.tree

import org.springframework.boot.task.TaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor

@Configuration
class TreeConfig {
    @Bean
    fun treeService(): TreeService{
        return TreeService()
    }

    @Bean
    fun executor(): AsyncTaskExecutor {
        return TaskExecutorBuilder()
            .corePoolSize(5)
            .maxPoolSize(20)
            .build()
    }
}