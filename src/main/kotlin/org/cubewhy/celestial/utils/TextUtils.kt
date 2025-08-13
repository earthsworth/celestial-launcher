/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import java.util.*
import kotlin.random.Random

fun ResourceBundle.format(key: String, vararg args: Any?): String =
    this.getString(key).format(*args)

fun safeConvertStringToUuid(str: String?): UUID? {
    if (str.isNullOrEmpty()) return null
    return try {
        UUID.fromString(str)

    } catch (_: IllegalArgumentException) {
        null
    }
}

fun randomDigitString(length: Int): String {
    val sb = StringBuilder(length)
    repeat(length) {
        sb.append(Random.nextInt(0, 10))
    }
    return sb.toString()
}