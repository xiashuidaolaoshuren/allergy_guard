package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "allergen_aliases",
    foreignKeys = [
        ForeignKey(
            entity = Allergen::class,
            parentColumns = ["id"],
            childColumns = ["allergenId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["allergenId"])]
)
data class AllergenAlias(
    @PrimaryKey val id: String,
    val allergenId: String,
    val alias: String
)
