package com.wisdomtower.academy.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "user_enrollments",
    primaryKeys = ["userId", "packageId"]
)
data class UserEnrollmentEntity(
    val userId: String,
    val packageId: String,
    val packageName: String,
    val isFreeTier: Boolean,
    val isVerified: Boolean = true,
    val enrolledAt: Long = System.currentTimeMillis()
)
