package com.khalid.celltowerexplorer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * قراءة واحدة مسجلة أثناء حركة المستخدم: موقعه الحالي + بيانات الخلية المتصل بها حالياً.
 */
@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val cellId: String?,
    val pci: Int?,
    val tac: Int?,
    val signalStrength: Int?,
    val networkType: String?,
    val timestamp: Long
)
