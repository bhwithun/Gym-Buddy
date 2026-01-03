package com.gymbuddy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs WHERE date = :date")
    fun getByDate(date: String): WorkoutLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: WorkoutLogEntity)

    @Query("DELETE FROM workout_logs WHERE date = :date")
    fun deleteByDate(date: String)

    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun getAll(): List<WorkoutLogEntity>
}
