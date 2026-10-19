package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ChapterDao
import com.example.data.dao.DocumentDao
import com.example.data.dao.FolderDao
import com.example.data.dao.SubjectDao
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.SubjectEntity

@Database(
    entities = [
        SubjectEntity::class,
        FolderEntity::class,
        ChapterEntity::class,
        DocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun folderDao(): FolderDao
    abstract fun chapterDao(): ChapterDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mon_classeur_bac.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
