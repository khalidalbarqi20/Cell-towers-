package com.khalid.celltowerexplorer.ui

import android.app.Application
import android.os.Build
import android.telephony.CellInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.khalid.celltowerexplorer.BuildConfig
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.data.TowerRepository
import com.khalid.celltowerexplorer.location.LocationRepository
import com.khalid.celltowerexplorer.location.UserLocation
import com.khalid.celltowerexplorer.network.RetrofitClient
import com.khalid.celltowerexplorer.telephony.CellInfoReader
import com.khalid.celltowerexplorer.telephony.CellSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val cellInfoReader = CellInfoReader(application)
    private val locationRepo = LocationRepository(application)
    private val telephonyManager = application.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
    private val repository = TowerRepository(
        towerDao = db.towerDao(), observationDao = db.observationDao(),
        openCellIdApi = RetrofitClient.openCellIdApi,
        openCellIdApiKey = BuildConfig.OPENCELLID_API_KEY,
        beaconDbApi = RetrofitClient.beaconDbApi)

    val registeredCell  = MutableLiveData<CellSnapshot?>(null)
    val allVisibleCells = MutableLiveData<List<CellSnapshot>>(emptyList())
    val userLocation    = MutableLiveData<UserLocation?>(null)
    val towers          = MutableLiveData<List<TowerEntity>>(emptyList())
    val isLoading       = MutableLiveData(false)
    val errorMessage    = MutableLiveData<String?>(null)
    private var allTowersCache = emptyList<TowerEntity>()
    private var selOp: String? = null
    private var selNt: String? = null

    init { startCellPolling(); startLocationUpdates() }

    private fun startCellPolling() {
        viewModelScope.launch {
            while (isActive) { refreshCellInfo(); delay(3000) }
        }
    }

    private fun getDataTm(): TelephonyManager = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                telephonyManager.createForSubscriptionId(subId)
            else telephonyManager
        } else telephonyManager
    } catch (e: Exception) { telephonyManager }

    private suspend fun refreshCellInfo() {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    getDataTm().requestCellInfoUpdate(Executor { it.run() },
                        object : TelephonyManager.CellInfoCallback() {
                            override fun onCellInfo(c: MutableList<CellInfo>) {}
                            override fun onError(e: Int, d: Throwable?) {}
                        })
                    delay(300)
                } catch (_: Exception) {}
            }
            val all = cellInfoReader.readAllCells()
            val pri = mapOf("5G" to 4,"4G" to 3,"3G" to 2,"2G" to 1)
            val reg = all.filter { it.isRegistered }.maxByOrNull { pri[it.networkType] ?: 0 }
            withContext(Dispatchers.Main) { registeredCell.value = reg; allVisibleCells.value = all }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                locationRepo.getLastKnownLocation()?.let { userLocation.value = it }
                locationRepo.locationUpdates(5000).collect { userLocation.value = it }
            } catch (_: Exception) {}
        }
    }

    fun searchTowers(lat: Double, lon: Double, r: Double) {
        viewModelScope.launch {
            isLoading.value = true; errorMessage.value = null
            try {
                val result = repository.findTowersNear(lat, lon, r)
                allTowersCache = result.towers
                towers.value = applyFilters(allTowersCache)
                errorMessage.value = result.diagnosticMessage
            } catch (e: Exception) { errorMessage.value = "خطأ: ${e.message}" }
            finally { isLoading.value = false }
        }
    }

    fun setFilters(op: String?, nt: String?) { selOp = op; selNt = nt; towers.value = applyFilters(allTowersCache) }
    private fun applyFilters(list: List<TowerEntity>) = list.filter {
        (selOp == null || it.operator == selOp) && (selNt == null || it.networkType == selNt)
    }
}
