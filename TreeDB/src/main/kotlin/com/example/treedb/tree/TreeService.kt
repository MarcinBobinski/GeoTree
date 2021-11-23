package com.example.treedb.tree

import com.example.treedb.tree.UMWarszawaApi.UMWarszawaApiService
import com.example.treedb.tree.treeDb.TreeDbService

internal class TreeService(
    private val umWarszawaApiService: UMWarszawaApiService,
    private val treeDbService: TreeDbService
) {
    fun refreshData() {
        val trees = umWarszawaApiService.fetchTrees()
        treeDbService.updateTrees(trees)
    }

    fun fetchTreesInArea(
        xMin: Double,
        yMin: Double,
        xMax: Double,
        yMax: Double
    ) : List<Tree>{
        return treeDbService.fetchTreesInArea(xMin, yMin, xMax, yMax)
    }

}

data class Tree(
    val id: Int,
    val x: Double,
    val y: Double,
    val inv_number: String
)