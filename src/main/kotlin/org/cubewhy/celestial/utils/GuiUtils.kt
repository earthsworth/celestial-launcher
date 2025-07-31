/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.utils

import org.cubewhy.celestial.launcherFrame
import java.awt.Component
import java.awt.Desktop
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.util.EventObject
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter


fun chooseFile(filter: FileFilter? = null): File? {
    val fileDialog = JFileChooser()
    if (filter != null) {
        fileDialog.fileFilter = filter
        fileDialog.addChoosableFileFilter(filter)
    }
    fileDialog.fileSelectionMode = JFileChooser.FILES_ONLY
    return if ((fileDialog.showOpenDialog(launcherFrame) == JFileChooser.CANCEL_OPTION)) null else fileDialog.selectedFile
}

fun chooseFolder(): File? {
    val fileDialog = JFileChooser()
    fileDialog.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    return if ((fileDialog.showOpenDialog(launcherFrame) == JFileChooser.CANCEL_OPTION)) null else fileDialog.selectedFile
}

fun createButtonOpenFolder(text: String?, folder: File): JButton {
    val btn = JButton(text)
    btn.addActionListener {
        folder.mkdirs()
        Desktop.getDesktop().open(folder)
    }
    return btn
}


fun saveFile(filter: FileNameExtensionFilter?): File? {
    val fileDialog = JFileChooser()
    fileDialog.fileFilter = filter
    fileDialog.addChoosableFileFilter(filter)
    fileDialog.fileSelectionMode = JFileChooser.FILES_ONLY
    return if ((fileDialog.showSaveDialog(launcherFrame) == JFileChooser.CANCEL_OPTION)) null else fileDialog.selectedFile
}

fun createJButton(text: String, func: (e: ActionEvent) -> Unit) =
    JButton(text).apply {
        this.addActionListener { func(it) }
    }

/**
 * Get a empty label
 * */
fun emptyJLabel() = JLabel()


fun String.toJLabel(): JLabel =
    // todo multi line support
    JLabel(this)

fun String.toJButton(func: ActionListener) =
    JButton(this).apply {
        addActionListener(func)
    }

fun JTextArea.readOnly(): JTextArea {
    this.isEditable = false
    return this
}


fun <T : SwingConstants> EventObject.source(): T {
    return this.source as T
}

fun Component.withScroller(
    vsbPolicy: Int = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    hsbPolicy: Int = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
) =
    JScrollPane(this, vsbPolicy, hsbPolicy).let {
        it.verticalScrollBar.unitIncrement = 30
        it
    }

fun String.toJTextArea(): JTextArea = JTextArea(this)
