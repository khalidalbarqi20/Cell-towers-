package com.khalid.celltowerexplorer.data

import com.khalid.celltowerexplorer.network.OpenCellIdApi
import com.khalid.celltowerexplorer.utils.ConfidenceCalculator
import com.khalid.celltowerexplorer.utils.OperatorLookup
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.cos

/** نتيجة البحث عن الأبراج: قائمة الأبراج، مع رسالة خطأ تشخيصية اختيارية إن فشل الاتصال بـ OpenCellID. */
data class TowerSearchResult(
    val towers: List<TowerEntity>,
    val diagnosticMessage: String? = null
)

/**
 * منطق "البحث المحلي أولاً" (item 14):
 * 1) يبحث في قاعدة البيانات المحلية ضمن صندوق محيط بموقع المستخدم.
 * 2) يستعلم من OpenCellID ضمن نفس النطاق ويضيف/يحدّث النتائج محلياً.
 * 3) يعيد القائمة المجمّعة من قاعدة البيانات المحلية بعد التحديث، مع رسالة
 *    تشخيصية إن فشل الاتصال بـ OpenCellID أو لم يُعِد أي نتائج، بدل تجاهل
 *    الخطأ بصمت كما كان سابقاً.
 */
class TowerRepository(
    private val towerDao: TowerDao,
    private val observationDao: ObservationDao,
    private val openCellIdApi: OpenCellIdApi,
    private val openCellIdApiKey: String
) {

    suspend fun findTowersNear(lat: Double, lon: Double, radiusMeters: Double): TowerSearchResult {
        val box = boundingBox(lat, lon, radiusMeters)
        var diagnosticMessage: String? = null

        if (openCellIdApiKey.isBlank()) {
            diagnosticMessage = "لم يتم ضبط مفتاح OpenCellID API. تحقق من local.properties أو GitHub Secret."
        } else {
            try {
                val bboxParam = "${box.latMin},${box.lonMin},${box.latMax},${box.lonMax}"
                val response = openCellIdApi.getCellsInArea(
                    apiKey = openCellIdApiKey,
                    bbox = bboxParam,
                    limit = 50
                )
                val cells = response.cells.orEmpty()
                cells.forEach { cell -> saveRemoteCell(cell) }
                if (cells.isEmpty()) {
                    diagnosticMessage = "لا توجد بيانات أبراج لدى OpenCellID ضمن هذا النطاق (قد تكون المنطقة غير مغطاة في قاعدة بياناتهم المعتمدة على مساهمات المستخدمين)."
                }
            } catch (e: HttpException) {
                diagnosticMessage = when (e.code()) {
                    401, 403 -> "مفتاح OpenCellID غير صالح أو مرفوض (HTTP ${e.code()})."
                    429 -> "تجاوزت الحد اليومي لطلبات OpenCellID (HTTP 429)."
                    else -> "خطأ من خادم OpenCellID (HTTP ${e.code()})."
                }
            } catch (e: IOException) {
                diagnosticMessage = "تعذر الاتصال بالإنترنت أو بخادم OpenCellID: ${e.message ?: "خطأ شبكة"}."
            } catch (e: Exception) {
                diagnosticMessage = "خطأ غير متوقع أثناء جلب الأبراج: ${e.message ?: "غير معروف"}."
            }
        }

        val localTowers = towerDao.getInBoundingBox(box.latMin, box.latMax, box.lonMin, box.lonMax)
        return TowerSearchResult(towers = localTowers, diagnosticMessage = diagnosticMessage)
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
