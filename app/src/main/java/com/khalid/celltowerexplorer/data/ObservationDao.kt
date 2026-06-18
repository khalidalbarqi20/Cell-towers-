package com.khalid.celltowerexplorer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ObservationDao {

    @Insert
    suspend fun insert(observation: ObservationEntity)

    @Query("SELECT * FROM observations WHERE cellId = :cellId ORDER BY timestamp DESC")
    suspend fun getByCellId(cellId: String): List<ObservationEntity>

    @Query("SELECT COUNT(*) FROM observations WHERE cellId = :cellId")
    suspend fun countByCellId(cellId: String): Int

    @Query("SELECT COUNT(*) FROM observations")
    suspend fun totalCount(): Int

    @Query("SELECT * FROM observations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ObservationEntity>
}
