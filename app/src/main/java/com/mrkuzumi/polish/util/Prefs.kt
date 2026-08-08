package com.mrkuzumi.polish.util

import android.content.Context

/**
 * 轻量本地偏好存储（SharedPreferences，无额外依赖）
 */
object Prefs {
    private const val NAME = "polish_prefs"
    private const val KEY_GENDER = "gender"
    private const val KEY_USERNAME = "username"

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
