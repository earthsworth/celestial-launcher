/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.utils

import org.cubewhy.celestial.game.AddonType
import org.cubewhy.celestial.gui.LauncherMainWindow
import java.io.*
import java.util.jar.JarFile
import java.util.zip.ZipFile


fun String.toFile(): File = File(this)


fun String.getInputStream(): InputStream? = LauncherMainWindow::class.java.getResourceAsStream(this)

fun File.openAsJar(): JarFile = JarFile(this)
fun File.openAsZip(): ZipFile = ZipFile(this)

fun ZipFile.unzipTo(targetDir: File) {
    for (entry in this.entries()) {
        val out = File(targetDir, entry!!.name)
        if (entry.isDirectory) {
            out.mkdirs()
        } else {
            out.parentFile.mkdirs()
            out.createNewFile()
            val entryInputStream = this.getInputStream(entry)
            FileOutputStream(out).use { fileOutPutStream ->
                fileOutPutStream.write(entryInputStream.readAllBytes())
            }
        }
    }
}


/**
 * Is mod
 *
 * @param type type of the addon (WEAVE, LUNARCN only)
 * @return yes or no
 * */
fun JarFile.isMod(type: AddonType): Boolean =
    when (type) {
        AddonType.LUNARCN -> this.getJarEntry("lunarcn.mod.json") != null
        AddonType.WEAVE -> this.getJarEntry("weave.mod.json") != null
        else -> throw IllegalStateException(type.name + " is not a type of Lunar mods!")
    }

fun File.isZipFile(): Boolean {
    try {
        FileInputStream(this).use { fis ->
            val header = ByteArray(4)
            if (fis.read(header) != 4) {
                return false
            }
            return isZipSignature(header)
        }
    } catch (e: IOException) {
        return false
    }
}

private fun isZipSignature(header: ByteArray): Boolean {
    return header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
}
