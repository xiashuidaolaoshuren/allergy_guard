package com.xiashuidaolaoshuren.allergyguard.data

import kotlinx.coroutines.flow.Flow

interface AllergenRepository {
    val allergens: Flow<List<Allergen>>

    suspend fun setAllergenEnabled(id: String, isEnabled: Boolean)
}

class RoomAllergenRepository(
    private val allergenDao: AllergenDao
) : AllergenRepository {
    override val allergens: Flow<List<Allergen>> = allergenDao.getAllAllergens()

    override suspend fun setAllergenEnabled(id: String, isEnabled: Boolean) {
        allergenDao.updateAllergenEnabledStatus(id, isEnabled)
    }
}