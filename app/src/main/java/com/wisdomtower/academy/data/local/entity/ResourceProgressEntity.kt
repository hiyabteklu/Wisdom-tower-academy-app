package com.wisdomtower.academy.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "resource_progress",
    primaryKeys = ["resourceId", "userId"]
)
data class ResourceProgressEntity(
    val resourceId: String,
    val userId: String,
    val progressPercent: Int = 0,
    val lastPageRead: Int = 1,
    val totalPages: Int = 1,
    val lastScore: Int = 0,
    val totalQuestions: Int = 0,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
