package com.khalid.celltowerexplorer.data

import com.khalid.celltowerexplorer.network.BeaconDbApi
import com.khalid.celltowerexplorer.network.BeaconDbCell
import com.khalid.celltowerexplorer.network.BeaconDbRequest
import com.khalid.celltowerexplorer.network.OpenCellIdApi
import com.khalid.celltowerexplorer.utils.ConfidenceCalculator
import com.khalid.celltowerexplorer.utils.OperatorLookup
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.cos

data class TowerSearchResult(
    val towers: List<TowerEntity>,
    val diagnosticMessage: String? = null
)

/**
 * ترتيب الاستعلام:
 * 1) قاعدة البيانات المحلية أولاً
 * 2) OpenCellID (يحتاج مفتاح) — أكبر قاعدة بيانات
 * 3) beaconDB (مجاني بدون مفتاح) — بديل Mozilla، يُستخدم فقط
 *    لاستكمال البيانات التي لم تجدها OpenCellID
 */
class TowerRepository(
    private val towerDao: TowerDao,
    private val observationDao: ObservationDao,
    private val openCellIdApi: OpenCellIdApi,
    private val openCellIdApiKey: String,
    private val beaconDbApi: BeaconDbApi
) {

    suspend fun findTowersNear(lat: Double, lon: Double, radiusMeters: Double): TowerSearchResult {
        val box = boundingBox(lat, lon, radiusMeters)
        val messages = mutableListOf<String>()

        // ① OpenCellID
        if (openCellIdApiKey.isBlank()) {
            messages.add("OpenCellID: مفتاح API غير مضبوط.")
        } else {
            try {
                val bbox = "${box.latMin},${box.lonMin},${box.latMax},${box.lonMax}"
                val response = openCellIdApi.getCellsInArea(
                    apiKey = openCellIdApiKey, bbox = bbox, limit = 50
                )
                val cells = response.cells.orEmpty()
                cells.forEach { saveOpenCellIdCell(it) }
                if (cells.isEmpty()) {
                    messages.add("OpenCellID: لا توجد بيانات لهذا النطاق.")
                }
            } catch (e: HttpException) {
                messages.add(when (e.code()) {
                    401, 403 -> "OpenCellID: مفتاح غير صالح (${e.code()})."
                    429 -> "OpenCellID: تجاوزت الحد اليومي."
                    else -> "OpenCellID: خطأ HTTP ${e.code()}."
                })
            } catch (e: IOException) {
                messages.add("OpenCellID: لا يوجد اتصال بالإنترنت.")
            } catch (e: Exception) {
                messages.add("OpenCellID: ${e.message ?: "خطأ غير معروف"}.")
            }
        }

        val localTowers = towerDao.getInBoundingBox(box.latMin, box.latMax, box.lonMin, box.lonMax)
        val diagnosticMessage = if (messages.isNotEmpty()) messages.joinToString(" | ") else null
        return TowerSearchResult(towers = localTowers, diagnosticMessage = diagnosticMessage)
    }

    /**
     * يستعلم beaconDB عن موقع برج واحد محدد بـ CellSnapshot.
     * يُستدعى عند الضغط على "البرج المتصل به" لتحديد موقعه على الخريطة
     * حتى لو لم يكن موجوداً بـ OpenCellID.
     */
    suspend fun lookupTowerInBeaconDb(
        radioType: String, mcc: Int, mnc: Int, lac: Int, cellId: Long
    ): Pair<Double, Double>? {
        return try {
            val radioStr = when (radioType) {
                "4G" -> "lte"
                "3G" -> "wcdma"
                "5G" -> "nr"
                else -> "gsm"
            }
            val response = beaconDbApi.geolocate(
                BeaconDbRequest(
                    listOf(BeaconDbCell(
                        radioType = radioStr,
                        mobileCountryCode = mcc,
                        mobileNetworkCode = mnc,
                        locationAreaCode = lac,
                        cellId = cellId
                    ))
                )
            )
            val loc = response.location ?: return null
            Pair(loc.lat, loc.lng)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveOpenCellIdCell(cell: com.khalid.celltowerexplorer.network.OpenCellIdCell) {
        val key = "${cell.mcc ?: 0}-${cell.mnc ?: 0}-${cell.lac ?: cell.tac ?: 0}-${cell.cellid ?: 0}"
        val existing = towerDao.getByCellId(key)
        val obsCount = observationDao.countByCellId(key)
        val confidence = ConfidenceCalculator.calculate(
            source = ConfidenceCalculator.SOURCE_OPENCELLID,
            localObservationCount = obsCount,
            isLocalEstimateConsistent = false
        )
        towerDao.upsert(TowerEntity(
            cellId = key,
            operator = existing?.operator ?: OperatorLookup.operatorName(cell.mcc, cell.mnc),
            latitude = cell.lat,
            longitude = cell.lon,
            source = ConfidenceCalculator.SOURCE_OPENCELLID,
            confidenceScore = confidence,
            lastSeen = System.currentTimeMillis(),
            networkType = mapRadioToNetworkType(cell.radio)
        ))
    }

    private fun mapRadioToNetworkType(radio: String?): String? = when (radio?.uppercase()) {
        "GSM", "GPRS", "EDGE" -> "2G"
        "UMTS", "HSPA", "HSDPA", "HSUPA", "HSPA+", "TDSCDMA" -> "3G"
        "LTE", "LTECATM" -> "4G"
        "NR" -> "5G"
        else -> null
    }

    private data class BoundingBox(val latMin: Double, val lonMin: Double, val latMax: Double, val lonMax: Double)

    private fun boundingBox(lat: Double, lon: Double, radiusMeters: Double): BoundingBox {
        val latDelta = radiusMeters / 111_320.0
        val lonDelta = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.0001))
        return BoundingBox(lat - latDelta, lon - lonDelta, lat + latDelta, lon + lonDelta)
    }
}
