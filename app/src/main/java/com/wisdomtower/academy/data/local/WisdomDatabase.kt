package com.wisdomtower.academy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wisdomtower.academy.data.local.dao.EnrollmentDao
import com.wisdomtower.academy.data.local.dao.ProgressDao
import com.wisdomtower.academy.data.local.dao.ResourceDao
import com.wisdomtower.academy.data.local.entity.DownloadedResourceEntity
import com.wisdomtower.academy.data.local.entity.ResourceProgressEntity
import com.wisdomtower.academy.data.local.entity.UserEnrollmentEntity

@Database(
    entities = [
        DownloadedResourceEntity::class,
        ResourceProgressEntity::class,
        UserEnrollmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WisdomDatabase : RoomDatabase() {

    abstract fun resourceDao(): ResourceDao
    abstract fun progressDao(): ProgressDao
    abstract fun enrollmentDao(): EnrollmentDao

    companion object {
        @Volatile
        private var INSTANCE: WisdomDatabase? = null

        fun getInstance(context: Context): WisdomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WisdomDatabase::class.java,
                    "wisdom_tower_academy.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
