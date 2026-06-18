package com.khalid.celltowerexplorer.data

import com.khalid.celltowerexplorer.network.OpenCellIdApi
import com.khalid.celltowerexplorer.utils.ConfidenceCalculator
import com.khalid.celltowerexplorer.utils.OperatorLookup
import kotlin.math.cos

/**
 * منطق "البحث المحلي أولاً" (item 14):
 * 1) يبحث في قاعدة البيانات المحلية ضمن صندوق محيط بموقع المستخدم.
 * 2) يستعلم من OpenCellID ضمن نفس النطاق ويضيف/يحدّث النتائج محلياً.
 * 3) يعيد القائمة المجمّعة من قاعدة البيانات المحلية بعد التحديث.
 */
class TowerRepository(
    private val towerDao: TowerDao,
    private val observationDao: ObservationDao,
    private val openCellIdApi: OpenCellIdApi,
    private val openCellIdApiKey: String
) {

    suspend fun findTowersNear(lat: Double, lon: Double, radiusMeters: Double): List<TowerEntity> {
        val box = boundingBox(lat, lon, radiusMeters)

        if (openCellIdApiKey.isNotBlank()) {
            try {
                val bboxParam = "${box.latMin},${box.lonMin},${box.latMax},${box.lonMax}"
                val response = openCellIdApi.getCellsInArea(
                    apiKey = openCellIdApiKey,
                    bbox = bboxParam,
                    limit = 50
                )
                response.cells.orEmpty().forEach { cell -> saveRemoteCell(cell) }
            } catch (e: Exception) {
                // فشل الاتصال بـ OpenCellID (لا إنترنت، تجاوز الحد اليومي، ...).
                // نتجاهل الخطأ ونعرض ما هو متوفر محلياً فقط دون تعطيل التطبيق.
            }
        }

        return towerDao.getInBoundingBox(box.latMin, box.latMax, box.lonMin, box.lonMax)
    }

    private suspend fun saveRemoteCell(cell: com.khalid.celltowerexplorer.network.OpenCellIdCell) {
        val cellIdKey = "${cell.mcc ?: 0}-${cell.mnc ?: 0}-${cell.lac ?: cell.tac ?: 0}-${cell.cellid ?: 0}"
        val existing = towerDao.getByCellId(cellIdKey)
        val observationCount = observationDao.countByCellId(cellIdKey)
        val confidence = ConfidenceCalculator.calculate(
            source = ConfidenceCalculator.SOURCE_OPENCELLID,
            localObservationCount = observationCount,
            isLocalEstimateConsistent = false
        )
        towerDao.upsert(
            TowerEntity(
                cellId = cellIdKey,
                operator = existing?.operator ?: OperatorLookup.operatorName(cell.mcc, cell.mnc),
                latitude = cell.lat,
                longitude = cell.lon,
                source = ConfidenceCalculator.SOURCE_OPENCELLID,
                confidenceScore = confidence,
                lastSeen = System.currentTimeMillis(),
                networkType = mapRadioToNetworkType(cell.radio)
            )
        )
    }

    private fun mapRadioToNetworkType(radio: String?): String? = when (radio?.uppercase()) {
        "GSM", "GPRS", "EDGE" -> "2G"
        "UMTS", "HSPA", "HSDPA", "HSUPA", "HSPA+", "TDSCDMA" -> "3G"
        "LTE", "LTECATM" -> "4G"
        "NR" -> "5G"
        else -> null
    }

    private data class BoundingBox(
        val latMin: Double,
        val lonMin: Double,
        val latMax: Double,
        val lonMax: Double
    )

    private fun boundingBox(lat: Double, lon: Double, radiusMeters: Double): BoundingBox {
        val latDelta = radiusMeters / 111_320.0 // درجة عرض واحدة ≈ 111.32 كم
        val lonDelta = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.0001))
        return BoundingBox(
            latMin = lat - latDelta,
            lonMin = lon - lonDelta,
            latMax = lat + latDelta,
            lonMax = lon + lonDelta
        )
    }
}
