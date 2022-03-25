package com.example.geotreeapp.tree.tree_db.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TreeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tree: Tree)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trees: List<Tree>)

    @Query("SELECT * FROM tree_table")
    fun fetchTrees(): LiveData<List<Tree>>

    @Query("DELETE FROM tree_table")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tree_table")
    fun size(): LiveData<Long>

    @Update
    suspend fun updateTree(tree: Tree)

    @Update
    suspend fun updateTrees(trees: List<Tree>)
}