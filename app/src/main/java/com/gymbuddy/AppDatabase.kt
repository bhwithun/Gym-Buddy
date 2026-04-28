package com.gymbuddy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RoutineDayEntity::class, WorkoutLogEntity::class, ProteinSettings::class, DailyProteinLog::class], version = 3)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun proteinSettingsDao(): ProteinSettingsDao
    abstract fun dailyProteinLogDao(): DailyProteinLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gymbuddy_db"
                )
                .fallbackToDestructiveMigration() // Allow destructive migration for schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
