package com.khalid.celltowerexplorer.network

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenCellIdApi {

    /**
     * يبحث عن الأبراج ضمن مستطيل جغرافي (Bounding Box).
     * ترتيب BBOX حسب توثيق OpenCellID: latMin,lonMin,latMax,lonMax
     * الحد الأقصى لكل طلب 50 برجاً (limit) — استخدم offset لجلب بقية النتائج عند الحاجة.
     */
    @GET("cell/getInArea")
    suspend fun getCellsInArea(
        @Query("key") apiKey: String,
        @Query("BBOX") bbox: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): OpenCellIdResponse
}
