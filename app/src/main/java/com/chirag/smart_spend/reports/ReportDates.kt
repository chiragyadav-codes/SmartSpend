package com.chirag.smart_spend.reports

import java.util.Calendar

object ReportDates {

    fun startOfDay(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfDay(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    fun startOfMonth(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(c)
    }

    fun endOfMonth(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(c)
    }

    fun startOfYear(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_YEAR, 1)
        return startOfDay(c)
    }

    fun endOfYear(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR))
        return endOfDay(c)
    }
}