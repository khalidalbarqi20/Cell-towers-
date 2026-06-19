package com.khalid.celltowerexplorer.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

/** يوفر إحداثيات GPS الحالية وتحديثات الموقع كل بضع ثوانٍ أو عند الحركة (item 1). */
class LocationRepository(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long): Flow<UserLocation> = callbackFlow {
        val request = LocationRequest.Builder(intervalMillis)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        UserLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            timestamp = loc.time
                        )
                    )
                }
            }
        }

        // Looper.getMainLooper() مطلوب عند الاستدعاء من داخل Foreground Service
        // لأن الخيط الخلفي (IO dispatcher) لا يملك Looper خاصاً به.
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): UserLocation? {
        return try {
            val location = fusedClient.lastLocation.await()
            location?.let { UserLocation(it.latitude, it.longitude, it.accuracy, it.time) }
        } catch (e: Exception) {
            null
        }
    }
}
