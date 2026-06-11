package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnProfileDao {
    @Query("SELECT * FROM vpn_profiles ORDER BY id DESC")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    @Query("SELECT * FROM vpn_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<VpnProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile)

    @Update
    suspend fun updateProfile(profile: VpnProfile)

    @Delete
    suspend fun deleteProfile(profile: VpnProfile)

    @Query("UPDATE vpn_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun setActiveProfile(profileId: Int) {
        deactivateAll()
        updateActiveStatus(profileId, true)
    }

    @Query("UPDATE vpn_profiles SET isActive = :isActive WHERE id = :profileId")
    suspend fun updateActiveStatus(profileId: Int, isActive: Boolean)
}
