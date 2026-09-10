package com.wisdomtower.academy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wisdomtower.academy.data.local.entity.ResourceProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM resource_progress WHERE resourceId = :resourceId AND userId = :userId LIMIT 1")
    fun getProgress(resourceId: String, userId: String): Flow<ResourceProgressEntity?>

    @Query("SELECT * FROM resource_progress WHERE userId = :userId")
    fun getAllUserProgress(userId: String): Flow<List<ResourceProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: ResourceProgressEntity)

    @Query("DELETE FROM resource_progress WHERE userId = :userId")
    suspend fun deleteProgressForUser(userId: String)
}
