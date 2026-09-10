package com.wisdomtower.academy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wisdomtower.academy.data.local.entity.UserEnrollmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrollmentDao {

    @Query("SELECT * FROM user_enrollments WHERE userId = :userId")
    fun getUserEnrollments(userId: String): Flow<List<UserEnrollmentEntity>>

    @Query("SELECT * FROM user_enrollments WHERE userId = :userId AND packageId = :packageId LIMIT 1")
    suspend fun getEnrollment(userId: String, packageId: String): UserEnrollmentEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM user_enrollments WHERE userId = :userId AND packageId = :packageId AND isVerified = 1)")
    fun isPackageUnlocked(userId: String, packageId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: UserEnrollmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollments(enrollments: List<UserEnrollmentEntity>)

    @Query("DELETE FROM user_enrollments WHERE userId = :userId")
    suspend fun deleteEnrollmentsForUser(userId: String)
}
