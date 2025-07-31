/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.elements.help

import org.cubewhy.celestial.t
import org.cubewhy.celestial.gui.elements.HelpPage
import org.cubewhy.celestial.utils.readOnly
import org.cubewhy.celestial.utils.toJTextArea

class HelpModLoader : HelpPage(t.getString("gui.help.loader.title")) {
    init {
        this.add(t.getString("gui.help.loader").toJTextArea().readOnly())
    }
}