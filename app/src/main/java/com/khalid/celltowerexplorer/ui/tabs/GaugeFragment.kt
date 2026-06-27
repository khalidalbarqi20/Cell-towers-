package com.khalid.celltowerexplorer.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.khalid.celltowerexplorer.databinding.FragmentGaugeBinding
import com.khalid.celltowerexplorer.location.LocationTrackingService
import com.khalid.celltowerexplorer.ui.MainViewModel
import com.khalid.celltowerexplorer.utils.BandCalculator

class GaugeFragment : Fragment() {
    private var _b: FragmentGaugeBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()
    private var tracking = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentGaugeBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        vm.registeredCell.observe(viewLifecycleOwner) { cell ->
            if (cell == null) {
                b.tvOperator.text = "لا توجد بيانات شبكة"; b.tvNetworkType.text = "--"
                b.gaugeRsrp.setGauge("--","RSRP",0f); b.gaugeSinr.setGauge("--","SINR",0f); b.gaugeRsrq.setGauge("--","RSRQ",0f)
                return@observe
            }
            b.tvOperator.text = cell.operatorName ?: "MNC:${cell.mnc}"
            b.tvNetworkType.text = cell.networkType
            val rsrp = cell.rsrp ?: cell.signalStrengthDbm
            if (rsrp != null) b.gaugeRsrp.setGauge("$rsrp","dBm",((rsrp+140f)/96f).coerceIn(0f,1f))
            else b.gaugeRsrp.setGauge("--","RSRP",0f)
            val sinr = cell.sinr
            if (sinr != null) b.gaugeSinr.setGauge("$sinr","dB",((sinr+23f)/63f).coerceIn(0f,1f))
            else b.gaugeSinr.setGauge("--","SINR",0f)
            val rsrq = cell.rsrq
            if (rsrq != null) b.gaugeRsrq.setGauge("$rsrq","dB",((rsrq+34f)/34f).coerceIn(0f,1f))
            else b.gaugeRsrq.setGauge("--","RSRQ",0f)
            b.tvCellId.text = cell.cellId?.toString() ?: "--"
            b.tvPci.text = cell.pci?.toString() ?: "--"
            b.tvTac.text = cell.areaCode?.toString() ?: "--"
            b.tvMccMnc.text = if (cell.mcc!=null&&cell.mnc!=null) "${cell.mcc}-${cell.mnc}" else "--"
            b.tvSignal.text = cell.signalStrengthDbm?.let{"$it dBm"} ?: "--"
            b.tvRssi.text = cell.rssi?.let{"$it dBm"} ?: "--"
            b.tvEarfcn.text = cell.earfcn?.toString() ?: cell.nrarfcn?.toString() ?: "--"
            b.tvNrarfcn.text = cell.nrarfcn?.toString() ?: "--"
            val band = BandCalculator.fromEarfcn(cell.earfcn) ?: BandCalculator.fromNrarfcn(cell.nrarfcn)
            b.tvBand.text = band?.label ?: "--"
        }
        b.btnTracking.setOnClickListener {
            tracking = !tracking
            if (tracking) { LocationTrackingService.start(requireContext()); b.btnTracking.text = "إيقاف التتبع" }
            else { LocationTrackingService.stop(requireContext()); b.btnTracking.text = "ابدأ التتبع" }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
