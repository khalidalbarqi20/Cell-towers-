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

    private var _binding: FragmentGaugeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var isTracking = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGaugeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.registeredCell.observe(viewLifecycleOwner) { cell ->
            if (cell == null) {
                binding.tvOperator.text = "لا توجد بيانات شبكة"
                binding.tvNetworkType.text = "--"
                return@observe
            }

            binding.tvOperator.text = cell.operatorName ?: "مشغّل غير معروف"
            binding.tvNetworkType.text = cell.networkType

            // RSRP gauge: -140 (أسوأ) → -44 (أفضل)
            val rsrp = cell.rsrp ?: cell.signalStrengthDbm
            if (rsrp != null) {
                val f = ((rsrp + 140f) / 96f).coerceIn(0f, 1f)
                binding.gaugeRsrp.setGauge("$rsrp", "dBm", f)
            } else {
                binding.gaugeRsrp.setGauge("--", "RSRP", 0f)
            }

            // SINR gauge: -23 → +40
            val sinr = cell.sinr
            if (sinr != null) {
                val f = ((sinr + 23f) / 63f).coerceIn(0f, 1f)
                binding.gaugeSinr.setGauge("$sinr", "dB", f)
            } else {
                binding.gaugeSinr.setGauge("--", "SINR", 0f)
            }

            // RSRQ gauge: -34 → 0
            val rsrq = cell.rsrq
            if (rsrq != null) {
                val f = ((rsrq + 34f) / 34f).coerceIn(0f, 1f)
                binding.gaugeRsrq.setGauge("$rsrq", "dB", f)
            } else {
                binding.gaugeRsrq.setGauge("--", "RSRQ", 0f)
            }

            // بيانات نصية
            binding.tvCellId.text = cell.cellId?.toString() ?: "--"
            binding.tvPci.text = cell.pci?.toString() ?: "--"
            binding.tvTac.text = cell.areaCode?.toString() ?: "--"
            binding.tvMccMnc.text = if (cell.mcc != null && cell.mnc != null) "${cell.mcc}-${cell.mnc}" else "--"
            binding.tvSignal.text = cell.signalStrengthDbm?.let { "$it dBm" } ?: "--"
            binding.tvRssi.text = cell.rssi?.let { "$it dBm" } ?: "--"
            binding.tvEarfcn.text = cell.earfcn?.toString() ?: cell.nrarfcn?.toString() ?: "--"
            binding.tvNrarfcn.text = cell.nrarfcn?.toString() ?: "--"

            val band = BandCalculator.fromEarfcn(cell.earfcn) ?: BandCalculator.fromNrarfcn(cell.nrarfcn)
            binding.tvBand.text = band?.label ?: "--"
        }

        binding.btnTracking.setOnClickListener {
            isTracking = !isTracking
            if (isTracking) {
                LocationTrackingService.start(requireContext())
                binding.btnTracking.text = "إيقاف التتبع"
            } else {
                LocationTrackingService.stop(requireContext())
                binding.btnTracking.text = "ابدأ التتبع"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
