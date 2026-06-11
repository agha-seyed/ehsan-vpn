package com.example.data

import kotlinx.coroutines.flow.Flow

class VpnRepository(private val vpnProfileDao: VpnProfileDao) {
    val allProfiles: Flow<List<VpnProfile>> = vpnProfileDao.getAllProfiles()
    val activeProfile: Flow<VpnProfile?> = vpnProfileDao.getActiveProfile()

    suspend fun insert(profile: VpnProfile) {
        vpnProfileDao.insertProfile(profile)
    }

    suspend fun update(profile: VpnProfile) {
        vpnProfileDao.updateProfile(profile)
    }

    suspend fun delete(profile: VpnProfile) {
        vpnProfileDao.deleteProfile(profile)
    }

    suspend fun setActiveProfile(profileId: Int) {
        vpnProfileDao.setActiveProfile(profileId)
    }
}
