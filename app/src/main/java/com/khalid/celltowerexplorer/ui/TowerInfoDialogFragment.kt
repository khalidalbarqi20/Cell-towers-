package com.khalid.celltowerexplorer.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.data.TowerEntity
import com.khalid.celltowerexplorer.databinding.DialogTowerInfoBinding
import com.khalid.celltowerexplorer.telephony.CellSnapshot
import com.khalid.celltowerexplorer.utils.BandCalculator
import com.khalid.celltowerexplorer.utils.DistanceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TowerInfoDialogFragment : DialogFragment() {

    private var _binding: DialogTowerInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogTowerInfoBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        // --- بيانات من TowerEntity (OpenCellID) ---
        val compositeCellId = args.getString(ARG_CELL_ID) ?: ""
        val operator       = args.getString(ARG_OPERATOR)
        val lat            = args.getDouble(ARG_LAT)
        val lon            = args.getDouble(ARG_LON)
        val networkType    = args.getString(ARG_NETWORK_TYPE)
        val confidence     = args.getInt(ARG_CONFIDENCE)
        val lastSeen       = args.getLong(ARG_LAST_SEEN)
        val userLat        = args.getDouble(ARG_USER_LAT)
        val userLon        = args.getDouble(ARG_USER_LON)

        // --- بيانات إضافية من CellSnapshot (شريحة الجهاز) —
        //     تُعبأ فقط إذا كان هذا هو البرج المتصل به حالياً ---
        val rsrp   = args.getInt(ARG_RSRP, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val rsrq   = args.getInt(ARG_RSRQ, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val sinr   = args.getInt(ARG_SINR, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val earfcn = args.getInt(ARG_EARFCN, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val nrarfcn= args.getInt(ARG_NRARFCN, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val pci    = args.getInt(ARG_PCI, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

        val na = getString(R.string.not_available)
        val parts = compositeCellId.split("-")
        val displayCellId  = parts.getOrNull(3) ?: compositeCellId
        val displayAreaCode = parts.getOrNull(2)

        // --- الرأس ---
        binding.infoOperator.text    = operator ?: na
        binding.infoNetworkType.text = networkType ?: na

        val distance = DistanceUtils.distanceMeters(userLat, userLon, lat, lon)
        val bearing  = DistanceUtils.bearingDegrees(userLat, userLon, lat, lon)
        binding.infoDistance.text  = DistanceUtils.formatDistance(distance)
        binding.infoDirection.text = DistanceUtils.bearingToArabicDirection(bearing)

        // --- دائرة قوة الإشارة (RSRP أو Signal) ---
        val signalDbm = rsrp ?: args.getInt(ARG_SIGNAL, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        if (signalDbm != null) {
            // RSRP يتراوح من -44 (ممتاز) إلى -140 (منعدم)
            val fraction = ((signalDbm + 140f) / 96f).coerceIn(0f, 1f)
            val label = if (rsrp != null) "dBm\nRSRP" else "dBm"
            binding.gaugeSignal.setGauge("$signalDbm", label, fraction, goodWhenHigh = true)
        } else {
            binding.gaugeSignal.setGauge(na, "إشارة", 0f)
        }

        // --- دائرة جودة الإشارة (RSRQ أو SINR) ---
        val qualityVal = sinr ?: rsrq
        if (qualityVal != null) {
            val isSinr = sinr != null
            // SINR: -23 إلى +40 dB | RSRQ: -34 إلى 0 dB
            val fraction = if (isSinr) {
                ((qualityVal + 23f) / 63f).coerceIn(0f, 1f)
            } else {
                ((qualityVal + 34f) / 34f).coerceIn(0f, 1f)
            }
            val lbl = if (isSinr) "dB\nSINR" else "dB\nRSRQ"
            binding.gaugeQuality.setGauge("$qualityVal", lbl, fraction, goodWhenHigh = true)
        } else {
            binding.gaugeQuality.setGauge(na, "جودة", 0f)
        }

        // --- دائرة الثقة ---
        binding.gaugeConfidence.setGauge("$confidence%", "ثقة", confidence / 100f, goodWhenHigh = true)

        // --- البيانات التفصيلية ---
        binding.infoCellId.text   = displayCellId.ifEmpty { na }
        binding.infoAreaCode.text = displayAreaCode ?: na
        binding.labelAreaCode.text = if (networkType in listOf("4G","5G")) "TAC" else "LAC"
        binding.infoPci.text      = pci?.toString() ?: na

        // الباند من EARFCN/NRARFCN
        val lteBand = BandCalculator.fromEarfcn(earfcn)
        val nrBand  = BandCalculator.fromNrarfcn(nrarfcn)
        binding.infoEarfcn.text  = if (lteBand != null) "${earfcn} → ${lteBand.label}" else na
        binding.infoNrarfcn.text = if (nrBand  != null) "${nrarfcn} → ${nrBand.label}"  else na

        binding.infoRsrp.text  = rsrp?.let  { "$it dBm" } ?: na
        binding.infoRsrq.text  = rsrq?.let  { "$it dB"  } ?: na
        binding.infoSinr.text  = sinr?.let  { "$it dB"  } ?: na
        binding.infoLastSeen.text = "${getString(R.string.last_seen_label)}: ${formatTs(lastSeen)}"

        binding.closeButton.setOnClickListener { dismiss() }
    }

    private fun formatTs(ts: Long): String {
        if (ts <= 0L) return getString(R.string.not_available)
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(ts))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CELL_ID     = "cellId"
        private const val ARG_OPERATOR    = "operator"
        private const val ARG_LAT         = "lat"
        private const val ARG_LON         = "lon"
        private const val ARG_NETWORK_TYPE= "networkType"
        private const val ARG_CONFIDENCE  = "confidence"
        private const val ARG_LAST_SEEN   = "lastSeen"
        private const val ARG_USER_LAT    = "userLat"
        private const val ARG_USER_LON    = "userLon"
        // من شريحة الجهاز (للبرج المتصل به)
        private const val ARG_RSRP   = "rsrp"
        private const val ARG_RSRQ   = "rsrq"
        private const val ARG_SINR   = "sinr"
        private const val ARG_EARFCN = "earfcn"
        private const val ARG_NRARFCN= "nrarfcn"
        private const val ARG_PCI    = "pci"
        private const val ARG_SIGNAL = "signal"

        /** للأبراج من الخريطة (بيانات OpenCellID فقط) */
        fun newInstance(tower: TowerEntity, userLat: Double, userLon: Double) =
            TowerInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CELL_ID,      tower.cellId)
                    putString(ARG_OPERATOR,     tower.operator)
                    putDouble(ARG_LAT,          tower.latitude)
                    putDouble(ARG_LON,          tower.longitude)
                    putString(ARG_NETWORK_TYPE, tower.networkType)
                    putInt(ARG_CONFIDENCE,      tower.confidenceScore)
                    putLong(ARG_LAST_SEEN,      tower.lastSeen)
                    putDouble(ARG_USER_LAT,     userLat)
                    putDouble(ARG_USER_LON,     userLon)
                }
            }

        /** للبرج المتصل به حالياً (بيانات شريحة الجهاز الكاملة) */
        fun newInstanceFromSnapshot(
            cell: CellSnapshot, userLat: Double, userLon: Double
        ) = TowerInfoDialogFragment().apply {
            val mcc = cell.mcc ?: 0
            val mnc = cell.mnc ?: 0
            val lac = cell.areaCode ?: 0
            val cid = cell.cellId ?: 0L
            arguments = Bundle().apply {
                putString(ARG_CELL_ID,      "$mcc-$mnc-$lac-$cid")
                putString(ARG_OPERATOR,     cell.operatorName)
                putDouble(ARG_LAT,          userLat) // موقع تقريبي بانتظار beaconDB
                putDouble(ARG_LON,          userLon)
                putString(ARG_NETWORK_TYPE, cell.networkType)
                putInt(ARG_CONFIDENCE,      50)
                putLong(ARG_LAST_SEEN,      cell.timestamp)
                putDouble(ARG_USER_LAT,     userLat)
                putDouble(ARG_USER_LON,     userLon)
                cell.rsrp?.let           { putInt(ARG_RSRP,    it) }
                cell.rsrq?.let           { putInt(ARG_RSRQ,    it) }
                cell.sinr?.let           { putInt(ARG_SINR,    it) }
                cell.signalStrengthDbm?.let { putInt(ARG_SIGNAL, it) }
                cell.earfcn?.let         { putInt(ARG_EARFCN,  it) }
                cell.nrarfcn?.let        { putInt(ARG_NRARFCN, it) }
                cell.pci?.let            { putInt(ARG_PCI,     it) }
            }
        }
    }
}
