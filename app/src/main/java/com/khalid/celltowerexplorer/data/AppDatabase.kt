package com.khalid.celltowerexplorer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ObservationEntity::class, TowerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun observationDao(): ObservationDao
    abstract fun towerDao(): TowerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE observations ADD COLUMN mcc INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN mnc INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN operator TEXT")
                db.execSQL("ALTER TABLE observations ADD COLUMN rsrp INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN rsrq INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN sinr INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN earfcn INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN nrarfcn INTEGER")
                db.execSQL("ALTER TABLE observations ADD COLUMN band TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cell_explorer.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
