package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllergenDao {
    @Query("SELECT * FROM allergens ORDER BY name ASC")
    fun getAllAllergens(): Flow<List<Allergen>>

    @Query("SELECT * FROM allergens WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledAllergens(): Flow<List<Allergen>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergen(allergen: Allergen)

    @Query("UPDATE allergens SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateAllergenEnabledStatus(id: String, isEnabled: Boolean)

    @Query("DELETE FROM allergens WHERE id = :id")
    suspend fun deleteAllergenById(id: String)
}
