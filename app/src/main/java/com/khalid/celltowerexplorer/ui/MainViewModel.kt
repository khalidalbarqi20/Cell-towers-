package com.khalid.celltowerexplorer.ui

import android.app.Application
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val cellInfoReader = CellInfoReader(application)
    private val locationRepo = LocationRepository(application)
    private val repository = TowerRepository(
        towerDao = db.towerDao(),
        observationDao = db.observationDao(),
        openCellIdApi = RetrofitClient.openCellIdApi,
        openCellIdApiKey = BuildConfig.OPENCELLID_API_KEY,
        beaconDbApi = RetrofitClient.beaconDbApi
    )

    val registeredCell = MutableLiveData<CellSnapshot?>(null)
    val userLocation   = MutableLiveData<UserLocation?>(null)
    val towers         = MutableLiveData<List<TowerEntity>>(emptyList())
    val isLoading      = MutableLiveData(false)
    val errorMessage   = MutableLiveData<String?>(null)

    private var allTowersCache = emptyList<TowerEntity>()
    private var selectedOperator: String? = null
    private var selectedNetworkType: String? = null

    init {
        startCellPolling()
        startLocationUpdates()
    }

    private fun startCellPolling() {
        viewModelScope.launch {
            while (isActive) {
                registeredCell.value = cellInfoReader.readRegisteredCell()
                delay(3000)
            }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                val last = locationRepo.getLastKnownLocation()
                if (last != null) userLocation.value = last
                locationRepo.locationUpdates(5000).collect { loc ->
                    userLocation.value = loc
                }
            } catch (e: Exception) {
                // الصلاحيات غير ممنوحة بعد
            }
        }
    }

    fun searchTowers(lat: Double, lon: Double, radiusMeters: Double) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val result = repository.findTowersNear(lat, lon, radiusMeters)
                allTowersCache = result.towers
                towers.value = applyFilters(allTowersCache)
                errorMessage.value = result.diagnosticMessage
            } catch (e: Exception) {
                errorMessage.value = "خطأ: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun setFilters(operator: String?, networkType: String?) {
        selectedOperator = operator
        selectedNetworkType = networkType
        towers.value = applyFilters(allTowersCache)
    }

    private fun applyFilters(list: List<TowerEntity>): List<TowerEntity> = list.filter { t ->
        (selectedOperator == null || t.operator == selectedOperator) &&
        (selectedNetworkType == null || t.networkType == selectedNetworkType)
    }
}
