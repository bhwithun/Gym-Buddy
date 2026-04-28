package com.gymbuddy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProteinSettingsDao {
    @Query("SELECT * FROM protein_settings WHERE id = 1")
    suspend fun getSettings(): ProteinSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ProteinSettings)

    @Update
    suspend fun update(settings: ProteinSettings)
}