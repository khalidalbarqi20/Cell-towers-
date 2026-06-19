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

/**
 * يقرأ معلومات الأبراج المرئية للهاتف عبر TelephonyManager (item 7 و 8).
 *
 * ملاحظة مهمة: بعض الحقول (PCI، الترددات، NRARFCN...) غير متاحة على جميع
 * الأجهزة وإصدارات أندرويد. لذلك كل قراءة حقل "غير أساسي" تمر عبر [safeCall]
 * التي تمسك Throwable بالكامل (وليس Exception فقط) لأن استدعاء دالة غير
 * موجودة على إصدار أندرويد قديم يرمي NoSuchMethodError، وهو خطأ (Error) لا
 * استثناء (Exception)، فلا يجوز الاعتماد على catch(Exception) وحدها هنا.
 */
class CellInfoReader(private val context: Context) {

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private fun hasPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val phoneState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation && phoneState
    }

    /** يقرأ كل الخلايا المرئية حالياً للهاتف، بما فيها الخلية المسجل بها. */
    fun readAllCells(): List<CellSnapshot> {
        if (!hasPermission()) return emptyList()
        return safeCall {
            telephonyManager.allCellInfo
                ?.mapNotNull { mapCellInfo(it) }
                .orEmpty()
        }.orEmpty()
    }

    /** الخلية المتصل بها الهاتف حالياً فقط (item 8: "البرج المتصل به حالياً"). */
    fun readRegisteredCell(): CellSnapshot? {
        val registeredCells = readAllCells().filter { it.isRegistered }
        if (registeredCells.isEmpty()) return null
        if (registeredCells.size == 1) return registeredCells.first()

        // قد يُبلغ النظام أحياناً عن أكثر من خلية "مسجّلة" بنفس الوقت (مثلاً خلية
        // 2G/3G قديمة تُستخدم احتياطياً للمكالمات الصوتية، بينما الإنترنت الفعلي
        // يعمل على 4G/5G). نفضّل دائماً أعلى جيل شبكة متاح لأنه يعكس الاتصال
        // الفعلي للبيانات في الغالبية العظمى من الحالات.
        val priority = mapOf("5G" to 4, "4G" to 3, "3G" to 2, "2G" to 1)
        return registeredCells.maxByOrNull { priority[it.networkType] ?: 0 }
    }

    private fun mapCellInfo(info: CellInfo): CellSnapshot? = safeCall {
        when (info) {
            is CellInfoLte -> mapLte(info)
            is CellInfoGsm -> mapGsm(info)
            is CellInfoWcdma -> mapWcdma(info)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                mapNr(info)
            } else {
                null
            }
        }
    }

    private fun mapLte(info: CellInfoLte): CellSnapshot {
        val identity = info.cellIdentity
        val strength = info.cellSignalStrength

        val mcc = safeCall { identity.mccString?.toIntOrNull() }
            ?: safeCall { safeInt(identity.mcc) }
        val mnc = safeCall { identity.mncString?.toIntOrNull() }
            ?: safeCall { safeInt(identity.mnc) }

        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "4G",
            cellId = safeCall { safeInt(identity.ci)?.toLong() },
            pci = safeCall { safeInt(identity.pci) },
            areaCode = safeCall { safeInt(identity.tac) },
            areaCodeLabel = "TAC",
            mcc = mcc,
            mnc = mnc,
            signalStrengthDbm = safeCall { safeInt(strength.dbm) },
            rsrp = safeCall { safeInt(strength.rsrp) },
            rsrq = safeCall { safeInt(strength.rsrq) },
            rssi = safeCall { safeInt(strength.rssi) },
            earfcn = safeCall { safeInt(identity.earfcn) }
        )
    }

    @Suppress("DEPRECATION")
    private fun mapGsm(info: CellInfoGsm): CellSnapshot {
        val identity = info.cellIdentity
        val strength = info.cellSignalStrength

        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "2G",
            cellId = safeCall { safeInt(identity.cid)?.toLong() },
            pci = null, // لا يوجد PCI في 2G
            areaCode = safeCall { safeInt(identity.lac) },
            areaCodeLabel = "LAC",
            mcc = safeCall { safeInt(identity.mcc) },
            mnc = safeCall { safeInt(identity.mnc) },
            signalStrengthDbm = safeCall { safeInt(strength.dbm) },
            rssi = safeCall { safeInt(strength.dbm) }
        )
    }

    @Suppress("DEPRECATION")
    private fun mapWcdma(info: CellInfoWcdma): CellSnapshot {
        val identity = info.cellIdentity
        val strength = info.cellSignalStrength

        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "3G",
            cellId = safeCall { safeInt(identity.cid)?.toLong() },
            pci = safeCall { safeInt(identity.psc) }, // PSC هو مكافئ PCI في 3G
            areaCode = safeCall { safeInt(identity.lac) },
            areaCodeLabel = "LAC",
            mcc = safeCall { safeInt(identity.mcc) },
            mnc = safeCall { safeInt(identity.mnc) },
            signalStrengthDbm = safeCall { safeInt(strength.dbm) }
        )
    }

    private fun mapNr(info: CellInfoNr): CellSnapshot {
        val identity = safeCall { info.cellIdentity as? CellIdentityNr }
        val strength = safeCall { info.cellSignalStrength as? CellSignalStrengthNr }

        return CellSnapshot(
            isRegistered = safeCall { info.isRegistered } ?: false,
            networkType = "5G",
            cellId = safeCall { identity?.nci },
            pci = safeCall { identity?.pci?.let { safeInt(it) } },
            areaCode = safeCall { identity?.tac?.let { safeInt(it) } },
            areaCodeLabel = "TAC",
            mcc = safeCall { identity?.mccString?.toIntOrNull() },
            mnc = safeCall { identity?.mncString?.toIntOrNull() },
            signalStrengthDbm = safeCall { strength?.ssRsrp?.let { safeInt(it) } },
            rsrp = safeCall { strength?.ssRsrp?.let { safeInt(it) } },
            rsrq = safeCall { strength?.ssRsrq?.let { safeInt(it) } },
            sinr = safeCall { strength?.ssSinr?.let { safeInt(it) } },
            nrarfcn = safeCall { identity?.nrarfcn?.let { safeInt(it) } }
        )
    }

    private fun safeInt(value: Int): Int? =
        if (value == Int.MAX_VALUE || value == CellInfo.UNAVAILABLE) null else value

    /** يلتقط أي خطأ (بما فيه Error مثل NoSuchMethodError) ويحوّله إلى null بدل تعطيل التطبيق. */
    private inline fun <T> safeCall(block: () -> T): T? {
        return try {
            block()
        } catch (t: Throwable) {
            null
        }
    }
}
