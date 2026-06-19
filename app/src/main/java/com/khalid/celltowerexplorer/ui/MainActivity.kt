package com.khalid.celltowerexplorer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.databinding.ActivityMainBinding
import com.khalid.celltowerexplorer.location.LocationRepository
import com.khalid.celltowerexplorer.location.LocationTrackingService
import com.khalid.celltowerexplorer.utils.DistanceUtils
import com.khalid.celltowerexplorer.utils.OperatorLookup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MapViewModel by viewModels()
    private lateinit var locationRepository: LocationRepository

    private var currentRadiusMeters = 1000.0
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isTrackingActive = false
    private var registeredCellPollingJob: Job? = null

    private val requiredPermissions: Array<String> by lazy {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val coreGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
            results[Manifest.permission.READ_PHONE_STATE] == true
        if (coreGranted) {
            onPermissionsGranted()
        } else {
            Toast.makeText(this, getString(R.string.permission_required_message), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        Configuration.getInstance().load(
            applicationContext,
            android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationRepository = LocationRepository(applicationContext)

        setupMap()
        setupButtons()
        observeViewModel()

        if (hasCorePermissions()) {
            onPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasCorePermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val phone = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return fine && phone
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(GeoPoint(24.7136, 46.6753)) // الرياض كمركز افتراضي مبدئي
    }

    private fun setupButtons() {
        binding.radius1kmButton.setOnClickListener { onRadiusSelected(1000.0) }
        binding.radius5kmButton.setOnClickListener { onRadiusSelected(5000.0) }
        binding.radius10kmButton.setOnClickListener { onRadiusSelected(10000.0) }

        binding.trackingToggleButton.setOnClickListener { toggleTracking() }

        binding.filterButton.setOnClickListener {
            FilterBottomSheet { operator, networkType ->
                viewModel.setFilters(operator, networkType)
            }.show(supportFragmentManager, "filter")
        }

        binding.statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }

    private fun onRadiusSelected(radiusMeters: Double) {
        currentRadiusMeters = radiusMeters
        val lat = currentLat
        val lon = currentLon
        if (lat != null && lon != null) {
            drawRadiusCircle(lat, lon, radiusMeters)
            viewModel.searchTowers(lat, lon, radiusMeters)
        }
    }

    private fun onPermissionsGranted() {
        lifecycleScope.launch {
            val location = locationRepository.getLastKnownLocation()
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                binding.mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                drawRadiusCircle(location.latitude, location.longitude, currentRadiusMeters)
                viewModel.searchTowers(location.latitude, location.longitude, currentRadiusMeters)
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.search_error_generic), Toast.LENGTH_SHORT).show()
            }
        }
        startRegisteredCellPolling()
    }

    /** يحدّث بطاقة "البرج المتصل به حالياً" كل 5 ثوانٍ طالما الشاشة مفتوحة (item 8). */
    private fun startRegisteredCellPolling() {
        registeredCellPollingJob?.cancel()
        registeredCellPollingJob = lifecycleScope.launch {
            while (true) {
                viewModel.refreshRegisteredCell()
                delay(5000)
            }
        }
    }

    private fun drawRadiusCircle(lat: Double, lon: Double, radiusMeters: Double) {
        binding.mapView.overlays.removeAll { it is Polygon }
        val center = GeoPoint(lat, lon)
        val polygon = Polygon().apply {
            points = Polygon.pointsAsCircle(center, radiusMeters)
            fillColor = 0x220000FF
            strokeColor = 0xFF0000FF.toInt()
            strokeWidth = 2f
        }
        binding.mapView.overlays.add(polygon)
        binding.mapView.invalidate()
    }

    /** يبني نفس صيغة مفتاح البرج "mcc-mnc-lacOrTac-cellid" من قراءة الشريحة الحالية لمطابقتها مع قاعدة البيانات. */
    private fun registeredCellKey(): String? {
        val cell = viewModel.registeredCell.value ?: return null
        val mcc = cell.mcc ?: return null
        val mnc = cell.mnc ?: return null
        val cellId = cell.cellId ?: return null
        val area = cell.areaCode ?: 0
        return "$mcc-$mnc-$area-$cellId"
    }

    private fun renderTowerMarkers(towers: List<TowerEntity>) {
        binding.mapView.overlays.removeAll { it is Marker }
        val lat = currentLat
        val lon = currentLon
        if (lat == null || lon == null) return

        val connectedKey = registeredCellKey()

        towers.forEach { tower ->
            val isConnected = connectedKey != null && tower.cellId == connectedKey
            val marker = Marker(binding.mapView)
            marker.position = GeoPoint(tower.latitude, tower.longitude)
            marker.icon = ContextCompat.getDrawable(
                this,
                if (isConnected) R.drawable.ic_tower_marker_connected else R.drawable.ic_tower_marker
            )
            val distance = DistanceUtils.distanceMeters(lat, lon, tower.latitude, tower.longitude)
            marker.title = tower.operator ?: tower.networkType ?: tower.cellId
            marker.snippet = DistanceUtils.formatDistance(distance)
            marker.setOnMarkerClickListener { _, _ ->
                showTowerInfoDialog(tower, lat, lon)
                true
            }
            binding.mapView.overlays.add(marker)
        }
        binding.mapView.invalidate()
    }

    private fun showTowerInfoDialog(tower: TowerEntity, userLat: Double, userLon: Double) {
        TowerInfoDialogFragment.newInstance(tower, userLat, userLon)
            .show(supportFragmentManager, "tower_info")
    }

    private fun showRegisteredCellDialog() {
        val cell = viewModel.registeredCell.value ?: return
        val lat = currentLat ?: return
        val lon = currentLon ?: return
        TowerInfoDialogFragment.newInstanceFromSnapshot(cell, lat, lon)
            .show(supportFragmentManager, "tower_info_registered")
    }

    /** بدء/إيقاف خدمة التسجيل في الخلفية كل 5-15 ثانية أثناء التحرك (item 9). */
    private fun toggleTracking() {
        isTrackingActive = !isTrackingActive
        if (isTrackingActive) {
            LocationTrackingService.start(this)
            binding.trackingToggleButton.text = getString(R.string.stop_tracking)
        } else {
            LocationTrackingService.stop(this)
            binding.trackingToggleButton.text = getString(R.string.start_tracking)
        }
    }

    private fun observeViewModel() {
        viewModel.towers.observe(this) { towers -> renderTowerMarkers(towers) }

        viewModel.registeredCell.observe(this) { cell ->
            binding.registeredCellDetails.text = buildRegisteredCellText(cell)
            renderTowerMarkers(viewModel.towers.value.orEmpty())
        }

        // الضغط على بطاقة البرج المتصل به يفتح نافذة التفاصيل البصرية الكاملة
        binding.registeredCellCard.setOnClickListener { showRegisteredCellDialog() }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun buildRegisteredCellText(cell: com.khalid.celltowerexplorer.telephony.CellSnapshot?): String {
        if (cell == null) return getString(R.string.registered_cell_none)
        val operatorName = cell.operatorName ?: OperatorLookup.operatorName(cell.mcc, cell.mnc)
        return buildString {
            append(cell.networkType)
            append(" • ")
            append(operatorName ?: getString(R.string.not_available))
            append("\n")
            append(getString(R.string.cell_id_label))
            append(": ")
            append(cell.cellId?.toString() ?: getString(R.string.not_available))
            cell.pci?.let { append(" • PCI: $it") }
            cell.signalStrengthDbm?.let { append("\n${it} dBm") }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        registeredCellPollingJob?.cancel()
    }
}
