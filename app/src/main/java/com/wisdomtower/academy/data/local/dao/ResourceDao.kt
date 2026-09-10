package com.wisdomtower.academy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wisdomtower.academy.data.local.entity.DownloadedResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query("SELECT * FROM downloaded_resources ORDER BY downloadTimestamp DESC")
    fun getAllDownloaded(): Flow<List<DownloadedResourceEntity>>

    @Query("SELECT * FROM downloaded_resources WHERE id = :id LIMIT 1")
    fun getDownloadedById(id: String): Flow<DownloadedResourceEntity?>

    @Query("SELECT * FROM downloaded_resources WHERE id = :id LIMIT 1")
    suspend fun getDownloadedByIdDirect(id: String): DownloadedResourceEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_resources WHERE id = :id)")
    fun isDownloaded(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: DownloadedResourceEntity)

    @Query("DELETE FROM downloaded_resources WHERE id = :id")
    suspend fun deleteResourceById(id: String)

    @Query("DELETE FROM downloaded_resources")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM downloaded_resources")
    fun getDownloadedCount(): Flow<Int>
}
