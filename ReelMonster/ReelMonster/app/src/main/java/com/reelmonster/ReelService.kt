package com.reelmonster

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Hosts the floating monster (only while Instagram / YouTube is on screen) and,
 * in Level 2, detects when a new reel/short appears.
 *
 * Detection idea: while a reel screen is open, read the visible author/caption text of the
 * current reel. When that "signature" changes and stays stable across two checks, a new reel started.
 * View IDs differ between app versions - see README ("Tuning Level 2") and watch `adb logcat -s ReelMonster`.
 */
class ReelService : AccessibilityService() {

    companion object {
        const val IG = "com.instagram.android"
        const val YT = "com.google.android.youtube"
        private const val TAG = "ReelMonster"
        private val NUMERIC = Regex("^[\\d.,\\s]+[KkMmBb]?$") // like counts etc. must not affect the signature

        // Nodes whose view-id contains one of these => "a reel screen is open"
        private val IG_MARKERS = listOf("clips_viewer", "clips_author", "clips_media_component")
        // Text of these nodes (and their children) identifies the current reel
        private val IG_KEYS = listOf("clips_author_username", "clips_caption")
        private val YT_MARKERS = listOf("reel_recycler", "reel_player_page_container", "reel_channel_bar")
        private val YT_KEYS = listOf("reel_channel_bar", "reel_dyn_title")
    }

    private lateinit var overlay: MonsterOverlay
    private val h = Handler(Looper.getMainLooper())

    private var pending = false
    private var lastSig: String? = null      // reel currently counted
    private var candidate: String? = null    // new signature waiting for confirmation
    private var lastCountAt = 0L
    private var notInReelsSince = 0L
    private var lastShortsAt = 0L            // last time a YouTube Short was seen on screen

    override fun onServiceConnected() {
        super.onServiceConnected()
        Store.init(this)
        overlay = MonsterOverlay(this, getSystemService(WINDOW_SERVICE) as WindowManager)
        overlay.onTap = {
            if (Store.mode == 1) bump(1)
            else overlay.say("Level 2: I count by myself. Long-press me to undo one.")
        }
        overlay.onUndo = { overlay.update(Store.add(-1), "Oops, took one back.") }
        val p = rootInActiveWindow?.packageName?.toString()
        if (p == IG) overlay.show()
        else if (p == YT) schedule(100)      // YouTube: show only once we see a Short
    }

    private fun bump(n: Int) {
        val v = Store.add(n)
        overlay.update(v, Lines.forCount(v))
    }

    private fun ignorable(pkg: String) =
        pkg == packageName || pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard")

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return

        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg == IG) overlay.show()
            else if (pkg == YT) { /* shown/hidden by check() depending on Shorts */ }
            else if (!ignorable(pkg)) { overlay.hide(); lastSig = null; candidate = null }
        }
        if (pkg != IG && pkg != YT) return
        if (pkg == IG && Store.mode != 2) return   // YouTube always needs the check (Shorts-only visibility)
        schedule(350)
    }

    private fun schedule(delay: Long) {
        if (pending) return           // throttle (not debounce): video progress bars fire events constantly
        pending = true
        h.postDelayed(check, delay)
    }

    private val check = Runnable {
        pending = false
        val root = rootInActiveWindow ?: return@Runnable
        val pkg = root.packageName?.toString()
        val s = Scan()
        when (pkg) {
            IG -> scan(root, IG_MARKERS, IG_KEYS, s, false)
            YT -> scan(root, YT_MARKERS, YT_KEYS, s, false)
            else -> return@Runnable
        }
        val now = SystemClock.elapsedRealtime()

        // YouTube: monster only while a Short is open. 1.5 s grace so opening comments etc. doesn't flicker it away.
        if (pkg == YT) {
            if (s.inReels) { lastShortsAt = now; overlay.show() }
            else if (now - lastShortsAt > 1500) overlay.hide()
            else schedule(500)
        }
        if (Store.mode != 2) return@Runnable

        if (!s.inReels) {
            if (lastSig != null) {
                if (notInReelsSince == 0L) notInReelsSince = now
                else if (now - notInReelsSince > 5000) { lastSig = null; candidate = null; notInReelsSince = 0 }
            }
            return@Runnable
        }
        notInReelsSince = 0
        val sig = s.sb.toString()
        Log.d(TAG, "pkg=$pkg sig=$sig")
        if (sig.isBlank()) return@Runnable

        if (sig == lastSig) { candidate = null; return@Runnable }
        if (sig == candidate && now - lastCountAt >= 500) {
            lastSig = sig; candidate = null; lastCountAt = now
            bump(1)
        } else {
            // First time we see this signature: wait for it to be stable (avoids counting mid-swipe A+B mixes).
            candidate = sig
            schedule(400)
        }
    }

    private class Scan { var inReels = false; val sb = StringBuilder(); var budget = 1500 }

    private fun scan(n: AccessibilityNodeInfo?, markers: List<String>, keys: List<String>, s: Scan, inKey: Boolean) {
        if (n == null || s.budget-- <= 0) return
        val id = n.viewIdResourceName ?: ""
        if (markers.any { id.contains(it) }) s.inReels = true
        val k = inKey || keys.any { id.contains(it) }
        if (k && n.isVisibleToUser) {
            n.text?.toString()?.let { if (it.isNotBlank() && !NUMERIC.matches(it.trim())) s.sb.append(it).append('|') }
            n.contentDescription?.toString()?.let { if (it.isNotBlank() && !NUMERIC.matches(it.trim())) s.sb.append(it).append('|') }
        }
        for (i in 0 until n.childCount) scan(n.getChild(i), markers, keys, s, k)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }
}
