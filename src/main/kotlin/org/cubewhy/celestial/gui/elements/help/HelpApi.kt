/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.elements.help

import org.cubewhy.celestial.gui.elements.HelpPage
import org.cubewhy.celestial.gui.layouts.VerticalFlowLayout
import org.cubewhy.celestial.launcherFrame
import org.cubewhy.celestial.t
import org.cubewhy.celestial.utils.readOnly
import org.cubewhy.celestial.utils.toJTextArea
import javax.swing.JButton

class HelpApi : HelpPage("API") {
    init {
        this.layout = VerticalFlowLayout()
        this.add(t.getString("gui.help.api").toJTextArea().readOnly())
        this.add(JButton(t.getString("gui.settings.title")).let {
            it.addActionListener {
                launcherFrame.cardLayout.show(launcherFrame.mainPanel, "settings")
            }
            it
        })
    }
}
