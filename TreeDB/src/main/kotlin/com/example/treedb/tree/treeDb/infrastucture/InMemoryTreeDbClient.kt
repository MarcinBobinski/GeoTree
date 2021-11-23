package com.example.treedb.tree.treeDb.infrastucture

import com.example.treedb.tree.Tree
import com.example.treedb.tree.treeDb.TreeDbClient
import sun.awt.Mutex
import java.time.Clock
import java.time.Instant

class InMemoryTreeDbClient (
    private val clock: Clock
) : TreeDbClient {
    private val mutex: Mutex = Mutex()
    private var trees: List<Tree>? = null

    private var lastUpdate: Instant? = null

    override fun updateTrees(trees: List<Tree>) {
        mutex.lock()
        this.trees = trees
        this.lastUpdate = Instant.now(clock)
        mutex.unlock()
    }

    override fun fetchTreesInArea(xMin: Double, yMin: Double, xMax: Double, yMax: Double): List<Tree> {
        mutex.lock()
        val treesCopy = this.trees
        mutex.unlock()
        return treesCopy?.filter { it.x >= xMin && it.x < xMax && it.y >= yMin && it.y < yMax } ?: listOf() // TODO: Maybe throw exception when treesCopy is null
    }

    override fun lastUpdate(): Instant? {
        mutex.lock()
        val lastUpdateCopy = this.lastUpdate
        mutex.unlock()
        return lastUpdateCopy
    }


}