package com.khalid.celltowerexplorer.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            binding.tvTotalTowers.text = db.towerDao().totalCount().toString()
            binding.tvTotalObs.text = db.observationDao().totalCount().toString()
            binding.tvCount4g.text = db.towerDao().countByNetworkType("4G").toString()
            binding.tvCount5g.text = db.towerDao().countByNetworkType("5G").toString()
            val other = db.towerDao().totalCount() -
                db.towerDao().countByNetworkType("4G") -
                db.towerDao().countByNetworkType("5G")
            binding.tvCountOther.text = other.coerceAtLeast(0).toString()

            binding.operatorContainer.removeAllViews()
            db.towerDao().countByOperator().forEach { item ->
                val tv = TextView(requireContext()).apply {
                    text = "• ${item.operator}: ${item.count} برج"
                    setTextColor(0xFFB0BEC5.toInt())
                    textSize = 14f
                    setPadding(0, 6, 0, 6)
                }
                binding.operatorContainer.addView(tv)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
