package com.khalid.celltowerexplorer.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.databinding.ActivityStatsBinding
import kotlinx.coroutines.launch

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val towerDao = db.towerDao()
            val observationDao = db.observationDao()

            binding.totalTowersValue.text = towerDao.totalCount().toString()
            binding.totalObservationsValue.text = observationDao.totalCount().toString()
            binding.count4gValue.text = towerDao.countByNetworkType("4G").toString()
            binding.count5gValue.text = towerDao.countByNetworkType("5G").toString()

            binding.operatorCountsContainer.removeAllViews()
            towerDao.countByOperator().forEach { item ->
                val row = TextView(this@StatsActivity)
                row.text = "${item.operator}: ${item.count}"
                row.textSize = 15f
                row.setPadding(0, 8, 0, 8)
                binding.operatorCountsContainer.addView(row)
            }
        }
    }
}
