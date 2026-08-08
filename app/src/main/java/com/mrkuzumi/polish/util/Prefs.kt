package com.mrkuzumi.polish.util

import android.content.Context

/**
 * 轻量本地偏好存储（SharedPreferences，无额外依赖）
 */
object Prefs {
    private const val NAME = "polish_prefs"
    private const val KEY_GENDER = "gender"
    private const val KEY_USERNAME = "username"
    private const val KEY_BOOKED = "booked_dates"

    fun getGender(context: Context): String? =
        prefs(context).getString(KEY_GENDER, null)

    fun setGender(context: Context, gender: String) {
        prefs(context).edit().putString(KEY_GENDER, gender).apply()
    }

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun setUsername(context: Context, name: String) {
        prefs(context).edit().putString(KEY_USERNAME, name).apply()
    }

    // ---- 已预约的未来日期 ----
    fun getBookedDates(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BOOKED, emptySet()) ?: emptySet()

    fun addBookedDate(context: Context, dateIso: String) {
        val set = getBookedDates(context).toMutableSet()
        set.add(dateIso)
        prefs(context).edit().putStringSet(KEY_BOOKED, set).apply()
    }

    fun removeBookedDate(context: Context, dateIso: String) {
        val set = getBookedDates(context).toMutableSet()
        set.remove(dateIso)
        prefs(context).edit().putStringSet(KEY_BOOKED, set).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
