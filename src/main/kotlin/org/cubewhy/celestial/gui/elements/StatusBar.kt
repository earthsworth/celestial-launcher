/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.gui.elements

import org.cubewhy.celestial.event.EventManager
import org.cubewhy.celestial.event.EventTarget
import org.cubewhy.celestial.event.impl.GameStartEvent
import org.cubewhy.celestial.event.impl.GameTerminateEvent
import org.cubewhy.celestial.event.impl.UpdateStatusTextEvent
import org.cubewhy.celestial.gamePid
import org.cubewhy.celestial.utils.toJLabel
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer

class StatusBar : JPanel() {
    private val label = JLabel()
    private val pidLabel = "game-pid".toJLabel()
    private val autoClearTimer = Timer(10000) {
        this.clear()
    }

//    val dialog = LogsDialog(t.getString("gui.status.logs"))

    init {
        EventManager.register(this)

        this.layout = BorderLayout(0, 0)
//        val btnOpenDialog =
//            JButton(ImageIcon("/images/logs.png".getInputStream()!!.readAllBytes()))

//        btnOpenDialog.addActionListener {
//            dialog.isVisible = true
//        }
        this.add(label, BorderLayout.WEST)

        val otherComponents = JPanel()
        otherComponents.add(pidLabel)
//        otherComponents.add(btnOpenDialog)

        pidLabel.isVisible = false

        this.add(otherComponents, BorderLayout.EAST)
    }

    private fun clear() {
        this.label.text = ""
    }

    var isRunningGame: Boolean = false
        set(value) {
            this@StatusBar.pidLabel.text = if (value) "PID $gamePid" else "NOT RUNNING"
            this@StatusBar.pidLabel.isVisible = value
            field = value
        }

    var text: String? = null
        set(value) {
            if (this.label.text.isNotEmpty()) {
                autoClearTimer.stop()
            }
            this.label.setText(value)
            if (!value.isNullOrEmpty()) {
//                dialog.addMessage(value)
                autoClearTimer.start()
            }
        }

    @EventTarget
    fun onGameStart(e: GameStartEvent) {
        gamePid.set(e.pid)
        this.isRunningGame = true
    }

    @EventTarget
    fun onGameTerminate(e: GameTerminateEvent) {
        gamePid.set(0)
        this.isRunningGame = false
    }

    @EventTarget
    fun onUpdateStatusText(e: UpdateStatusTextEvent) {
        if (this.label.text.isNotEmpty() && autoClearTimer.isRunning) {
            autoClearTimer.stop()
        }
        this.label.setText(e.text)
        if (e.text.isNotEmpty()) {
            // dialog.addMessage(value)
            autoClearTimer.start()
        }
    }
}

fun String.updateStatusText() {
    UpdateStatusTextEvent(this).call()
}