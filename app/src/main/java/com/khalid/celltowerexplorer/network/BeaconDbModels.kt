package com.khalid.celltowerexplorer.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// ========== نماذج طلب beaconDB (يتبع بروتوكول ichnaea/Mozilla) ==========

data class BeaconDbRequest(
    val cellTowers: List<BeaconDbCell>
)

data class BeaconDbCell(
    val radioType: String,   // "lte" / "gsm" / "wcdma" / "nr"
    val mobileCountryCode: Int,
    val mobileNetworkCode: Int,
    val locationAreaCode: Int,
    val cellId: Long,
    val psc: Int? = null
)

// ========== نماذج استجابة beaconDB ==========

data class BeaconDbResponse(
    val location: BeaconDbLocation?,
    val accuracy: Double?
)

data class BeaconDbLocation(
    val lat: Double,
    val lng: Double
)

// ========== واجهة Retrofit ==========

interface BeaconDbApi {
    /**
     * يعطي الموقع التقريبي للبرج من MCC+MNC+LAC+CellID.
     * مجاني بالكامل — لا يحتاج مفتاح API.
     * مرجع: https://beacondb.net/v1/geolocate (يتبع ichnaea protocol)
     */
    @Headers("Content-Type: application/json")
    @POST("v1/geolocate")
    suspend fun geolocate(@Body request: BeaconDbRequest): BeaconDbResponse
}
