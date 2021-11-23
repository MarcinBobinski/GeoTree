package com.example.treedb.tree

import com.example.treedb.tree.UMWarszawaApi.UMWarszawaApiService
import com.example.treedb.tree.treeDb.TreeDbService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class TreeConfig {
    @Bean
    fun treeService(
        umWarszawaApiService: UMWarszawaApiService,
        treeDbService: TreeDbService
    ): TreeService{
        return TreeService(
            umWarszawaApiService,
            treeDbService
        )
    }
}