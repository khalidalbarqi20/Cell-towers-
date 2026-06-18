package com.khalid.celltowerexplorer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class OperatorCount(val operator: String, val count: Int)

@Dao
interface TowerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tower: TowerEntity)

    @Query("SELECT * FROM towers WHERE cellId = :cellId LIMIT 1")
    suspend fun getByCellId(cellId: String): TowerEntity?

    @Query(
        """
        SELECT * FROM towers
        WHERE latitude BETWEEN :latMin AND :latMax
        AND longitude BETWEEN :lonMin AND :lonMax
        """
    )
    suspend fun getInBoundingBox(
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double
    ): List<TowerEntity>

    @Query("SELECT * FROM towers")
    suspend fun getAll(): List<TowerEntity>

    @Query("SELECT COUNT(*) FROM towers")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM towers WHERE networkType = :type")
    suspend fun countByNetworkType(type: String): Int

    @Query("SELECT operator AS operator, COUNT(*) AS count FROM towers WHERE operator IS NOT NULL GROUP BY operator")
    suspend fun countByOperator(): List<OperatorCount>
}
