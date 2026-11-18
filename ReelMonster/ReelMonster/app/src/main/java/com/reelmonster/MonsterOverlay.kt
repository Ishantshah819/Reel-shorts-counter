package com.reelmonster

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot

/** Draggable floating monster + speech bubble, drawn as an accessibility overlay (no "draw over apps" permission needed). */
class MonsterOverlay(private val ctx: Context, private val wm: WindowManager) {
    var onTap: () -> Unit = {}
    var onUndo: () -> Unit = {}

    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * dm.density).toInt()
    private val h = Handler(Looper.getMainLooper())
    private var attached = false

    private val monster = MonsterView(ctx)
    private val bubble = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(Color.rgb(40, 30, 60))
        setPadding(dp(10), dp(6), dp(10), dp(6))
        maxWidth = dp(200)
        gravity = Gravity.CENTER
        visibility = View.GONE
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.LTGRAY) }
    }
    private val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(monster)
        addView(bubble)
    }
    private val lp = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = if (Store.posX >= 0) Store.posX else dm.widthPixels - dp(130)
        y = if (Store.posY >= 0) Store.posY else dp(260)
    }
    private val hideBubble = Runnable { bubble.visibility = View.GONE }

    private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0
    private var dragging = false
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop

    private val gestures = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { if (!dragging) onTap(); return true }
        override fun onLongPress(e: MotionEvent) { if (!dragging) onUndo() }
    })

    init {
        root.setOnTouchListener { _, ev ->
            gestures.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; startX = lp.x; startY = lp.y; dragging = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX; val dy = ev.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) dragging = true
                    if (dragging && attached) {
                        lp.x = (startX + dx).toInt().coerceIn(0, (dm.widthPixels - root.width).coerceAtLeast(0))
                        lp.y = (startY + dy).toInt().coerceIn(0, (dm.heightPixels - root.height).coerceAtLeast(0))
                        try { wm.updateViewLayout(root, lp) } catch (_: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) { Store.posX = lp.x; Store.posY = lp.y }
            }
            true
        }
    }

    fun show() {
        monster.setCount(Store.today())
        if (attached) return
        try { wm.addView(root, lp); attached = true } catch (_: Exception) {}
    }

    fun hide() {
        if (!attached) return
        try { wm.removeView(root) } catch (_: Exception) {}
        attached = false
    }

    fun say(text: String) {
        bubble.text = text
        bubble.visibility = View.VISIBLE
        h.removeCallbacks(hideBubble)
        h.postDelayed(hideBubble, 3500)
    }

    /** Update the number; optionally speak and bounce. */
    fun update(count: Int, line: String?) {
        monster.setCount(count)
        if (line != null) {
            say(line)
            monster.animate().scaleX(1.25f).scaleY(1.25f).setDuration(90).withEndAction {
                monster.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
        }
    }
}
