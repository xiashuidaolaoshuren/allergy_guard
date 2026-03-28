package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllergenAliasDao {
    @Query("SELECT * FROM allergen_aliases ORDER BY alias ASC")
    fun getAllAliases(): Flow<List<AllergenAlias>>

    @Query("SELECT * FROM allergen_aliases WHERE allergenId = :allergenId ORDER BY alias ASC")
    fun getAliasesForAllergen(allergenId: String): Flow<List<AllergenAlias>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlias(alias: AllergenAlias): Long

    @Query("DELETE FROM allergen_aliases WHERE id = :id")
    suspend fun deleteAliasById(id: String)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM allergen_aliases WHERE allergenId = :allergenId AND TRIM(LOWER(alias)) = TRIM(LOWER(:alias)) LIMIT 1)"
    )
    suspend fun aliasExists(allergenId: String, alias: String): Boolean
}
