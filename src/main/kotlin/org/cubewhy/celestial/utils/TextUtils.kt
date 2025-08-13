/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import java.util.*

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
