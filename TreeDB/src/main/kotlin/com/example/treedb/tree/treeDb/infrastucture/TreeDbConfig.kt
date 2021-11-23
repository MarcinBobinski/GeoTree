package com.example.treedb.tree.treeDb.infrastucture

import com.example.treedb.tree.treeDb.TreeDbClient
import com.example.treedb.tree.treeDb.TreeDbService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
internal class TreeDbConfig(
  private val clock: Clock
) {
    @Bean
    fun treeDbService(): TreeDbService {
        return TreeDbService(treeDbClient())
    }

    fun treeDbClient(): TreeDbClient {
        return InMemoryTreeDbClient(clock)
    }

}