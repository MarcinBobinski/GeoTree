package com.example.geotreeapp.tree.tree_db.infrastructure

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Tree::class], version = 2, exportSchema = false)
@TypeConverters(TreeStatusConverter::class)
abstract class TreeDatabase : RoomDatabase() {
    abstract fun treeDao(): TreeDao

    companion object {
        @Volatile
        private var INSTANCE: TreeDatabase? = null

        fun getInstance(context: Context): TreeDatabase {
            val tempInstance = INSTANCE
            if(tempInstance != null){
                return tempInstance
            }
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TreeDatabase::class.java,
                        "tree_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}