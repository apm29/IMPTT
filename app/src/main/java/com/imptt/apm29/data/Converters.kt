package com.imptt.apm29.data

import androidx.room.TypeConverter
import java.util.*

/**
 *  author : ciih
 *  date : 2020/9/15 9:08 AM
 *  description :
 */
class Converters {
    @TypeConverter
    fun calendarToDatestamp(calendar: Calendar): Long = calendar.timeInMillis

    @TypeConverter
    fun datestampToCalendar(value: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = value }
}