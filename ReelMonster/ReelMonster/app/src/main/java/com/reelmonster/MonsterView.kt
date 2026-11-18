package com.reelmonster

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min

/** The cute monster. Grows and gets angrier as the count rises; the count is written on its belly. */
class MonsterView(c: Context) : View(c) {
    private var count = 0
    private val d = c.resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun setCount(n: Int) { count = n; requestLayout(); invalidate() }

    override fun onMeasure(w: Int, h: Int) {
        val s = ((64 + 0.8f * min(count, 100)) * d).toInt()
        setMeasuredDimension(s, s)
    }

    override fun onDraw(cv: Canvas) {
        val s = width.toFloat()
        val t = min(count, 100) / 100f
        val ev = ArgbEvaluator()
        val body = (if (t < 0.5f) ev.evaluate(t * 2, 0xFF5BD16B.toInt(), 0xFFF5A623.toInt())
                    else ev.evaluate((t - 0.5f) * 2, 0xFFF5A623.toInt(), 0xFFE74C3C.toInt())) as Int
        val dark = ev.evaluate(0.35f, body, Color.BLACK) as Int

        // horns
        p.style = Paint.Style.FILL; p.color = dark
        path.reset(); path.moveTo(.18f * s, .30f * s); path.lineTo(.20f * s, .02f * s); path.lineTo(.42f * s, .20f * s); path.close(); cv.drawPath(path, p)
        path.reset(); path.moveTo(.82f * s, .30f * s); path.lineTo(.80f * s, .02f * s); path.lineTo(.58f * s, .20f * s); path.close(); cv.drawPath(path, p)
        // feet
        cv.drawCircle(.32f * s, .95f * s, .09f * s, p); cv.drawCircle(.68f * s, .95f * s, .09f * s, p)
        // body
        p.color = body
        cv.drawRoundRect(RectF(.06f * s, .16f * s, .94f * s, .97f * s), .42f * s, .42f * s, p)

        // eyes
        p.color = Color.WHITE
        cv.drawCircle(.34f * s, .38f * s, .13f * s, p); cv.drawCircle(.66f * s, .38f * s, .13f * s, p)
        p.color = Color.rgb(30, 30, 30)
        val look = .02f * s + t * .03f * s
        cv.drawCircle(.34f * s, .38f * s + look, .06f * s, p); cv.drawCircle(.66f * s, .38f * s + look, .06f * s, p)
        // angry brows after 40
        if (count >= 40) {
            p.style = Paint.Style.STROKE; p.strokeWidth = .035f * s; p.strokeCap = Paint.Cap.ROUND
            cv.drawLine(.20f * s, .20f * s, .42f * s, .27f * s, p); cv.drawLine(.80f * s, .20f * s, .58f * s, .27f * s, p)
            p.style = Paint.Style.FILL
        }
        // mouth
        if (count < 10) {
            p.style = Paint.Style.STROKE; p.strokeWidth = .035f * s; p.strokeCap = Paint.Cap.ROUND
            cv.drawArc(RectF(.36f * s, .46f * s, .64f * s, .60f * s), 0f, 180f, false, p)
            p.style = Paint.Style.FILL
        } else {
            p.color = Color.rgb(120, 20, 40)
            cv.drawOval(RectF(.33f * s, .50f * s, .67f * s, (.58f + .1f * t) * s), p)
            if (count >= 30) {
                p.color = Color.WHITE
                cv.drawRect(.38f * s, .50f * s, .44f * s, .54f * s, p); cv.drawRect(.56f * s, .50f * s, .62f * s, .54f * s, p)
            }
        }
        // count on belly
        val txt = count.toString()
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = (if (txt.length >= 3) .20f else .25f) * s
        p.style = Paint.Style.STROKE; p.strokeWidth = .05f * s; p.color = Color.WHITE
        cv.drawText(txt, s / 2, .88f * s, p)
        p.style = Paint.Style.FILL; p.color = Color.rgb(40, 30, 60)
        cv.drawText(txt, s / 2, .88f * s, p)
    }
}
