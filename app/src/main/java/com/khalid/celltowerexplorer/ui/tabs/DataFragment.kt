package com.khalid.celltowerexplorer.ui.tabs

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.data.ObservationEntity
import com.khalid.celltowerexplorer.databinding.FragmentDataBinding
import com.khalid.celltowerexplorer.databinding.ItemObservationBinding
import com.khalid.celltowerexplorer.ui.ObservationDetailDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataFragment : Fragment() {
    private var _b: FragmentDataBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: ObsAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentDataBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        adapter = ObsAdapter(
            onTap = { obs -> ObservationDetailDialog.from(obs).show(parentFragmentManager, "d") },
            onLong = { obs ->
                val uri = Uri.parse("geo:${obs.latitude},${obs.longitude}?q=${obs.latitude},${obs.longitude}(${obs.operator ?: "برج"})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(requireActivity().packageManager) != null) startActivity(intent)
                else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${obs.latitude},${obs.longitude}")))
            }
        )
        b.rvObservations.layoutManager = LinearLayoutManager(requireContext())
        b.rvObservations.adapter = adapter
        load()
        b.btnExportCsv.setOnClickListener { export() }
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        lifecycleScope.launch {
            val obs = AppDatabase.getInstance(requireContext()).observationDao().getRecent(500)
            b.tvObsCount.text = "${obs.size} قراءة مسجّلة"
            adapter.submit(obs)
        }
    }

    private fun export() {
        lifecycleScope.launch {
            try {
                val obs = AppDatabase.getInstance(requireContext()).observationDao().getRecent(5000)
                if (obs.isEmpty()) { Toast.makeText(requireContext(),"لا توجد بيانات",Toast.LENGTH_SHORT).show(); return@launch }
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val sb = StringBuilder("الوقت,المشغّل,نوع الشبكة,Cell ID,PCI,TAC,MCC,MNC,الباند,RSRP,RSRQ,SINR,الإشارة,خط العرض,خط الطول\n")
                obs.forEach { o -> sb.appendLine("${sdf.format(Date(o.timestamp))},${o.operator?:""},${o.networkType?:""},${o.cellId?:""},${o.pci?:""},${o.tac?:""},${o.mcc?:""},${o.mnc?:""},${o.band?:""},${o.rsrp?:""},${o.rsrq?:""},${o.sinr?:""},${o.signalStrength?:""},${o.latitude},${o.longitude}") }
                val file = File(requireContext().cacheDir,"cell_${System.currentTimeMillis()}.csv")
                file.writeText(sb.toString())
                val uri = FileProvider.getUriForFile(requireContext(),"${requireContext().packageName}.provider",file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/csv"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },"تصدير CSV"))
            } catch (e: Exception) { Toast.makeText(requireContext(),"خطأ: ${e.message}",Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    class ObsAdapter(private val onTap:(ObservationEntity)->Unit, private val onLong:(ObservationEntity)->Unit) : RecyclerView.Adapter<ObsAdapter.VH>() {
        private val items = mutableListOf<ObservationEntity>()
        fun submit(list: List<ObservationEntity>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        inner class VH(val b: ItemObservationBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemObservationBinding.inflate(LayoutInflater.from(p.context),p,false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val o = items[pos]; val b = h.b
            b.tvItemOperator.text = o.operator ?: "غير معروف"
            b.tvItemNetworkType.text = o.networkType ?: "--"
            b.tvItemCellId.text = "Cell: ${o.cellId?:"--"} • PCI: ${o.pci?:"--"}"
            b.tvItemBand.text = o.band ?: ""
            val sig = o.rsrp ?: o.signalStrength
            b.tvItemSignal.text = if (sig!=null) "إشارة: $sig dBm" else "إشارة: --"
            b.signalBar.setBackgroundColor(when { sig!=null&&sig>=-80->Color.parseColor("#4CAF50"); sig!=null&&sig>=-100->Color.parseColor("#FFC107"); else->Color.parseColor("#F44336") })
            b.tvItemTime.text = SimpleDateFormat("HH:mm",Locale("ar")).format(Date(o.timestamp))
            b.root.setOnClickListener { onTap(o) }
            b.root.setOnLongClickListener { onLong(o); true }
        }
    }
}
