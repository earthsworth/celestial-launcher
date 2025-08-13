/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LunarAccountConfig(
    val activeAccountLocalId: String? = null,
    val accounts: MutableMap<String, LunarAccount> = mutableMapOf(),
)

@Serializable
data class LunarAccount(
    var accessToken: String? = null,
    var accessTokenExpiresAt: String,
    val eligibleForMigration: Boolean = false,
    val hasMultipleProfiles: Boolean = false,
    val legacy: Boolean = false,
    val persistent: Boolean = true,
    @SerialName("userProperites") // lol lunar made a mistake
    val userProperties: List<String> = listOf(),
    val localId: String,
    var refreshToken: String? = null,
    val minecraftProfile: LunarMinecraftProfile,
    var remoteId: String, // xbox id, same in the jwt
    val type: String = "Xbox",
    val username: String,
) {
    val offline: Boolean
        get() {
            return accessToken == null || refreshToken == null || refreshToken == "OFFLINE_ALT"
        }
}

@Serializable
data class LunarMinecraftProfile(
    val id: String, // mc uuid
    val name: String, // in game name
)