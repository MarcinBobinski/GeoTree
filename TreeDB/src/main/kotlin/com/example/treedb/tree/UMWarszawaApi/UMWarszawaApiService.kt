package com.example.treedb.tree.UMWarszawaApi

import com.example.treedb.tree.Tree
import org.springframework.core.task.AsyncTaskExecutor
import java.util.stream.Collectors

internal class UMWarszawaApiService(
    private val umWarszawaApiClient: UMWarszawaApiClient,
    private val executor: AsyncTaskExecutor
) {
    fun fetchTrees(): List<Tree> {
        val pageCount = umWarszawaApiClient.fetchPageCount()

        val tasks =
            (0..pageCount).map { executor.submit<List<Tree>> { return@submit umWarszawaApiClient.fetchTrees(it) } }

        return tasks.stream().map { it.get() }.collect(Collectors.toList()).flatten().distinct()
    }
}