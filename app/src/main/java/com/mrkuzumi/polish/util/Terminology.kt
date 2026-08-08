package com.mrkuzumi.polish.util

import android.content.Context

/** 根据性别切换术语：男 → 🦌/磨剑，女 → ⛏/挖矿 */
object Terminology {
    private fun isFemale(ctx: Context) = Prefs.getGender(ctx) == "female"

    fun emoji(ctx: Context) = if (isFemale(ctx)) "⛏" else "🦌"
    fun verb(ctx: Context) = if (isFemale(ctx)) "挖矿" else "磨剑"
    fun emojiWithCount(ctx: Context, count: Int) = "${emoji(ctx)}×$count"
    fun defaultUsername(ctx: Context) = "${verb(ctx)}用户"

    /** 通知文案 */
    fun reminderTitle(ctx: Context) = "该${verb(ctx)}了！"
    fun reminderBody(ctx: Context, dateStr: String) =
        "$dateStr 的${verb(ctx)}日到了，记得准时${verb(ctx)}！(^^ゞ"
    fun reminderBigText(ctx: Context, dateStr: String) =
        "$dateStr 的${verb(ctx)}日到了！\n点此打开${verb(ctx)}记录今天份的练习。"

    /** 预约弹窗 */
    fun bookingTitle(ctx: Context) = "预约${verb(ctx)}日？"
    fun bookingBody(ctx: Context, dateText: String) =
        "要为 $dateText 约定一个${verb(ctx)}日提醒吗？\n到了那天记得准时${verb(ctx)}！(^^ゞ"

    /** 通知渠道 */
    fun channelName(ctx: Context) = "${verb(ctx)}提醒"
    fun channelDesc(ctx: Context) = "预约${verb(ctx)}日定时通知"
}
