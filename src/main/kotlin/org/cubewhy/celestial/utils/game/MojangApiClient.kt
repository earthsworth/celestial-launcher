/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.utils.game

import kotlinx.serialization.Serializable
import okhttp3.coroutines.executeAsync
import org.cubewhy.celestial.JSON
import org.cubewhy.celestial.utils.RequestUtils.get
import org.cubewhy.celestial.utils.string
import org.cubewhy.celestial.utils.stringAsync
import java.net.URL

object MojangApiClient {
    private var versionManifest: URL = URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
    var texture: URL = URL("https://resources.download.minecraft.net")


    suspend fun manifest(): MinecraftManifest {
        get(versionManifest).executeAsync().use { response ->
            return JSON.decodeFromString(response.body.stringAsync())
        }
    }

    /**
     * Get information of a Minecraft release
     *
     * @param version version id
     * @return version json
     */
    suspend fun getVersion(version: String, manifest: MinecraftManifest): MinecraftArtifactInfo? {
        for (versionInfo in manifest.versions) {
            if (versionInfo.id == version) {
                val url = versionInfo.url
                get(url).executeAsync().use { response ->
                    return JSON.decodeFromString(response.body.stringAsync())
                }
            }
        }
        return null
    }

    /**
     * Get texture index (Minecraft)
     *
     * @param info json object from MinecraftData.getVersion
     * @return json of texture index
     */
    suspend fun getTextureIndex(info: MinecraftArtifactInfo): MinecraftResources {
        val url = URL(info.assetIndex.url)
        get(url).executeAsync().use { response ->
            return JSON.decodeFromString(response.string!!)
        }
    }
}

@Serializable
data class MinecraftResources(
    val objects: Map<String, Resource>
) {
    @Serializable
    data class Resource(
        val hash: String,
        val size: Int
    )
}

@Serializable
data class MinecraftManifest(
    val versions: List<MinecraftVersionInfo>
) {
    @Serializable
    data class MinecraftVersionInfo(
        val id: String,
        val url: String
    )
}

@Serializable
data class MinecraftArtifactInfo(
    val assets: String,
    val assetIndex: AssetIndex
) {
    @Serializable
    data class AssetIndex(
        val id: String,
        val sha1: String,
        val url: String
    )
}
