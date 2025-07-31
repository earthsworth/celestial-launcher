/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import java.awt.Desktop
import java.net.URI

fun String.toURI(): URI = URI.create(this)

fun URI.open() {
    try {
        Desktop.getDesktop().browse(this)
    } catch (e: UnsupportedOperationException) {
        // open with native methods
        when (OSEnum.current) {
            OSEnum.Linux -> Runtime.getRuntime().exec("xdg-open $this")
            OSEnum.MacOS, OSEnum.MacOSX, OSEnum.Darwin -> Runtime.getRuntime().exec("open $this")
            OSEnum.Windows -> Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $this")
            else -> throw RuntimeException(e) // really unsupported
        }
    }
}

