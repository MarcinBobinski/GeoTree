package com.example.treedb.tree.treeDb

import com.example.treedb.tree.Tree
import java.time.Instant

interface TreeDbClient {
    fun updateTrees(trees: List<Tree>)
    fun fetchTreesInArea(xMin: Double, yMin: Double, xMax: Double, yMax: Double): List<Tree>
    fun lastUpdate(): Instant?
}