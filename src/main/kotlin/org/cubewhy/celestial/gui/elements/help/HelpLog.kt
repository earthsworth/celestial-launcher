/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.elements.help

import org.cubewhy.celestial.t
import org.cubewhy.celestial.launcherLogFile
import org.cubewhy.celestial.gui.elements.HelpPage
import org.cubewhy.celestial.gui.layouts.VerticalFlowLayout
import org.cubewhy.celestial.utils.createButtonOpenFolder
import org.cubewhy.celestial.utils.readOnly
import org.cubewhy.celestial.utils.toJTextArea

class HelpLog : HelpPage("Log") {
    init {
        this.layout = VerticalFlowLayout()
        this.add(t.getString("gui.help.log").toJTextArea().readOnly())
        this.add(createButtonOpenFolder(t.getString("gui.settings.folder.log"), launcherLogFile.parentFile))
    }
}