package com.khalid.celltowerexplorer.ui.tabs

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.data.ObservationEntity
import com.khalid.celltowerexplorer.databinding.FragmentDataBinding
import com.khalid.celltowerexplorer.databinding.ItemObservationBinding
import com.khalid.celltowerexplorer.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private val adapter = ObservationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvObservations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvObservations.adapter = adapter
        loadData()

        binding.btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val obs = db.observationDao().getRecent(500)
            binding.tvObsCount.text = "${obs.size} قراءة مسجّلة"
            adapter.submitList(obs)
        }
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val obs = db.observationDao().getRecent(5000)
                if (obs.isEmpty()) {
                    Toast.makeText(requireContext(), "لا توجد بيانات للتصدير", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val sb = StringBuilder()
                sb.appendLine("الوقت,المشغّل,نوع الشبكة,Cell ID,PCI,TAC,MCC,MNC,الباند,RSRP,RSRQ,SINR,الإشارة,خط العرض,خط الطول")
                obs.forEach { o ->
                    sb.appendLine("${sdf.format(Date(o.timestamp))},${o.operator ?: ""},${o.networkType ?: ""},${o.cellId ?: ""},${o.pci ?: ""},${o.tac ?: ""},${o.mcc ?: ""},${o.mnc ?: ""},${o.band ?: ""},${o.rsrp ?: ""},${o.rsrq ?: ""},${o.sinr ?: ""},${o.signalStrength ?: ""},${o.latitude},${o.longitude}")
                }
                val file = File(requireContext().cacheDir, "cell_data_${System.currentTimeMillis()}.csv")
                file.writeText(sb.toString())
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "تصدير CSV"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ===== Adapter =====
    class ObservationAdapter : RecyclerView.Adapter<ObservationAdapter.VH>() {
        private val items = mutableListOf<ObservationEntity>()

        fun submitList(list: List<ObservationEntity>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        inner class VH(val b: ItemObservationBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemObservationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val o = items[position]
            val b = holder.b
            b.tvItemOperator.text = o.operator ?: "مشغّل غير معروف"
            b.tvItemNetworkType.text = o.networkType ?: "--"
            b.tvItemCellId.text = "Cell: ${o.cellId ?: "--"} • PCI: ${o.pci ?: "--"}"
            b.tvItemBand.text = o.band ?: ""
            val sig = o.rsrp ?: o.signalStrength
            b.tvItemSignal.text = if (sig != null) "إشارة: $sig dBm" else "إشارة: --"
            // لون شريط الإشارة
            val color = when {
                sig != null && sig >= -80 -> Color.parseColor("#4CAF50")
                sig != null && sig >= -100 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#F44336")
            }
            b.signalBar.setBackgroundColor(color)
            val sdf = SimpleDateFormat("HH:mm", Locale("ar"))
            b.tvItemTime.text = sdf.format(Date(o.timestamp))
        }
    }
}
