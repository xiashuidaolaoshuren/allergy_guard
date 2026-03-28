package com.xiashuidaolaoshuren.allergyguard.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AllergenAliasRepository {
    fun getAllAliases(): Flow<List<AllergenAlias>>
    fun getAliasesForAllergen(allergenId: String): Flow<List<AllergenAlias>>
    suspend fun addAlias(allergenId: String, alias: String): Boolean
    suspend fun deleteAlias(aliasId: String)
    suspend fun aliasExists(allergenId: String, alias: String): Boolean
}

class RoomAllergenAliasRepository(
    private val aliasDao: AllergenAliasDao
) : AllergenAliasRepository {
    override fun getAllAliases(): Flow<List<AllergenAlias>> = aliasDao.getAllAliases()

    override fun getAliasesForAllergen(allergenId: String): Flow<List<AllergenAlias>> =
        aliasDao.getAliasesForAllergen(allergenId)

    override suspend fun addAlias(allergenId: String, alias: String): Boolean {
        val entry = AllergenAlias(
            id = UUID.randomUUID().toString(),
            allergenId = allergenId,
            alias = alias
        )
        return aliasDao.insertAlias(entry) != -1L
    }

    override suspend fun deleteAlias(aliasId: String) {
        aliasDao.deleteAliasById(aliasId)
    }

    override suspend fun aliasExists(allergenId: String, alias: String): Boolean =
        aliasDao.aliasExists(allergenId, alias)
}
