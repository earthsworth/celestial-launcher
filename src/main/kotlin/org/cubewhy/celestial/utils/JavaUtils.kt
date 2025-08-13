/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import org.cubewhy.celestial.config

val javaExecUsedToLaunchGame: String
    get() =
        config.jre.ifEmpty { currentJavaExec.absolutePath }
