package com.example.treedb.infastructure

import com.example.treedb.tree.TreeService
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
internal class TreeRefreshDataScheduler(
    private val treeService: TreeService
) {

    // Every Hour
    @Scheduled(fixedDelay = 3_600_000)
    fun task() {
        treeService.refreshData()
    }
}