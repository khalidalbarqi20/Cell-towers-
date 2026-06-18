package com.khalid.celltowerexplorer.utils

/**
 * يحوّل رمزي MCC/MNC إلى اسم المشغّل، خاصة لشبكات السعودية (MCC=420)
 * نظراً لأن OpenCellID لا يعيد اسم المشغّل مباشرة، بل رمزَيه فقط.
 */
object OperatorLookup {

    private val saudiOperators = mapOf(
        1 to "STC",
        3 to "Mobily",
        4 to "Zain",
        5 to "Virgin Mobile",
        6 to "Lebara",
        7 to "Zain"
    )

    fun operatorName(mcc: Int?, mnc: Int?): String? {
        if (mcc == null || mnc == null) return null
        return when (mcc) {
            420 -> saudiOperators[mnc] ?: "مشغّل سعودي (MNC $mnc)"
            else -> null // يمكن إضافة دول أخرى مستقبلاً
        }
    }
}
