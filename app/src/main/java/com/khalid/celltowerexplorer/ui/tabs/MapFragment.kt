package com.khalid.celltowerexplorer.ui.tabs

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.databinding.FragmentMapBinding
import com.khalid.celltowerexplorer.ui.FilterBottomSheet
import com.khalid.celltowerexplorer.ui.MainViewModel
import com.khalid.celltowerexplorer.ui.TowerInfoDialogFragment
import com.khalid.celltowerexplorer.utils.DistanceUtils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var currentRadiusMeters = 5000.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext()))
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(13.0)
        binding.mapView.controller.setCenter(GeoPoint(24.7136, 46.6753))

        binding.btn1km.setOnClickListener { setRadius(1000.0) }
        binding.btn5km.setOnClickListener { setRadius(5000.0) }
        binding.btn10km.setOnClickListener { setRadius(10000.0) }
        binding.btnFilter.setOnClickListener {
            FilterBottomSheet { op, nt -> viewModel.setFilters(op, nt) }
                .show(parentFragmentManager, "filter")
        }

        viewModel.userLocation.observe(viewLifecycleOwner) { loc ->
            loc ?: return@observe
            val gp = GeoPoint(loc.latitude, loc.longitude)
            binding.mapView.controller.animateTo(gp)
            drawCircle(loc.latitude, loc.longitude, currentRadiusMeters)
            viewModel.searchTowers(loc.latitude, loc.longitude, currentRadiusMeters)
        }

        viewModel.towers.observe(viewLifecycleOwner) { towers ->
            renderMarkers(towers)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.mapProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setRadius(r: Double) {
        currentRadiusMeters = r
        val loc = viewModel.userLocation.value ?: return
        drawCircle(loc.latitude, loc.longitude, r)
        viewModel.searchTowers(loc.latitude, loc.longitude, r)
    }

    private fun drawCircle(lat: Double, lon: Double, r: Double) {
        binding.mapView.overlays.removeAll { it is Polygon }
        val polygon = Polygon().apply {
            points = Polygon.pointsAsCircle(GeoPoint(lat, lon), r)
            fillColor = 0x220000FF
            strokeColor = 0xFF1565C0.toInt()
            strokeWidth = 2f
        }
        binding.mapView.overlays.add(polygon)
        binding.mapView.invalidate()
    }

    private fun renderMarkers(towers: List<TowerEntity>) {
        binding.mapView.overlays.removeAll { it is Marker }
        val loc = viewModel.userLocation.value ?: return
        val cell = viewModel.registeredCell.value
        val connectedKey = cell?.let {
            "${it.mcc ?: 0}-${it.mnc ?: 0}-${it.areaCode ?: 0}-${it.cellId ?: 0}"
        }

        towers.forEach { tower ->
            val isConnected = connectedKey != null && tower.cellId == connectedKey
            val marker = Marker(binding.mapView)
            marker.position = GeoPoint(tower.latitude, tower.longitude)
            marker.icon = ContextCompat.getDrawable(requireContext(),
                if (isConnected) R.drawable.ic_tower_marker_connected else R.drawable.ic_tower_marker)
            val dist = DistanceUtils.distanceMeters(loc.latitude, loc.longitude, tower.latitude, tower.longitude)
            marker.title = "${tower.operator ?: tower.networkType ?: "برج"} • ${DistanceUtils.formatDistance(dist)}"
            marker.setOnMarkerClickListener { _, _ ->
                TowerInfoDialogFragment.newInstance(tower, loc.latitude, loc.longitude)
                    .show(parentFragmentManager, "tower_info")
                true
            }
            binding.mapView.overlays.add(marker)
        }
        binding.mapView.invalidate()
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
