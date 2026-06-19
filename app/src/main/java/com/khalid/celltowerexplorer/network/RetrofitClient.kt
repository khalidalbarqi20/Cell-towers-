package com.khalid.celltowerexplorer.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val OPENCELLID_BASE_URL = "https://opencellid.org/"
    private const val BEACONDB_BASE_URL = "https://beacondb.net/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val openCellIdApi: OpenCellIdApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPENCELLID_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenCellIdApi::class.java)
    }

    val beaconDbApi: BeaconDbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BEACONDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BeaconDbApi::class.java)
    }
}
