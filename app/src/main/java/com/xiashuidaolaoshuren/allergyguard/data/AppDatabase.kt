package com.xiashuidaolaoshuren.allergyguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

@Database(
    entities = [Allergen::class, ScanResult::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun allergenDao(): AllergenDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        private const val DATABASE_NAME = "allergy_guard.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedCommonAllergens(db)
                    }
                })
                .build()
        }

        private fun seedCommonAllergens(db: SupportSQLiteDatabase) {
            val commonAllergens = listOf(
                "Milk",
                "Eggs",
                "Fish",
                "Crustacean Shellfish",
                "Tree Nuts",
                "Peanuts",
                "Wheat",
                "Soybeans",
                "Sesame"
            )

            commonAllergens.forEach { name ->
                val id = deterministicSeedId(name)
                val safeName = name.replace("'", "''")
                db.execSQL(
                    "INSERT INTO allergens (id, name, isEnabled, isCustom) VALUES ('$id', '$safeName', 1, 0)"
                )
            }
        }

        private fun deterministicSeedId(name: String): String {
            val normalized = name
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
            return "builtin_$normalized"
        }
    }
}
