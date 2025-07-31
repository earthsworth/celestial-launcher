/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.pages

import org.cubewhy.celestial.t
import org.cubewhy.celestial.utils.GitUtils.branch
import org.cubewhy.celestial.utils.GitUtils.buildUser
import org.cubewhy.celestial.utils.GitUtils.buildUserEmail
import org.cubewhy.celestial.utils.GitUtils.buildVersion
import org.cubewhy.celestial.utils.GitUtils.commitMessage
import org.cubewhy.celestial.utils.GitUtils.commitTime
import org.cubewhy.celestial.utils.GitUtils.getCommitId
import org.cubewhy.celestial.utils.GitUtils.remote
import org.cubewhy.celestial.utils.readOnly
import org.cubewhy.celestial.utils.toJTextArea
import java.awt.Color
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.TitledBorder


class AboutPanel : JPanel() {
    init {
        this.name = "about"
        this.border = TitledBorder(
            null,
            t.getString("gui.about.title"),
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null,
            Color.orange
        )
        val message = """
                Celestial v${buildVersion} (Running on Java ${System.getProperty("java.version")})
                -----
                Git build info:
                    Build user: $buildUser
                    Email: $buildUserEmail
                    Remote ($branch): $remote
                    Commit time: $commitTime
                    Commit: ${getCommitId(true)}
                    Commit Message: $commitMessage
                
                """.trimIndent()

        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val textArea = (t.getString("gui.about") + "\n" + message).toJTextArea().readOnly()
        this.add(textArea)
    }
}
