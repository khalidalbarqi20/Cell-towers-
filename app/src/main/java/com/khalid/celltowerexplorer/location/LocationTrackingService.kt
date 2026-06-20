package com.khalid.celltowerexplorer.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.khalid.celltowerexplorer.R
import com.khalid.celltowerexplorer.data.AppDatabase
import com.khalid.celltowerexplorer.data.ObservationEntity
import com.khalid.celltowerexplorer.telephony.CellInfoReader
import com.khalid.celltowerexplorer.ui.MainActivity
import com.khalid.celltowerexplorer.utils.BandCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationRepository: LocationRepository
    private lateinit var cellInfoReader: CellInfoReader
    private var trackingJob: Job? = null

    companion object {
        const val CHANNEL_ID = "cell_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val INTERVAL_MILLIS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository(applicationContext)
        cellInfoReader = CellInfoReader(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        if (trackingJob?.isActive == true) return
        trackingJob = serviceScope.launch {
            locationRepository.locationUpdates(INTERVAL_MILLIS).collect { loc ->
                val db = AppDatabase.getInstance(applicationContext)
                val cell = cellInfoReader.readRegisteredCell()

                // حساب الباند من EARFCN أو NRARFCN
                val bandInfo = BandCalculator.fromEarfcn(cell?.earfcn)
                    ?: BandCalculator.fromNrarfcn(cell?.nrarfcn)

                db.observationDao().insert(
                    ObservationEntity(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        cellId = cell?.cellId?.toString(),
                        pci = cell?.pci,
                        tac = cell?.areaCode,
                        mcc = cell?.mcc,
                        mnc = cell?.mnc,
                        operator = cell?.operatorName,
                        signalStrength = cell?.signalStrengthDbm,
                        rsrp = cell?.rsrp,
                        rsrq = cell?.rsrq,
                        sinr = cell?.sinr,
                        earfcn = cell?.earfcn,
                        nrarfcn = cell?.nrarfcn,
                        band = bandInfo?.label,
                        networkType = cell?.networkType,
                        timestamp = loc.timestamp
                    )
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
