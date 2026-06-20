package com.khalid.celltowerexplorer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val cellId: String?,
    val pci: Int?,
    val tac: Int?,
    val mcc: Int?,
    val mnc: Int?,
    val operator: String?,
    val signalStrength: Int?,
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val earfcn: Int?,
    val nrarfcn: Int?,
    val band: String?,
    val networkType: String?,
    val timestamp: Long
)
