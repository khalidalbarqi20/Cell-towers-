package com.khalid.celltowerexplorer.telephony

/**
 * لقطة موحّدة لمعلومات خلية واحدة، بصرف النظر عن نوع الشبكة (2G/3G/4G/5G).
 * كل الحقول (إلا isRegistered و networkType) قابلة لأن تكون null لأن
 * بعض الأجهزة/إصدارات أندرويد لا تكشف عنها (PCI، الترددات، NRARFCN...).
 */
data class CellSnapshot(
    val isRegistered: Boolean,
    val networkType: String,
    val cellId: Long? = null,
    val pci: Int? = null,
    val areaCode: Int? = null,
    val areaCodeLabel: String = "TAC",
    val mcc: Int? = null,
    val mnc: Int? = null,
    val signalStrengthDbm: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val rssi: Int? = null,
    val sinr: Int? = null,
    val earfcn: Int? = null,
    val nrarfcn: Int? = null,
    val operatorName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
