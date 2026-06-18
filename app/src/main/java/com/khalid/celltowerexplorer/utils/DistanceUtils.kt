package com.khalid.celltowerexplorer.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * دوال حساب المسافة (معادلة Haversine) والاتجاه (Bearing) بين نقطتين جغرافيتين.
 */
object DistanceUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /** المسافة بالمتر بين نقطتين باستخدام معادلة Haversine. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /** يعرض المسافة كنص: "742 متر" إذا أقل من 1000م، أو "3.2 كم" إذا أكثر. */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()} متر"
        } else {
            val km = meters / 1000.0
            "%.1f كم".format(km)
        }
    }

    /** زاوية الاتجاه (Bearing) بالدرجات من 0 إلى 360، حيث 0 = شمال. */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360) % 360
    }

    /** يحوّل زاوية الاتجاه إلى واحدة من ثماني جهات البوصلة بالعربية. */
    fun bearingToArabicDirection(bearing: Double): String {
        val directions = listOf(
            "شمال", "شمال شرقي", "شرقي", "جنوب شرقي",
            "جنوب", "جنوب غربي", "غربي", "شمال غربي"
        )
        val normalized = ((bearing % 360) + 360) % 360
        val index = (((normalized + 22.5) / 45.0).toInt()) % 8
        return directions[index]
    }
}
