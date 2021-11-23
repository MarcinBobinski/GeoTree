package com.example.treedb.tree.treeDb

import com.example.treedb.tree.Tree
import java.time.Instant

internal class TreeDbService(
    private val treeDbClient: TreeDbClient
) {
    fun updateTrees(trees: List<Tree>){
        treeDbClient.updateTrees(trees)
    }

    fun fetchTreesInArea(
        xMin: Double,
        yMin: Double,
        xMax: Double,
        yMax: Double
    ) : List<Tree>{
        return treeDbClient.fetchTreesInArea(xMin, yMin, xMax, yMax)
    }

    fun lastUpdate(): Instant? {
        return treeDbClient.lastUpdate()
    }
}