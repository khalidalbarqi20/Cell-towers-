package com.khalid.celltowerexplorer.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * خدمة أمامية (Foreground Service) تسجّل كل 10 ثوانٍ (ضمن نطاق 5-15 ثانية
 * المطلوب في item 9) موقع المستخدم وبيانات الخلية المتصل بها في قاعدة
 * البيانات المحلية. الإشعار الدائم مطلوب من نظام أندرويد لخدمات الموقع
 * الأمامية على أندرويد 8+ (item 17: العمل دون سيرفر، استهلاك بطارية منخفض).
 */
class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationRepository: LocationRepository
    private lateinit var cellInfoReader: CellInfoReader
    private var trackingJob: Job? = null

    companion object {
        const val CHANNEL_ID = "cell_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val INTERVAL_MILLIS = 10_000L

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
                val registeredCell = cellInfoReader.readRegisteredCell()
                db.observationDao().insert(
                    ObservationEntity(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        cellId = registeredCell?.cellId?.toString(),
                        pci = registeredCell?.pci,
                        tac = registeredCell?.areaCode,
                        signalStrength = registeredCell?.signalStrengthDbm,
                        networkType = registeredCell?.networkType,
                        timestamp = loc.timestamp
                    )
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
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
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
