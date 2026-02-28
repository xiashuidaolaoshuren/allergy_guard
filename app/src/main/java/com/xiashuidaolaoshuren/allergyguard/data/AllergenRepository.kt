package com.xiashuidaolaoshuren.allergyguard.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AllergenRepository {
    val allergens: Flow<List<Allergen>>

    suspend fun setAllergenEnabled(id: String, isEnabled: Boolean)
    suspend fun customAllergenNameExists(name: String): Boolean
    suspend fun addCustomAllergen(name: String): Boolean
}

class RoomAllergenRepository(
    private val allergenDao: AllergenDao
) : AllergenRepository {
    override val allergens: Flow<List<Allergen>> = allergenDao.getAllAllergens()

    override suspend fun setAllergenEnabled(id: String, isEnabled: Boolean) {
        allergenDao.updateAllergenEnabledStatus(id, isEnabled)
    }

    override suspend fun customAllergenNameExists(name: String): Boolean {
        return allergenDao.allergenNameExists(name)
    }

    override suspend fun addCustomAllergen(name: String): Boolean {
        val allergen = Allergen(
            id = UUID.randomUUID().toString(),
            name = name,
            isEnabled = true,
            isCustom = true
        )
        return allergenDao.insertAllergenIgnore(allergen) != -1L
    }
}