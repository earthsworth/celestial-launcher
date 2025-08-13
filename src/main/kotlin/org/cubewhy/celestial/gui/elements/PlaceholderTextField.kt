/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.elements

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JTextField
import javax.swing.text.Document

// https://stackoverflow.com/questions/16213836/java-swing-jtextfield-set-placeholder
class PlaceholderTextField : JTextField {
    var placeholder: String? = null

    constructor()

    constructor(
        pDoc: Document?,
        pText: String?,
        pColumns: Int
    ) : super(pDoc, pText, pColumns)

    constructor(pColumns: Int) : super(pColumns)

    constructor(pText: String?) : super(pText)

    constructor(pText: String?, pColumns: Int) : super(pText, pColumns)

    override fun paintComponent(pG: Graphics?) {
        super.paintComponent(pG)

        if (placeholder == null || placeholder!!.isEmpty() || getText().isNotEmpty()) {
            return
        }

        val g = pG as Graphics2D
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        g.color = disabledTextColor
        if (placeholder != null) {
            g.drawString(
                placeholder!!, getInsets().left, pG.fontMetrics
                    .maxAscent + getInsets().top
            )
        }
    }
}