package com.example.geotreeapp.tree.tree_db.infrastructure

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_table")
data class Tree(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val x: Double,
    val y: Double
)
