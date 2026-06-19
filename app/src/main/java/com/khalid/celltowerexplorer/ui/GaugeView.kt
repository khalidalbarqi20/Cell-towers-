package com.khalid.celltowerexplorer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * دائرة بصرية (Gauge) تعرض قيمة رقمية ونصّها وعنوانها الفرعي.
 * تُرسم بالكامل عبر Canvas — لا تحتاج أي مكتبة خارجية.
 * الألوان تتغير تلقائياً حسب النسبة: أخضر (جيد) / أصفر (متوسط) / أحمر (ضعيف).
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#2A2A4A")
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 36f
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        textAlign = Paint.Align.CENTER
        textSize = 22f
    }

    private val rect = RectF()

    var valueText: String = "--"
    var subLabel: String = ""
    var progress: Float = 0f  // 0.0 إلى 1.0
    var isGoodWhenHigh: Boolean = true // أخضر عند القيم العالية (الثقة/السرعة) أو العكس (للإشارة السالبة مثلاً)

    fun setGauge(value: String, sub: String, fraction: Float, goodWhenHigh: Boolean = true) {
        valueText = value
        subLabel = sub
        progress = fraction.coerceIn(0f, 1f)
        isGoodWhenHigh = goodWhenHigh
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 14f
        rect.set(padding, padding, w - padding, h - padding)

        // خلفية الدائرة الرمادية
        canvas.drawArc(rect, 135f, 270f, false, bgPaint)

        // لون الدائرة حسب التقدم
        arcPaint.color = getColor(progress, isGoodWhenHigh)
        canvas.drawArc(rect, 135f, 270f * progress, false, arcPaint)

        // القيمة في المنتصف
        val cx = w / 2f
        val cy = h / 2f - 6f
        valuePaint.textSize = if (valueText.length > 4) 26f else 32f
        canvas.drawText(valueText, cx, cy + valuePaint.textSize / 3, valuePaint)

        // الوحدة تحت القيمة
        if (subLabel.isNotEmpty()) {
            canvas.drawText(subLabel, cx, cy + valuePaint.textSize / 3 + labelPaint.textSize + 2f, labelPaint)
        }
    }

    private fun getColor(fraction: Float, goodWhenHigh: Boolean): Int {
        val effective = if (goodWhenHigh) fraction else 1f - fraction
        return when {
            effective > 0.6f -> Color.parseColor("#4CAF50") // أخضر
            effective > 0.3f -> Color.parseColor("#FFC107") // أصفر
            else -> Color.parseColor("#F44336")             // أحمر
        }
    }
}
