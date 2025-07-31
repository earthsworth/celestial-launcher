/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cubewhy.celestial.files.Downloadable
import org.cubewhy.celestial.game.RemoteAddon
import org.cubewhy.celestial.gui.layouts.VerticalFlowLayout
import org.cubewhy.celestial.t
import org.cubewhy.celestial.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.File
import java.net.URI
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * @param addon remote addon
 * @param file path to save the addon
 * */
class AddonInfoDialog(val addon: RemoteAddon, val file: File) : JDialog() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AddonInfoDialog::class.java)
    }

    private val panel = JPanel()

    init {
        this.title = t.getString("gui.plugins.info.title")
        this.setSize(600, 600)
        this.panel.layout = VerticalFlowLayout()
        this.modalityType = ModalityType.APPLICATION_MODAL
        this.isLocationByPlatform = true
        this.initGui()
    }

    private fun initGui() {
        this.panel.add(JLabel(t.format("gui.plugins.info.name", addon.name)))
        this.panel.add(JLabel(t.format("gui.plugins.info.category", addon.category)))
        val exist = JLabel(t.getString("gui.plugins.exist"))

        val btnDownload = JButton(t.getString("gui.plugins.download"))
        btnDownload.addActionListener {
            CoroutineScope(Dispatchers.IO).launch {
                Downloadable(addon.downloadURL, file, addon.sha1).downloadAsync()
                exist.isVisible = file.exists()
            }

        }

        this.panel.add(exist)
        exist.isVisible = file.exists()

        this.panel.add(btnDownload)
        this.panel.add(JSeparator())

        val metaInfo = JPanel()
        metaInfo.layout = VerticalFlowLayout(VerticalFlowLayout.LEFT)
        metaInfo.border = TitledBorder(
            null,
            t.getString("gui.plugins.info.meta"),
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null,
            Color.orange
        )
        if (addon.meta == null) {
            metaInfo.add(JLabel(t.getString("gui.plugins.info.meta.notfound")))
        } else {
            val meta = addon.meta
            metaInfo.add(JLabel(t.format("gui.plugins.info.meta.name", meta.name)))
            metaInfo.add(JLabel(t.format("gui.plugins.info.meta.version", meta.version)))
            metaInfo.add(JLabel(t.format("gui.plugins.info.meta.description", meta.description)))
            metaInfo.add(JLabel(t.format("gui.plugins.info.meta.authors", meta.authors.getAuthorsString())))
            if (meta.website != null) metaInfo.add(
                createOpenWebsiteButton(
                    t.getString("gui.plugins.info.meta.website"),
                    meta.website.toURI()
                )
            )
            if (meta.repository != null) metaInfo.add(
                createOpenWebsiteButton(
                    t.getString("gui.plugins.info.meta.repo"),
                    meta.repository.toURI()
                )
            )
            if (!meta.dependencies.isNullOrEmpty()) {
                val dependencies = JTextArea()
                dependencies.border = TitledBorder(
                    null,
                    t.getString("gui.plugins.info.meta.dependencies"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION,
                    null,
                    Color.orange
                )
                dependencies.isEditable = false
                val sb = StringBuilder()
                meta.dependencies.forEachIsEnd { item, isEnd ->
                    sb.append(item)
                    if (!isEnd) sb.append("\n")
                }
                metaInfo.add(dependencies.withScroller())
            }
        }
        this.panel.add(metaInfo)
        this.add(this.panel.withScroller())
    }

    private fun createOpenWebsiteButton(text: String, uri: URI): JButton {
        val button = JButton(text)
        button.addActionListener {
            uri.open()
        }
        return button
    }
}

private fun Array<String>.getAuthorsString(): String {
    val sb = StringBuilder()
    this.forEachIndexed { index, author ->
        if (index != 0) {
            if (index != this.size - 1) sb.append(", ") else sb.append(" and ")
        }
        sb.append(author)
        if (this.size == 1) return@forEachIndexed
    }
    return sb.toString()
}


