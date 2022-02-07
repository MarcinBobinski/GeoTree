package com.example.geotreeapp.tree.tree_db

import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeDao

class TreeRepository(private val treeDao: TreeDao) {
    val trees = treeDao.fetchTrees()
    val size = treeDao.size()

    suspend fun addTree(tree: Tree){
        treeDao.insert(tree)
    }

    suspend fun addTrees(trees: List<Tree>){
        treeDao.insertAll(trees)
    }

    fun fetchTreesInArea(xMin: Double, xMax:Double, yMin:Double, yMax: Double): List<Tree> {
        return treeDao.fetchTreesInArea(xMin, xMax, yMin, yMax)
    }

    suspend fun deleteAll(){
        treeDao.deleteAll()
    }
}