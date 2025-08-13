/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.game

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.cubewhy.celestial.JSON
import org.cubewhy.celestial.entities.LunarAccount
import org.cubewhy.celestial.entities.LunarAccountConfig
import org.cubewhy.celestial.entities.LunarMinecraftProfile
import org.cubewhy.celestial.utils.randomDigitString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random

object AccountManager {
    private val log: Logger = LoggerFactory.getLogger(AccountManager::class.java)
    val accountsFile = File(System.getProperty("user.home"), ".lunarclient/settings/game/accounts.json")

    val accountConfig: LunarAccountConfig by lazy {
        // get config file
        // ~/.lunarclient/settings/game/accounts.json
        if (!(accountsFile.exists())) {
            return@lazy LunarAccountConfig() // first run
        }
        val content = accountsFile.readText()
        // parse json
        JSON.decodeFromString(LunarAccountConfig.serializer(), content)
    }

    fun addOfflineAccount(username: String, uuid: UUID): LunarAccount {
        val localId = UUID.randomUUID().toString().replace("-", "")
        // generate fake jwt
        val remoteId = randomDigitString(16)
        val algorithm = Algorithm.HMAC384("celestial")
        val jwt = JWT.create()
            .withClaim("xuid", remoteId)
            .withClaim("agg", "Adult")
            .withClaim("auth", "XBOX")
            .withClaim("ns", "default")
            .withClaim("flags", listOf<String>("multiplayer", "twofactorauth", "msamigration_stage4", "orders_2022"))
            .withClaim("profiles", mapOf("mc" to uuid.toString()))
            .withClaim("platform", "PC_LAUNCHER")
            .withClaim(
                "pfd", listOf(
                    mapOf(
                        "type" to "mc",
                        "id" to uuid.toString(),
                        "name" to username,
                    )
                )
            )
        val fakeAccessToken = jwt.sign(algorithm)
        val account = LunarAccount(
            accessToken = fakeAccessToken,
            accessTokenExpiresAt = Instant.now().plus(10, ChronoUnit.DAYS).toString(),
            localId = localId,
            minecraftProfile = LunarMinecraftProfile(uuid.toString().replace("-", ""), username),
            username = username,
            refreshToken = "OFFLINE_ALT",
            remoteId = remoteId
        )
        accountConfig.accounts[localId] = account
        accountsFile.writeText(JSON.encodeToString(this.accountConfig))
        return account
    }

    fun fixBrokenOfflineAccounts() {
        var hasModify = false
        for (entry in accountConfig.accounts.entries) {
            val key = entry.key
            val value = entry.value

            val remoteId = Random.nextInt(1000, 1000000)
            val algorithm = Algorithm.HMAC384("celestial")
            val jwt = JWT.create()
                .withClaim("xuid", remoteId)
                .sign(algorithm)

            if (value.accessToken == null || value.refreshToken == null) {
                // generate fake jwt
                value.accessToken = jwt
                value.refreshToken = "OFFLINE_ALT"
                value.remoteId = remoteId.toString()
                value.accessTokenExpiresAt = Instant.now().plus(10, ChronoUnit.DAYS).toString()
                accountConfig.accounts[key] = value
                hasModify = true
                log.info("Fixed broken offline account ${value.username} (${value.minecraftProfile.id})")
            }

            // parse instant
            try {
                val instant = Instant.parse(value.accessTokenExpiresAt)
                if (instant.isBefore(Instant.now())) {
                    value.accessTokenExpiresAt = Instant.now().plus(10, ChronoUnit.DAYS).toString()
                    accountConfig.accounts[key] = value
                    hasModify = true
                }
            } catch (_: RuntimeException) {
                value.accessTokenExpiresAt = Instant.now().plus(10, ChronoUnit.DAYS).toString()
                accountConfig.accounts[key] = value
                hasModify = true
            }
        }
        if (hasModify) {
            accountsFile.writeText(JSON.encodeToString(this.accountConfig))
        }
    }
}