package com.samuelcruz.bangtinnha.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val VN_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

/** 24950 → "24.950". */
fun formatDong(value: Long): String = "%,d".format(value).replace(',', '.')

/** Hundredths of a đồng → "26.345,50" (Vietnamese separators); whole values drop the decimals. */
fun formatHundredths(value: Long): String {
    val whole = value / 100
    val cents = value % 100
    return if (cents == 0L) formatDong(whole) else "${formatDong(whole)},${"%02d".format(cents)}"
}

fun formatTime(epochMillis: Long, pattern: String = "HH:mm dd/MM"): String =
    DateTimeFormatter.ofPattern(pattern).format(Instant.ofEpochMilli(epochMillis).atZone(VN_ZONE))

/** "5 phút", "3 giờ", "2 ngày" for how long ago [epochMillis] was. */
fun formatAge(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = (now - epochMillis) / 60_000
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút"
        minutes < 48 * 60 -> "${minutes / 60} giờ"
        else -> "${minutes / (24 * 60)} ngày"
    }
}
