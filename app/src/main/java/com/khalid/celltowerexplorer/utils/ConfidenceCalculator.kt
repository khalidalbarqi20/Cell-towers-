package com.khalid.celltowerexplorer.utils

/**
 * يحسب "درجة الثقة" (Confidence Score) لكل برج، من 0 إلى 100،
 * حسب مصدر البيانات وعدد المشاهدات المحلية المتراكمة له.
 */
object ConfidenceCalculator {
    const val SOURCE_OPENCELLID = "OpenCellID"
    const val SOURCE_USER_ESTIMATED = "UserEstimated"

    private const val BASE_OPENCELLID_SCORE = 50
    private const val MAX_OPENCELLID_SCORE = 80
    private const val CONSISTENT_LOCAL_ESTIMATE_SCORE = 95
    private const val INCONSISTENT_LOCAL_ESTIMATE_SCORE = 60

    /**
     * @param source مصدر بيانات البرج (OpenCellID أو UserEstimated)
     * @param localObservationCount عدد القراءات المحلية المسجلة لهذا البرج
     * @param isLocalEstimateConsistent عند التقدير المحلي: هل القراءات متقاربة ومتطابقة؟
     */
    fun calculate(
        source: String,
        localObservationCount: Int,
        isLocalEstimateConsistent: Boolean
    ): Int {
        return when (source) {
            SOURCE_OPENCELLID -> {
                // كل 100 مشاهدة محلية تضيف ما يقارب 30 نقطة، بحد أقصى 80
                val bonus = ((localObservationCount.coerceAtMost(100)) * 0.3).toInt()
                (BASE_OPENCELLID_SCORE + bonus).coerceAtMost(MAX_OPENCELLID_SCORE)
            }
            SOURCE_USER_ESTIMATED -> {
                if (isLocalEstimateConsistent) CONSISTENT_LOCAL_ESTIMATE_SCORE
                else INCONSISTENT_LOCAL_ESTIMATE_SCORE
            }
            else -> BASE_OPENCELLID_SCORE
        }
    }
}
