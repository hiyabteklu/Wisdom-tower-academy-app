package com.wisdomtower.academy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_resources")
data class DownloadedResourceEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val resourceType: String, // "PDF", "NOTE", "FLASHCARD", "QUIZ"
    val packageId: String,
    val packageName: String,
    val subjectId: String,
    val subjectName: String,
    val localFilePath: String? = null,
    val textContent: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadTimestamp: Long = System.currentTimeMillis()
)
