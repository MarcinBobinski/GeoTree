package com.example.geotreeapp.tree.tree_db.infrastructure

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "tree_table")
data class Tree(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val x: Double,
    val y: Double,
    val inv_number: String,
    val type: String,
    val treeStatus: TreeStatus
)

enum class TreeStatus(val value: Int) {
    NOT_VERIFIED(0), VERIFIED(1), MISSING(2)
}

class TreeStatusConverter {
    @TypeConverter
    fun toTreeStatus(value: Int) = enumValues<TreeStatus>()[value]
    @TypeConverter
    fun fromTreeStatus(value: TreeStatus) = value.ordinal
}