package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAllergenRepositoryInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomAllergenRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomAllergenRepository(database.allergenDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addCustomAllergen_insertsAsEnabledCustomByDefault() = runBlocking {
        val added = repository.addCustomAllergen("Paprika")
        assertTrue(added)

        val inserted = repository.allergens.first().firstOrNull { it.name == "Paprika" }
        assertNotNull(inserted)
        assertTrue(inserted!!.isCustom)
        assertTrue(inserted.isEnabled)
    }

    @Test
    fun customAllergenNameExists_isCaseInsensitiveAndTrimAware() = runBlocking {
        repository.addCustomAllergen("Peanut")

        assertTrue(repository.customAllergenNameExists(" peanut "))
        assertTrue(repository.customAllergenNameExists("PEANUT"))
        assertFalse(repository.customAllergenNameExists("Peanuts"))
    }

    @Test
    fun addCustomAllergen_returnsFalseForExactDuplicateName() = runBlocking {
        val firstInsert = repository.addCustomAllergen("Sesame")
        val secondInsert = repository.addCustomAllergen("Sesame")

        assertTrue(firstInsert)
        assertFalse(secondInsert)
        assertEquals(1, repository.allergens.first().count { it.name == "Sesame" })
    }
}