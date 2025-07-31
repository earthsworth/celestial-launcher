/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.files

import cn.hutool.crypto.SecureUtil
import okhttp3.coroutines.executeAsync
import org.apache.commons.io.FileUtils
import org.cubewhy.celestial.configDir
import org.cubewhy.celestial.event.impl.FileDownloadEvent
import org.cubewhy.celestial.gui.LauncherMainWindow
import org.cubewhy.celestial.runningOnGui
import org.cubewhy.celestial.utils.RequestUtils.get
import org.cubewhy.celestial.utils.bytesAsync
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

object DownloadManager {
    val cacheDir: File = File(configDir, "cache")
    private val log: Logger = LoggerFactory.getLogger(DownloadManager::class.java)
    private var pool: ExecutorService? = null

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }


    fun waitForAll() {
        if (pool == null) return
        pool!!.shutdown()
        while (!pool!!.awaitTermination(1, TimeUnit.SECONDS)) {
            Thread.onSpinWait()
        }
        // create a new pool (on invoking download)
        pool = null
    }


    /**
     * Cache something
     *
     * @param url      url to the target file (online)
     * @param name     file name
     * @param override allow override?
     * @return status (true=success, false=failure)
     */
    suspend fun cache(url: URL, name: String, override: Boolean): Boolean {
        val file = File(cacheDir, name)
        if (file.exists() && !override) {
            return true
        }
        log.info("Caching $name (from $url)")
        // download
        return download0(url, file)
    }

    /**
     * Download a file
     *
     * @param url url to the target file (online)
     * @param file file instance of the local file
     * @return is success
     */

    internal suspend fun download0(url: URL, file: File, hash: String?, type: Downloadable.Type): Boolean {
        // connect
        if (file.isFile && hash != null) {
            // compare hash
            if (compareHash(file, hash, type)) {
                return true
            }
        }
        FileDownloadEvent(file, FileDownloadEvent.Type.START).call()
        log.info("Downloading $url to $file")
        get(url).executeAsync().use { response ->
            if (!response.isSuccessful) {
                FileDownloadEvent(file, FileDownloadEvent.Type.FAILURE).call()
                return false
            }
            val bytes = response.body.bytesAsync()
            FileUtils.writeByteArrayToFile(file, bytes)
        }
        if (runningOnGui) LauncherMainWindow.statusBar.text = "Download " + file.name + " success."
        if (hash != null) {
            val result = compareHash(file, hash, type)
            if (!result) {
                FileDownloadEvent(file, FileDownloadEvent.Type.FAILURE).call()
            }
            return result
        }
        FileDownloadEvent(file, FileDownloadEvent.Type.SUCCESS).call()
        return true
    }

    private fun compareHash(file: File, hashString: String, hashType: Downloadable.Type): Boolean {
        return hashType == Downloadable.Type.SHA1 && SecureUtil.sha1(file) == hashString || hashType == Downloadable.Type.SHA256 && SecureUtil.sha256(
            file
        ) == hashString
    }


    private suspend fun download0(url: URL, file: File): Boolean {
        return download0(url, file, null, Downloadable.Type.SHA1)
    }


    suspend fun download(downloadable: Downloadable) {
        downloadable.download()
    }
}
