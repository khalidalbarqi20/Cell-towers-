package com.khalid.celltowerexplorer.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.khalid.celltowerexplorer.utils.OperatorLookup

class CellInfoReader(private val context: Context) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private fun getDataTm(): TelephonyManager = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                telephonyManager.createForSubscriptionId(subId)
            else telephonyManager
        } else telephonyManager
    } catch (e: Exception) { telephonyManager }

    private fun hasPerm() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    fun readAllCells(): List<CellSnapshot> {
        if (!hasPerm()) return emptyList()
        val tm = getDataTm()
        return safeCall { tm.allCellInfo?.mapNotNull { mapCell(it, tm) }.orEmpty() }.orEmpty()
    }

    fun readRegisteredCell(): CellSnapshot? {
        val all = readAllCells().filter { it.isRegistered }
        val priority = mapOf("5G" to 4, "4G" to 3, "3G" to 2, "2G" to 1)
        return all.maxByOrNull { priority[it.networkType] ?: 0 }
    }

    private fun op(mcc: Int?, mnc: Int?, tm: TelephonyManager) =
        OperatorLookup.operatorName(mcc, mnc) ?: safeCall { tm.networkOperatorName?.takeIf { it.isNotBlank() } }

    private fun mapCell(info: CellInfo, tm: TelephonyManager): CellSnapshot? = safeCall {
        when (info) {
            is CellInfoLte -> {
                val id = info.cellIdentity; val sig = info.cellSignalStrength
                val mcc = safeCall { id.mccString?.toIntOrNull() } ?: safeCall { si(id.mcc) }
                val mnc = safeCall { id.mncString?.toIntOrNull() } ?: safeCall { si(id.mnc) }
                CellSnapshot(safeCall { info.isRegistered } ?: false, "4G",
                    cellId = safeCall { si(id.ci)?.toLong() }, pci = safeCall { si(id.pci) },
                    areaCode = safeCall { si(id.tac) }, areaCodeLabel = "TAC",
                    mcc = mcc, mnc = mnc, operatorName = op(mcc, mnc, tm),
                    signalStrengthDbm = safeCall { si(sig.dbm) }, rsrp = safeCall { si(sig.rsrp) },
                    rsrq = safeCall { si(sig.rsrq) }, rssi = safeCall { si(sig.rssi) },
                    earfcn = safeCall { si(id.earfcn) })
            }
            is CellInfoGsm -> {
                val id = info.cellIdentity; val sig = info.cellSignalStrength
                val mcc = safeCall { @Suppress("DEPRECATION") si(id.mcc) }
                val mnc = safeCall { @Suppress("DEPRECATION") si(id.mnc) }
                CellSnapshot(safeCall { info.isRegistered } ?: false, "2G",
                    cellId = safeCall { @Suppress("DEPRECATION") si(id.cid)?.toLong() },
                    areaCode = safeCall { @Suppress("DEPRECATION") si(id.lac) }, areaCodeLabel = "LAC",
                    mcc = mcc, mnc = mnc, operatorName = op(mcc, mnc, tm),
                    signalStrengthDbm = safeCall { si(sig.dbm) })
            }
            is CellInfoWcdma -> {
                val id = info.cellIdentity; val sig = info.cellSignalStrength
                val mcc = safeCall { @Suppress("DEPRECATION") si(id.mcc) }
                val mnc = safeCall { @Suppress("DEPRECATION") si(id.mnc) }
                CellSnapshot(safeCall { info.isRegistered } ?: false, "3G",
                    cellId = safeCall { @Suppress("DEPRECATION") si(id.cid)?.toLong() },
                    pci = safeCall { @Suppress("DEPRECATION") si(id.psc) },
                    areaCode = safeCall { @Suppress("DEPRECATION") si(id.lac) }, areaCodeLabel = "LAC",
                    mcc = mcc, mnc = mnc, operatorName = op(mcc, mnc, tm),
                    signalStrengthDbm = safeCall { si(sig.dbm) })
            }
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                val id = safeCall { info.cellIdentity as? CellIdentityNr }
                val sig = safeCall { info.cellSignalStrength as? CellSignalStrengthNr }
                val mcc = safeCall { id?.mccString?.toIntOrNull() }
                val mnc = safeCall { id?.mncString?.toIntOrNull() }
                CellSnapshot(safeCall { info.isRegistered } ?: false, "5G",
                    cellId = safeCall { id?.nci }, pci = safeCall { id?.pci?.let { si(it) } },
                    areaCode = safeCall { id?.tac?.let { si(it) } }, areaCodeLabel = "TAC",
                    mcc = mcc, mnc = mnc, operatorName = op(mcc, mnc, tm),
                    signalStrengthDbm = safeCall { sig?.ssRsrp?.let { si(it) } },
                    rsrp = safeCall { sig?.ssRsrp?.let { si(it) } },
                    rsrq = safeCall { sig?.ssRsrq?.let { si(it) } },
                    sinr = safeCall { sig?.ssSinr?.let { si(it) } },
                    nrarfcn = safeCall { id?.nrarfcn?.let { si(it) } })
            } else null
        }
    }

    private fun si(v: Int): Int? = if (v == Int.MAX_VALUE || v == CellInfo.UNAVAILABLE) null else v
    private inline fun <T> safeCall(block: () -> T): T? = try { block() } catch (t: Throwable) { null }
}
