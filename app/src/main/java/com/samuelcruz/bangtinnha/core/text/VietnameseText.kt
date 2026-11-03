package com.samuelcruz.bangtinnha.core.text

import java.text.Normalizer

/** Lower-case, accent-free form used to compare Vietnamese place names ("Phường Bình Minh" → "phuong binh minh"). */
fun normalizeVietnamese(text: String): String {
    val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
    return decomposed
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
        .replace(Regex("\\s+"), " ")
        .trim()
}

/** True when [text] contains any of [keywords], ignoring case and accents. No keywords means "match everything". */
fun matchesAnyKeyword(text: String, keywords: List<String>): Boolean {
    val usable = keywords.map(::normalizeVietnamese).filter { it.isNotEmpty() }
    if (usable.isEmpty()) return true
    val haystack = normalizeVietnamese(text)
    return usable.any { it in haystack }
}
