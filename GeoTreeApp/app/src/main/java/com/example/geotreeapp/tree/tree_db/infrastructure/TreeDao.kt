package com.example.geotreeapp.tree.tree_db.infrastructure

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TreeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tree: Tree)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trees: List<Tree>)
//    suspend fun insertAll(vararg tree: Tree)

    @Query("SELECT * FROM tree_table")
    fun fetchTrees(): LiveData<List<Tree>>

    @Query("SELECT * FROM tree_table WHERE x>=:xMin AND x<:xMax AND y>=:yMin AND y>=:yMax")
    fun fetchTreesInArea(xMin: Double, xMax:Double, yMin:Double, yMax: Double): List<Tree>

    @Query("DELETE FROM tree_table")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tree_table")
    fun size(): LiveData<Long>
}