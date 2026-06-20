package com.khalid.celltowerexplorer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.databinding.ActivityMainBinding
import com.khalid.celltowerexplorer.ui.tabs.DataFragment
import com.khalid.celltowerexplorer.ui.tabs.GaugeFragment
import com.khalid.celltowerexplorer.ui.tabs.MapFragment
import com.khalid.celltowerexplorer.ui.tabs.StatsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                      results[Manifest.permission.READ_PHONE_STATE] == true
        if (!granted) {
            Toast.makeText(this, "يحتاج التطبيق صلاحيات الموقع والهاتف للعمل", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
        setupNavigation()

        // افتح تبويب المقاييس افتراضياً
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GaugeFragment())
                .commit()
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_gauge -> GaugeFragment()
                R.id.nav_map   -> MapFragment()
                R.id.nav_data  -> DataFragment()
                R.id.nav_stats -> StatsFragment()
                else -> GaugeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }
    }

    private fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }
}
