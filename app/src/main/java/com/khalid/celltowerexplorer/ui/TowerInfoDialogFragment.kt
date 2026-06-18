package com.khalid.celltowerexplorer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.databinding.DialogTowerInfoBinding
import com.khalid.celltowerexplorer.utils.DistanceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TowerInfoDialogFragment : DialogFragment() {

    private var _binding: DialogTowerInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTowerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val compositeCellId = arguments?.getString(ARG_CELL_ID) ?: return
        val operator = arguments?.getString(ARG_OPERATOR)
        val lat = arguments?.getDouble(ARG_LAT) ?: 0.0
        val lon = arguments?.getDouble(ARG_LON) ?: 0.0
        val networkType = arguments?.getString(ARG_NETWORK_TYPE)
        val confidence = arguments?.getInt(ARG_CONFIDENCE) ?: 0
        val lastSeen = arguments?.getLong(ARG_LAST_SEEN) ?: 0L
        val userLat = arguments?.getDouble(ARG_USER_LAT) ?: 0.0
        val userLon = arguments?.getDouble(ARG_USER_LON) ?: 0.0

        // مفتاح البرج بصيغة "mcc-mnc-lacOrTac-cellid" — نستخرج منه رقم الخلية الحقيقي ومنطقة LAC/TAC للعرض.
        val parts = compositeCellId.split("-")
        val displayCellId = parts.getOrNull(3) ?: compositeCellId
        val displayAreaCode = parts.getOrNull(2)

        val distance = DistanceUtils.distanceMeters(userLat, userLon, lat, lon)
        val bearing = DistanceUtils.bearingDegrees(userLat, userLon, lat, lon)
        val notAvailable = getString(R.string.not_available)

        binding.infoOperator.text = "${getString(R.string.operator_label)}: ${operator ?: notAvailable}"
        binding.infoCellId.text = "${getString(R.string.cell_id_label)}: $displayCellId"
        binding.infoAreaCode.text = "LAC/TAC: ${displayAreaCode ?: notAvailable}"
        // PCI غير متوفر من OpenCellID نفسه (لا يُعاد ضمن استجابة getInArea)، بل فقط من القراءة
        // المباشرة لشريحة الهاتف للبرج المتصل به حالياً، والمعروضة في البطاقة العلوية بالشاشة الرئيسية.
        binding.infoPci.text = "${getString(R.string.pci_label)}: $notAvailable"
        binding.infoNetworkType.text =
            "${getString(R.string.network_type_label)}: ${networkType ?: notAvailable}"
        binding.infoDistance.text =
            "${getString(R.string.distance_label)}: ${DistanceUtils.formatDistance(distance)}"
        binding.infoDirection.text =
            "${getString(R.string.direction_label)}: ${DistanceUtils.bearingToArabicDirection(bearing)}"
        binding.infoConfidence.text = "${getString(R.string.confidence_label)}: $confidence%"
        binding.infoLastSeen.text = "${getString(R.string.last_seen_label)}: ${formatTimestamp(lastSeen)}"

        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.not_available)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CELL_ID = "cellId"
        private const val ARG_OPERATOR = "operator"
        private const val ARG_LAT = "lat"
        private const val ARG_LON = "lon"
        private const val ARG_NETWORK_TYPE = "networkType"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_LAST_SEEN = "lastSeen"
        private const val ARG_USER_LAT = "userLat"
        private const val ARG_USER_LON = "userLon"

        fun newInstance(tower: TowerEntity, userLat: Double, userLon: Double): TowerInfoDialogFragment {
            val fragment = TowerInfoDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CELL_ID, tower.cellId)
                putString(ARG_OPERATOR, tower.operator)
                putDouble(ARG_LAT, tower.latitude)
                putDouble(ARG_LON, tower.longitude)
                putString(ARG_NETWORK_TYPE, tower.networkType)
                putInt(ARG_CONFIDENCE, tower.confidenceScore)
                putLong(ARG_LAST_SEEN, tower.lastSeen)
                putDouble(ARG_USER_LAT, userLat)
                putDouble(ARG_USER_LON, userLon)
            }
            return fragment
        }
    }
}
