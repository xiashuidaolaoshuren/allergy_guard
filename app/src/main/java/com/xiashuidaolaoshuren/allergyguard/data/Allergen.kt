package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "allergens",
    indices = [Index(value = ["name"], unique = true)]
)
data class Allergen(
    @PrimaryKey val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false
)
