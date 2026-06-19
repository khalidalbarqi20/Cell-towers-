package com.khalid.celltowerexplorer.utils

/**
 * يحسب رقم الباند (Band) والتردد التقريبي من EARFCN (4G/LTE) أو NRARFCN (5G/NR)
 * باستخدام جداول 3GPP — يعمل بدون إنترنت بالكامل.
 *
 * مصدر: 3GPP TS 36.101 (LTE) و 3GPP TS 38.101 (NR)
 */
object BandCalculator {

    data class BandInfo(
        val band: Int,
        val freqMhz: Double,
        val label: String // مثال: "B3 (1800 MHz)" أو "n78 (3500 MHz)"
    )

    // جدول LTE EARFCN → Band (3GPP TS 36.101 Table 5.7.3-1 مختصر للباندات الشائعة)
    private val lteBands = listOf(
        Triple(0, 599, 1 to 2100.0),
        Triple(600, 1199, 2 to 1900.0),
        Triple(1200, 1949, 3 to 1800.0),
        Triple(1950, 2399, 4 to 1700.0),
        Triple(2400, 2649, 5 to 850.0),
        Triple(2750, 3449, 7 to 2600.0),
        Triple(3450, 3799, 8 to 900.0),
        Triple(3800, 4149, 9 to 1700.0),
        Triple(4150, 4749, 10 to 1700.0),
        Triple(4750, 4999, 11 to 1500.0),
        Triple(5000, 5179, 12 to 700.0),
        Triple(5180, 5279, 13 to 700.0),
        Triple(5280, 5379, 14 to 700.0),
        Triple(5730, 5849, 17 to 700.0),
        Triple(5850, 5999, 18 to 850.0),
        Triple(6000, 6149, 19 to 850.0),
        Triple(6150, 6449, 20 to 800.0),
        Triple(6450, 6599, 21 to 1500.0),
        Triple(6600, 7399, 22 to 3500.0),
        Triple(7500, 7699, 26 to 850.0),
        Triple(7700, 8039, 28 to 700.0),
        Triple(8040, 8689, 29 to 700.0),
        Triple(8690, 9039, 30 to 2300.0),
        Triple(9040, 9209, 31 to 450.0),
        Triple(9210, 9659, 32 to 1500.0),
        Triple(36200, 36349, 33 to 1900.0),
        Triple(36350, 36949, 34 to 2000.0),
        Triple(36950, 37549, 35 to 1900.0),
        Triple(37550, 38249, 36 to 1900.0),
        Triple(38250, 38649, 37 to 1900.0),
        Triple(38650, 39649, 38 to 2600.0),
        Triple(39650, 41589, 39 to 1900.0),
        Triple(41590, 43589, 40 to 2300.0),
        Triple(43590, 45589, 41 to 2500.0),
        Triple(45590, 46589, 42 to 3500.0),
        Triple(46590, 46789, 43 to 3700.0),
        Triple(65536, 65935, 65 to 2100.0),
        Triple(65936, 66435, 66 to 1700.0),
        Triple(67336, 67535, 68 to 700.0),
        Triple(67536, 67835, 69 to 2500.0),
        Triple(68336, 68585, 70 to 1700.0),
        Triple(68586, 68935, 71 to 600.0),
        Triple(68936, 68985, 72 to 450.0),
        Triple(68986, 69035, 73 to 450.0),
        Triple(69036, 69465, 74 to 1500.0),
        Triple(70366, 70545, 85 to 700.0)
    )

    // جدول NR NRARFCN → Band (3GPP TS 38.101 مختصر للباندات الشائعة)
    private val nrBands = listOf(
        Triple(422000, 434000, 1 to 2100.0),
        Triple(386000, 398000, 3 to 1800.0),
        Triple(173800, 178800, 5 to 850.0),
        Triple(524000, 538000, 7 to 2600.0),
        Triple(185000, 192000, 8 to 900.0),
        Triple(145800, 149200, 12 to 700.0),
        Triple(151600, 153600, 14 to 700.0),
        Triple(158200, 164200, 20 to 800.0),
        Triple(386000, 399000, 25 to 1900.0),
        Triple(171800, 178800, 26 to 850.0),
        Triple(140600, 149600, 28 to 700.0),
        Triple(295000, 303600, 30 to 2300.0),
        Triple(402000, 405000, 34 to 2000.0),
        Triple(514000, 524000, 38 to 2600.0),
        Triple(376000, 384000, 39 to 1900.0),
        Triple(460000, 480000, 40 to 2300.0),
        Triple(499200, 537999, 41 to 2500.0),
        Triple(743334, 795000, 50 to 1500.0),
        Triple(286400, 303400, 51 to 1500.0),
        Triple(620000, 680000, 66 to 1700.0),
        Triple(123400, 130400, 71 to 600.0),
        Triple(285400, 286400, 74 to 1500.0),
        Triple(620000, 653333, 75 to 1500.0),
        Triple(286400, 303400, 76 to 1500.0),
        Triple(620000, 680000, 77 to 3500.0),
        Triple(620000, 653333, 78 to 3500.0),
        Triple(693334, 733333, 79 to 4700.0),
        Triple(2054166, 2104165, 257 to 28000.0),
        Triple(2016667, 2070832, 258 to 26000.0),
        Triple(2270833, 2337499, 260 to 39000.0),
        Triple(2399166, 2415832, 261 to 28000.0)
    )

    fun fromEarfcn(earfcn: Int?): BandInfo? {
        if (earfcn == null) return null
        val match = lteBands.firstOrNull { earfcn in it.first..it.second }
            ?: return null
        val (band, freq) = match.third
        return BandInfo(band, freq, "B$band (${freq.toInt()} MHz)")
    }

    fun fromNrarfcn(nrarfcn: Int?): BandInfo? {
        if (nrarfcn == null) return null
        val match = nrBands.firstOrNull { nrarfcn in it.first..it.second }
            ?: return null
        val (band, freq) = match.third
        val freqLabel = if (freq >= 6000) "${(freq / 1000).toInt()} GHz" else "${freq.toInt()} MHz"
        return BandInfo(band, freq, "n$band ($freqLabel)")
    }
}
