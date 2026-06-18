package com.khalid.celltowerexplorer.data

import androidx.room.Entity

/**
 * برج معروف، إما من OpenCellID أو من تقدير محلي (UserEstimated) مستقبلاً.
 * cellId هنا هو مفتاح مركّب نصي بصيغة "mcc-mnc-lac-cellid" لضمان تفرّده.
 */
@Entity(tableName = "towers", primaryKeys = ["cellId"])
data class TowerEntity(
    val cellId: String,
    val operator: String?,
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val confidenceScore: Int,
    val lastSeen: Long,
    val networkType: String? = null
)
