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
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.khalid.celltowerexplorer.utils.OperatorLookup

class CellInfoReader(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        return fine && phone
    }

    fun readAllCells(): List<CellSnapshot> {
        if (!hasPermission()) return emptyList()
        return safeCall { telephonyManager.allCellInfo?.mapNotNull { mapCellInfo(it) }.orEmpty() }.orEmpty()
    }

    fun readRegisteredCell(): CellSnapshot? {
        val all = readAllCells().filter { it.isRegistered }
        if (all.isEmpty()) return null
        val priority = mapOf("5G" to 4, "4G" to 3, "3G" to 2, "2G" to 1)
        return all.maxByOrNull { priority[it.networkType] ?: 0 }
    }

    private fun resolveOperator(mcc: Int?, mnc: Int?): String? {
        return OperatorLookup.operatorName(mcc, mnc)
            ?: safeCall { telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() } }
    }

    private fun mapCellInfo(info: CellInfo): CellSnapshot? = safeCall {
        when (info) {
            is CellInfoLte   -> mapLte(info)
            is CellInfoGsm   -> mapGsm(info)
            is CellInfoWcdma -> mapWcdma(info)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) mapNr(info) else null
        }
    }

    private fun mapLte(info: CellInfoLte): CellSnapshot {
        val id = info.cellIdentity
        val sig = info.cellSignalStrength
        val mcc = safeCall { id.mccString?.toIntOrNull() } ?: safeCall { safeInt(id.mcc) }
        val mnc = safeCall { id.mncString?.toIntOrNull() } ?: safeCall { safeInt(id.mnc) }
        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "4G",
            cellId = safeCall { safeInt(id.ci)?.toLong() },
            pci = safeCall { safeInt(id.pci) },
            areaCode = safeCall { safeInt(id.tac) },
            areaCodeLabel = "TAC",
            mcc = mcc, mnc = mnc,
            operatorName = resolveOperator(mcc, mnc),
            signalStrengthDbm = safeCall { safeInt(sig.dbm) },
            rsrp = safeCall { safeInt(sig.rsrp) },
            rsrq = safeCall { safeInt(sig.rsrq) },
            rssi = safeCall { safeInt(sig.rssi) },
            earfcn = safeCall { safeInt(id.earfcn) }
        )
    }

    @Suppress("DEPRECATION")
    private fun mapGsm(info: CellInfoGsm): CellSnapshot {
        val id = info.cellIdentity
        val sig = info.cellSignalStrength
        val mcc = safeCall { safeInt(id.mcc) }
        val mnc = safeCall { safeInt(id.mnc) }
        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "2G",
            cellId = safeCall { safeInt(id.cid)?.toLong() },
            areaCode = safeCall { safeInt(id.lac) },
            areaCodeLabel = "LAC",
            mcc = mcc, mnc = mnc,
            operatorName = resolveOperator(mcc, mnc),
            signalStrengthDbm = safeCall { safeInt(sig.dbm) }
        )
    }

    @Suppress("DEPRECATION")
    private fun mapWcdma(info: CellInfoWcdma): CellSnapshot {
        val id = info.cellIdentity
        val sig = info.cellSignalStrength
        val mcc = safeCall { safeInt(id.mcc) }
        val mnc = safeCall { safeInt(id.mnc) }
        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "3G",
            cellId = safeCall { safeInt(id.cid)?.toLong() },
            pci = safeCall { safeInt(id.psc) },
            areaCode = safeCall { safeInt(id.lac) },
            areaCodeLabel = "LAC",
            mcc = mcc, mnc = mnc,
            operatorName = resolveOperator(mcc, mnc),
            signalStrengthDbm = safeCall { safeInt(sig.dbm) }
        )
    }

    private fun mapNr(info: CellInfoNr): CellSnapshot {
        val id = safeCall { info.cellIdentity as? CellIdentityNr }
        val sig = safeCall { info.cellSignalStrength as? CellSignalStrengthNr }
        val mcc = safeCall { id?.mccString?.toIntOrNull() }
        val mnc = safeCall { id?.mncString?.toIntOrNull() }
        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "5G",
            cellId = safeCall { id?.nci },
            pci = safeCall { id?.pci?.let { safeInt(it) } },
            areaCode = safeCall { id?.tac?.let { safeInt(it) } },
            areaCodeLabel = "TAC",
            mcc = mcc, mnc = mnc,
            operatorName = resolveOperator(mcc, mnc),
            signalStrengthDbm = safeCall { sig?.ssRsrp?.let { safeInt(it) } },
            rsrp = safeCall { sig?.ssRsrp?.let { safeInt(it) } },
            rsrq = safeCall { sig?.ssRsrq?.let { safeInt(it) } },
            sinr = safeCall { sig?.ssSinr?.let { safeInt(it) } },
            nrarfcn = safeCall { id?.nrarfcn?.let { safeInt(it) } }
        )
    }

    private fun safeInt(v: Int): Int? = if (v == Int.MAX_VALUE || v == CellInfo.UNAVAILABLE) null else v
    private inline fun <T> safeCall(block: () -> T): T? = try { block() } catch (t: Throwable) { null }
}
