package com.benza.smartrooms.util

internal fun String?.orPrettyEmailLocalPart(email: String?): String =
    this?.takeIf(String::isNotBlank)
        ?: email?.substringBefore("@").orEmpty().toPrettyFallbackName()

internal fun String.toPrettyFallbackName(): String {
    val tokens = split(Regex("[\\s._-]+")).filter(String::isNotBlank)
    if (tokens.isEmpty()) return this

    return tokens.joinToString(" ") { token ->
        token.lowercase().replaceFirstChar { char -> char.uppercase() }
    }
}
