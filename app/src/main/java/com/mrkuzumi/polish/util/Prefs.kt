package com.mrkuzumi.polish.util

import android.content.Context

/**
 * 轻量本地偏好存储（SharedPreferences，无额外依赖）
 */
object Prefs {
    private const val NAME = "polish_prefs"
    private const val KEY_GENDER = "gender"

    fun getGender(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_GENDER, null)

    fun setGender(context: Context, gender: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GENDER, gender)
            .apply()
    }
}
