package com.khalid.celltowerexplorer.network

/**
 * نموذج استجابة OpenCellID لنقطة النهاية cell/getInArea بصيغة JSON.
 * المرجع: https://wiki.opencellid.org/wiki/API
 */
data class OpenCellIdResponse(
    val count: Int? = null,
    val cells: List<OpenCellIdCell>? = null
)

data class OpenCellIdCell(
    val lon: Double,
    val lat: Double,
    val mcc: Int? = null,
    val mnc: Int? = null,
    val lac: Int? = null,
    val cellid: Long? = null,
    val averageSignalStrength: Int? = null,
    val range: Int? = null,
    val samples: Int? = null,
    val changeable: Boolean? = null,
    val radio: String? = null,
    val tac: Int? = null
)
