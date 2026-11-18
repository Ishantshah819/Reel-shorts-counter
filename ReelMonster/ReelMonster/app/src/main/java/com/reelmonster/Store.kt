package com.reelmonster

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** All data lives in SharedPreferences on the phone: survives restarts, no internet, no login. */
object Store {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("reelmonster", Context.MODE_PRIVATE)
    }

    private fun key(cal: Calendar = Calendar.getInstance()) =
        "c_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)

    fun today(): Int = sp.getInt(key(), 0)

    /** Adds n (may be negative) to today's count and returns the new value. commit() = written immediately. */
    fun add(n: Int): Int {
        val v = (today() + n).coerceAtLeast(0)
        sp.edit().putInt(key(), v).commit()
        return v
    }

    /** 1 = manual (tap), 2 = automatic. */
    var mode: Int
        get() = sp.getInt("mode", 1)
        set(v) { sp.edit().putInt("mode", v).commit() }

    /** Estimated seconds one reel costs you. */
    var secsPerReel: Int
        get() = sp.getInt("secs", 30)
        set(v) { sp.edit().putInt("secs", v.coerceIn(5, 180)).commit() }

    var posX: Int
        get() = sp.getInt("px", -1)
        set(v) { sp.edit().putInt("px", v).apply() }
    var posY: Int
        get() = sp.getInt("py", -1)
        set(v) { sp.edit().putInt("py", v).apply() }

    /** Oldest -> newest, 7 entries ending today: (weekday label, count). */
    fun last7(): List<Pair<String, Int>> {
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        return (6 downTo 0).map { back ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -back) }
            fmt.format(cal.time) to sp.getInt(key(cal), 0)
        }
    }

    fun register(l: SharedPreferences.OnSharedPreferenceChangeListener) = sp.registerOnSharedPreferenceChangeListener(l)
    fun unregister(l: SharedPreferences.OnSharedPreferenceChangeListener) = sp.unregisterOnSharedPreferenceChangeListener(l)
}
