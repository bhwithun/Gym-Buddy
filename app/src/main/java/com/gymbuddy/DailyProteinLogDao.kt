package com.gymbuddy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DailyProteinLogDao {
    @Query("SELECT * FROM daily_protein_logs WHERE date = :date")
    suspend fun getLogForDate(date: String): DailyProteinLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: DailyProteinLog)

    @Update
    suspend fun update(log: DailyProteinLog)

    @Query("DELETE FROM daily_protein_logs WHERE date < :date")
    suspend fun deleteLogsBeforeDate(date: String)
}