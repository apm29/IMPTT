package com.imptt.apm29.utilities

import java.text.SimpleDateFormat
import java.util.*

/**
 *  author : ciih
 *  date : 2020/9/24 11:29 AM
 *  description :
 */
/**
 * 返回文字描述的日期
 *
 * @param time 时间戳  秒
 * @return String类型的文字描述
 */
fun getTimeFormatText(time: Int?): String? {
    if (time == null) {
        return ""
    }
    val second: Long = 1 // 1秒
    val minute = 60 * second // 1分钟
    val hour = 60 * minute // 1小时
    val day = 24 * hour // 1天
    val month = 31 * day // 月
    val year = 12 * month // 年

    //当前时间
    val newTime = (System.currentTimeMillis() / 1000).toInt()
    //相差时间
    val diff = newTime - time
    if (diff >= year) {
        val formatter = SimpleDateFormat("yyyy-MM-dd",Locale.getDefault())
        return formatter.format(java.lang.Long.valueOf(time.toString() + "000"))
    }
    if (diff in day until year) {
        val formatter = SimpleDateFormat("MM-dd",Locale.getDefault())
        return formatter.format(java.lang.Long.valueOf(time.toString() + "000"))
    }
    if (diff in hour until day) {
        return (diff / hour).toString() + "小时前"
    }
    return if (diff in minute until hour) {
        (diff / minute).toString() + "分钟前"
    } else "刚刚"
}