package com.khalid.celltowerexplorer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.khalid.celltowerexplorer.BuildConfig
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.data.TowerRepository
import com.khalid.celltowerexplorer.network.RetrofitClient
import com.khalid.celltowerexplorer.telephony.CellInfoReader
import com.khalid.celltowerexplorer.telephony.CellSnapshot
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val cellInfoReader = CellInfoReader(application)
    private val repository = TowerRepository(
        towerDao = db.towerDao(),
        observationDao = db.observationDao(),
        openCellIdApi = RetrofitClient.openCellIdApi,
        openCellIdApiKey = BuildConfig.OPENCELLID_API_KEY
    )

    // النتائج الخام غير المصفّاة، تُستخدم كقاعدة عند تغيير الفلاتر دون إعادة استعلام الشبكة.
    private var allTowersCache: List<TowerEntity> = emptyList()

    val towers = MutableLiveData<List<TowerEntity>>(emptyList())
    val registeredCell = MutableLiveData<CellSnapshot?>(null)
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>(null)

    var selectedOperatorFilter: String? = null
        private set
    var selectedNetworkTypeFilter: String? = null
        private set

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
                errorMessage.value = "تعذّر تحديث الأبراج: ${e.message ?: "خطأ غير معروف"}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refreshRegisteredCell() {
        registeredCell.value = cellInfoReader.readRegisteredCell()
    }

    /** operator/networkType بقيمة null تعني "بدون تصفية" لذلك المعيار (item 16). */
    fun setFilters(operator: String?, networkType: String?) {
        selectedOperatorFilter = operator
        selectedNetworkTypeFilter = networkType
        towers.value = applyFilters(allTowersCache)
    }

    private fun applyFilters(list: List<TowerEntity>): List<TowerEntity> {
        return list.filter { tower ->
            (selectedOperatorFilter == null || tower.operator == selectedOperatorFilter) &&
                (selectedNetworkTypeFilter == null || tower.networkType == selectedNetworkTypeFilter)
        }
    }
}
