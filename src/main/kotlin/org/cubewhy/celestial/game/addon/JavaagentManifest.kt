/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.game.addon

import kotlinx.serialization.Serializable

@Serializable
data class JavaagentManifest(
    val extraConfigMain: String?,
)
