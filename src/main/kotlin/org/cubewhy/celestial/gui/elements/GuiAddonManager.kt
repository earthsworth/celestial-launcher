/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.gui.elements

import org.cubewhy.celestial.*
import org.cubewhy.celestial.event.impl.AddonAddEvent
import org.cubewhy.celestial.files.DownloadManager
import org.cubewhy.celestial.game.AddonType
import org.cubewhy.celestial.game.BaseAddon
import org.cubewhy.celestial.game.addon.FabricMod
import org.cubewhy.celestial.game.addon.JavaAgent
import org.cubewhy.celestial.game.addon.JavaAgent.Companion.add
import org.cubewhy.celestial.game.addon.JavaAgent.Companion.migrate
import org.cubewhy.celestial.game.addon.JavaAgent.Companion.setArgFor
import org.cubewhy.celestial.game.addon.LunarCNMod
import org.cubewhy.celestial.game.addon.WeaveMod
import org.cubewhy.celestial.game.addon.WeaveMod.Companion.add
import org.cubewhy.celestial.gui.GuiLauncher
import org.cubewhy.celestial.gui.layouts.VerticalFlowLayout
import org.cubewhy.celestial.utils.chooseFile
import org.cubewhy.celestial.utils.createButtonOpenFolder
import org.cubewhy.celestial.utils.currentJavaExec
import org.cubewhy.celestial.utils.format
import org.cubewhy.celestial.utils.isMod
import org.cubewhy.celestial.utils.openAsJar
import org.cubewhy.celestial.utils.readOnly
import org.cubewhy.celestial.utils.isZipFile
import org.cubewhy.celestial.utils.source
import org.cubewhy.celestial.utils.toJButton
import org.cubewhy.celestial.utils.toJLabel
import org.cubewhy.celestial.utils.toJTextArea
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.net.URI
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter


class GuiAddonManager : JPanel() {
    private val tab = JTabbedPane()
    private val lunarcnList = DefaultListModel<LunarCNMod>()
    private val weaveList = DefaultListModel<WeaveMod>()
    private val agentList = DefaultListModel<JavaAgent>()
    private val fabricList = DefaultListModel<FabricMod>()

    private val toggleWeave = JMenuItem("toggle")
    private val toggleCN = JMenuItem("toggle")
    private val toggleAgent = JMenuItem("toggle")
    private val toggleAgentClasspath = JMenuItem("toggle-classpath")

    private val log = LoggerFactory.getLogger(DownloadManager::class.java)

    init {
        this.border = TitledBorder(
            null,
            t.getString("gui.addons.title"),
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null,
            Color.orange
        )
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)

        this.initGui()
    }

    private fun initGui() {
        // load items
        loadAgents()
        loadWeaveMods()
        loadLunarCNMods()
        loadFabricMods()

        val jListLunarCN = JList(lunarcnList)
        val jListWeave = JList(weaveList)
        val jListAgents = JList(agentList)
        val jListFabric = JList(fabricList)

        toggleWeave.addActionListener {
            jListWeave.selectedValue.toggle()
            weaveList.removeAllElements()
            loadWeaveMods()
        }
        toggleCN.addActionListener {
            jListLunarCN.selectedValue.toggle()
            lunarcnList.removeAllElements()
            loadLunarCNMods()
        }
        toggleAgent.addActionListener {
            jListAgents.selectedValue.toggle()
            agentList.removeAllElements()
            loadAgents()
        }

        toggleAgentClasspath.addActionListener {
            jListAgents.selectedValue.toggleClasspath()
            agentList.removeAllElements()
            loadAgents()
        }

        // menus
        val agentMenu = JPopupMenu()
        agentMenu.add(toggleAgent)
        agentMenu.add(toggleAgentClasspath)
        val manageArg = JMenuItem(t.getString("gui.addon.agents.arg"))
        val removeAgent = JMenuItem(t.getString("gui.addon.agents.remove"))
        val renameAgent = JMenuItem(t.getString("gui.addon.rename"))
        agentMenu.add(manageArg)
        agentMenu.add(renameAgent)
        agentMenu.addSeparator()
        agentMenu.add(removeAgent)

        manageArg.addActionListener {
            // open a dialog
            val currentAgent = jListAgents.selectedValue
            val newArg =
                JOptionPane.showInputDialog(this, t.getString("gui.addon.agents.arg.message"), currentAgent.arg)
            if (newArg != null && currentAgent.arg != newArg) {
                setArgFor(currentAgent, newArg)
                if (newArg.isBlank()) {
                    GuiLauncher.statusBar.text =
                        String.format(t.getString("gui.addon.agents.arg.remove.success"), currentAgent.file.name)
                } else {
                    GuiLauncher.statusBar.text = String.format(
                        t.getString("gui.addon.agents.arg.set.success"),
                        currentAgent.file.name,
                        newArg
                    )
                }
                agentList.clear()
                loadAgents()
            }
        }

        removeAgent.addActionListener {
            val currentAgent = jListAgents.selectedValue
            val name = currentAgent.file.name
            if (JOptionPane.showConfirmDialog(
                    this,
                    t.format("gui.addon.agents.remove.confirm.message", name),
                    t.getString("gui.addon.agents.remove.confirm.title"),
                    JOptionPane.OK_CANCEL_OPTION
                ) == JOptionPane.OK_OPTION && currentAgent.file.delete()
            ) {
                GuiLauncher.statusBar.text = t.format("gui.addon.agents.remove.success", name)
                agentList.clear()
                loadAgents()
            }
        }

        renameAgent.addActionListener {
            val currentAgent = jListAgents.selectedValue
            val file = currentAgent.file
            val name = file.name
            val newName = JOptionPane.showInputDialog(
                this,
                t.getString("gui.addon.rename.dialog.message"),
                name.substring(0, name.length - 4)
            )
            if (newName != null && file.renameTo(File(file.parentFile, "$newName.jar"))) {
                log.info("Rename agent $name -> $newName.jar")
                GuiLauncher.statusBar.text = t.format("gui.addon.rename.success", newName)
                // rename the name in the config
                migrate(name, "$newName.jar")
                agentList.clear()
                loadAgents()
            }
        }


        // weave menu
        val weaveMenu = JPopupMenu()
        weaveMenu.add(toggleWeave)
        val renameWeaveMod = JMenuItem(t.getString("gui.addon.rename"))
        val removeWeaveMod = JMenuItem(t.getString("gui.addon.mods.weave.remove"))
        weaveMenu.add(renameWeaveMod)
        weaveMenu.addSeparator()
        weaveMenu.add(removeWeaveMod)

        renameWeaveMod.addActionListener {
            val currentMod = jListWeave.selectedValue
            val file = currentMod.file
            val name = file.name
            val newName = JOptionPane.showInputDialog(
                this,
                t.getString("gui.addon.rename.dialog.message"),
                name.substring(0, name.length - 4)
            )
            if (newName != null && file.renameTo(File(file.parentFile, "$newName.jar"))) {
                log.info(String.format("Rename weave mod %s -> %s", name, "$newName.jar"))
                GuiLauncher.statusBar.text = t.format("gui.addon.rename.success", newName)
                weaveList.clear()
                loadWeaveMods()
            }
        }

        removeWeaveMod.addActionListener {
            val currentMod = jListWeave.selectedValue
            val name = currentMod.file.name
            if (JOptionPane.showConfirmDialog(
                    this,
                    t.format("gui.addon.mods.weave.remove.confirm.message", name),
                    t.getString("gui.addon.mods.weave.remove.confirm.title"),
                    JOptionPane.OK_CANCEL_OPTION
                ) == JOptionPane.OK_OPTION && currentMod.file.delete()
            ) {
                GuiLauncher.statusBar.text =
                    String.format(t.getString("gui.addon.mods.weave.remove.success"), name)
                weaveList.clear()
                loadWeaveMods()
            }
        }

        val lunarCNMenu = JPopupMenu()
        lunarCNMenu.add(toggleCN)
        val renameLunarCNMod = JMenuItem(t.getString("gui.addon.rename"))
        val removeLunarCNMod = JMenuItem(t.getString("gui.addon.mods.cn.remove"))
        lunarCNMenu.add(renameLunarCNMod)
        lunarCNMenu.addSeparator()
        lunarCNMenu.add(removeLunarCNMod)

        renameLunarCNMod.addActionListener {
            val currentMod = jListLunarCN.selectedValue
            val file = currentMod.file
            val name = file.name
            val newName = JOptionPane.showInputDialog(
                this,
                t.getString("gui.addon.rename.dialog.message"),
                name.substring(0, name.length - 4)
            )
            if (newName != null && file.renameTo(File(file.parentFile, "$newName.jar"))) {
                log.info(String.format("Rename LunarCN mod %s -> %s", name, "$newName.jar"))
                GuiLauncher.statusBar.text = String.format(t.getString("gui.addon.rename.success"), newName)
                lunarcnList.clear()
                loadLunarCNMods()
            }
        }

        removeLunarCNMod.addActionListener {
            val currentMod = jListLunarCN.selectedValue
            val name = currentMod.file.name
            if (JOptionPane.showConfirmDialog(
                    this,
                    String.format(t.getString("gui.addon.mods.cn.remove.confirm.message"), name),
                    t.getString("gui.addon.mods.cn.remove.confirm.title"),
                    JOptionPane.OK_CANCEL_OPTION
                ) == JOptionPane.OK_OPTION && currentMod.file.delete()
            ) {
                GuiLauncher.statusBar.text = t.format("gui.addon.mods.cn.remove.success", name)
                lunarcnList.clear()
                loadLunarCNMods()
            }
        }

        val fabricMenu = JPopupMenu()
        val renameFabricMod = JMenuItem(t.getString("gui.addon.rename"))
        val removeFabricMod = JMenuItem(t.getString("gui.addon.mods.fabric.remove"))

        renameFabricMod.addActionListener {
            val currentMod = jListFabric.selectedValue
            val file = currentMod.file
            val name = file.name
            val newName = JOptionPane.showInputDialog(
                this,
                t.getString("gui.addon.rename.dialog.message"),
                name.substring(0, name.length - 4)
            )
            if (newName != null && file.renameTo(File(file.parentFile, "$newName.jar"))) {
                log.info(String.format("Rename Fabric mod %s -> %s", name, "$newName.jar"))
                GuiLauncher.statusBar.text = String.format(t.getString("gui.addon.rename.success"), newName)
                fabricList.clear()
                loadFabricMods()
            }
        }

        removeFabricMod.addActionListener {
            val currentMod = jListFabric.selectedValue
            val name = currentMod.file.name
            if (JOptionPane.showConfirmDialog(
                    this,
                    t.format("gui.addon.mods.fabric.remove.confirm.message", name),
                    t.getString("gui.addon.mods.fabric.remove.confirm.title"),
                    JOptionPane.OK_CANCEL_OPTION
                ) == JOptionPane.OK_OPTION && currentMod.file.delete()
            ) {
                GuiLauncher.statusBar.text =
                    t.format("gui.addon.mods.fabric.remove.success", name)
                fabricList.clear()
                loadFabricMods()
            }
        }

        fabricMenu.add(renameFabricMod)
        fabricMenu.addSeparator()
        fabricMenu.add(removeFabricMod)

        // bind menus
        bindMenu(jListLunarCN, lunarCNMenu)
        bindMenu(jListWeave, weaveMenu)
        bindMenu(jListFabric, fabricMenu)
        bindMenu(jListAgents, agentMenu)


        // buttons
        val btnAddLunarCNMod = JButton(t.getString("gui.addon.mods.add"))
        val btnAddWeaveMod = JButton(t.getString("gui.addon.mods.add"))
        val btnAddFabric = JButton(t.getString("gui.addon.mods.add"))
        val btnAddAgent = JButton(t.getString("gui.addon.agents.add"))

        btnAddAgent.addActionListener {
            val file = chooseFile(FileNameExtensionFilter("Agent", "jar"))
            if (file == null) {
                log.info("Cancel add agent because file == null")
                return@addActionListener
            }
            val arg = JOptionPane.showInputDialog(this, t.getString("gui.addon.agents.add.arg"))
            try {
                val agent = add(file, arg)
                if (agent != null) {
                    // success
                    AddonAddEvent(AddonType.JAVAAGENT, agent)
                    GuiLauncher.statusBar.text = t.getString("gui.addon.agents.add.success")
                    agentList.clear()
                    loadAgents()
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        t.getString("gui.addon.agents.add.failure.exists"),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } catch (e: IOException) {
                val trace = e.stackTraceToString()
                log.error(trace)
                JOptionPane.showMessageDialog(
                    this,
                    t.format("gui.addon.agents.add.failure.io", trace),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        btnAddWeaveMod.addActionListener {
            val file = chooseFile(FileNameExtensionFilter("Weave Mod", "jar")) ?: return@addActionListener
            try {
                if (!(file.openAsJar().isMod(AddonType.WEAVE))) {
                    JOptionPane.showMessageDialog(
                        this,
                        t.format("gui.addon.mods.incorrect", file),
                        "Warning | Type incorrect",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
                val mod = add(file)
                if (mod != null) {
                    // success
                    AddonAddEvent(AddonType.WEAVE, mod)
                    GuiLauncher.statusBar.text = t.getString("gui.addon.mods.weave.add.success")
                    weaveList.clear()
                    loadWeaveMods()
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        t.getString("gui.addon.mods.weave.add.failure.exists"),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } catch (e: IOException) {
                val trace = e.stackTraceToString()
                log.error(trace)
                JOptionPane.showMessageDialog(
                    this,
                    t.format("gui.addon.mods.weave.add.failure.io", trace),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        btnAddLunarCNMod.addActionListener {
            val file = chooseFile(FileNameExtensionFilter("LunarCN Mod", "jar")) ?: return@addActionListener
            try {
                if (!(file.openAsJar().isMod(AddonType.LUNARCN))) {
                    JOptionPane.showMessageDialog(
                        this,
                        t.format("gui.addon.mods.incorrect", file),
                        "Warning | Type incorrect",
                        JOptionPane.WARNING_MESSAGE
                    )
                }
                val mod = LunarCNMod.add(file)
                if (mod != null) {
                    // success
                    AddonAddEvent(AddonType.LUNARCN, mod)
                    GuiLauncher.statusBar.text = t.getString("gui.addon.mods.cn.add.success")
                    weaveList.clear()
                    loadWeaveMods()
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        t.getString("gui.addon.mods.cn.add.failure.exists"),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } catch (e: IOException) {
                val trace = e.stackTraceToString()
                log.error(trace)
                JOptionPane.showMessageDialog(
                    this,
                    t.format("gui.addon.mods.cn.add.failure.io", trace),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        btnAddFabric.addActionListener {
            val file = chooseFile(FileNameExtensionFilter("Fabric Mod", "jar"))
            if (file == null) {
                log.info("Cancel add fabric mod because file == null")
                return@addActionListener
            }
            try {
                val mod = FabricMod.add(file)
                if (mod != null) {
                    // success
                    AddonAddEvent(AddonType.FABRIC, mod)
                    GuiLauncher.statusBar.text = t.getString("gui.addon.mods.fabric.add.success")
                    fabricList.clear()
                    loadFabricMods()
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        t.getString("gui.addon.mods.fabric.add.failure.exists"),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } catch (e: IOException) {
                val trace = e.stackTraceToString()
                log.error(trace)
                JOptionPane.showMessageDialog(
                    this,
                    t.format("gui.addon.mods.fabric.add.failure.io", trace),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }

        // panels
        val panelLunarCN = JPanel()
        panelLunarCN.name = "cn"
        panelLunarCN.layout = BoxLayout(panelLunarCN, BoxLayout.Y_AXIS)
        panelLunarCN.add(SearchableList(lunarcnList, jListLunarCN))
        val btnPanel1 = JPanel()
        btnPanel1.layout = BoxLayout(btnPanel1, BoxLayout.X_AXIS)
        btnPanel1.add(btnAddLunarCNMod)
        btnPanel1.add(createButtonOpenFolder(t.getString("gui.addon.folder"), LunarCNMod.modFolder))
        panelLunarCN.add(btnPanel1)

        val panelWeave = JPanel()
        panelWeave.name = "weave"
        panelWeave.layout = BoxLayout(panelWeave, BoxLayout.Y_AXIS)
        panelWeave.add(SearchableList(weaveList, jListWeave))
        val btnPanel2 = JPanel()
        btnPanel2.layout = BoxLayout(btnPanel2, BoxLayout.X_AXIS)
        btnPanel2.add(btnAddWeaveMod)
        btnPanel2.add(createButtonOpenFolder(t.getString("gui.addon.folder"), WeaveMod.modFolder))
        panelWeave.add(btnPanel2)

        val panelAgents = JPanel()
        panelAgents.name = "agents"
        panelAgents.layout = BoxLayout(panelAgents, BoxLayout.Y_AXIS)
        panelAgents.add(SearchableList(agentList, jListAgents))
        val btnPanel3 = JPanel()
        btnPanel3.layout = BoxLayout(btnPanel3, BoxLayout.X_AXIS)
        btnPanel3.add(btnAddAgent)
        btnPanel3.add(createButtonOpenFolder(t.getString("gui.addon.folder"), JavaAgent.javaAgentFolder))
        panelAgents.add(btnPanel3)

        val panelFabric = JPanel()
        panelFabric.name = "fabric"
        panelFabric.layout = BoxLayout(panelFabric, BoxLayout.Y_AXIS)
        panelFabric.add("Not fully support Fabric yet, please add mods by your self".toJLabel())
        //        panelFabric.add(new JScrollPane(jListFabric));
        val btnPanel4 = JPanel()
        btnPanel4.layout = BoxLayout(btnPanel4, BoxLayout.X_AXIS)
        //        btnPanel4.add(btnAddFabric);
        btnPanel4.add(createButtonOpenFolder(t.getString("gui.addon.folder"), FabricMod.modFolder))
        panelFabric.add(btnPanel4)

        val panelCelePatch = JPanel()
        panelCelePatch.name = "celepatch"
        panelCelePatch.layout = GridBagLayout()
        panelCelePatch.transferHandler = PatchTransferHandler()
        val gbc = GridBagConstraints()

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.CENTER
        // todo fix drag for Windows

        panelCelePatch.add(t.getString("gui.addons.patch.drag").toJButton {
            chooseFile(FileNameExtensionFilter("Celestial Patch", "jar"))?.let {
                PatchDialog(panelCelePatch, it).isVisible = true
            }
        }, gbc)

        tab.addTab(t.getString("gui.addons.agents"), panelAgents)
        tab.addTab(t.getString("gui.addons.mods.cn"), panelLunarCN)
        tab.addTab(t.getString("gui.addons.mods.weave"), panelWeave)
        tab.addTab(t.getString("gui.addons.mods.fabric"), panelFabric)

        tab.addTab(t.getString("gui.addons.patch"), panelCelePatch)

        this.add(tab)
        tab.addChangeListener {
            // refresh a mod list
            autoRefresh(tab.selectedComponent as JPanel)
        }
    }

    private fun autoRefresh(panel: JPanel) {
        val name = panel.name
        log.debug("Refreshing mod list $name (Focus changed)")
        when (name) {
            "agents" -> {
                agentList.clear()
                loadAgents()
            }

            "weave" -> {
                weaveList.clear()
                loadWeaveMods()
            }

            "cn" -> {
                lunarcnList.clear()
                loadLunarCNMods()
            }

            "fabric" -> {
                fabricList.clear()
                loadFabricMods()
            }
        }
    }

    private fun loadFabricMods() {
        for (mod in FabricMod.findAll()) {
            fabricList.addElement(mod)
        }
    }

    private fun loadLunarCNMods() {
        for (lunarCNMod in LunarCNMod.findAll()) {
            lunarcnList.addElement(lunarCNMod)
        }
    }

    private fun bindMenu(list: JList<out BaseAddon>, menu: JPopupMenu) {
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val index = list.locationToIndex(e.point)
                    list.selectedIndex = index
                    val current = list.selectedValue ?: return
                    if (current.isEnabled) {
                        toggleWeave.text = t.getString("gui.addon.toggle.disable")
                        toggleAgent.text = t.getString("gui.addon.toggle.disable")
                        toggleCN.text = t.getString("gui.addon.toggle.disable")
                    } else {
                        toggleWeave.text = t.getString("gui.addon.toggle.enable")
                        toggleAgent.text = t.getString("gui.addon.toggle.enable")
                        toggleCN.text = t.getString("gui.addon.toggle.enable")
                    }
                    if (current is JavaAgent) {
                        if (current.classpath) {
                            toggleAgentClasspath.text = t.getString("gui.addon.agents.cp.disable")
                        } else {
                            toggleAgentClasspath.text = t.getString("gui.addon.agents.cp.enable")
                        }
                    }
                    menu.show(list, e.x, e.y)
                }
            }
        })
    }

    private fun loadWeaveMods() {
        for (weaveMod in WeaveMod.findAll()) {
            weaveList.addElement(weaveMod)
        }
    }

    private fun loadAgents() {
        for (javaAgent in JavaAgent.findAll()) {
            agentList.addElement(javaAgent)
        }
    }

    private class PatchTransferHandler : TransferHandler() {
        companion object {
            private var log = LoggerFactory.getLogger(PatchTransferHandler::class.java)
        }

        override fun canImport(support: TransferSupport): Boolean {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor)
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }

            val data = support.transferable.getTransferData(DataFlavor.stringFlavor) as String
            val patch = File(URI.create(data))
            log.info("Dragged patch: $patch")
            if (!patch.exists()) return false
            val panel = support.component as JPanel
            SwingUtilities.invokeLater {
                PatchDialog(panel, patch).isVisible = true
            }

            return true
        }
    }
}

class PatchDialog(panel: JPanel, private val patch: File) : JDialog(SwingUtilities.getWindowAncestor(panel) as JFrame) {

    init {
        this.title = t.getString("gui.addons.patch.title")
        this.layout = VerticalFlowLayout()
        this.modalityType = ModalityType.APPLICATION_MODAL
        this.isLocationByPlatform = true

        this.setSize(600, 600)

        initGui()
    }

    fun initGui() {
        val gameDir = File(config.installationDir)

        this.add(patch.path.toJTextArea().readOnly())
        this.add(t.getString("gui.addons.patch.warn").toJTextArea().readOnly())
        val lunarJar = gameDir.resolve("lunar.jar")
        if (!(gameDir.exists() || lunarJar.exists())) {
            this.add(t.getString("gui.addons.patch.not-installed").toJLabel())
            return
        }
        if (!patch.isZipFile()) {
            this.add(t.getString("gui.addons.patch.not-zip").toJLabel())
        }
        this.add(JButton(t.getString("gui.addons.patch.confirm")).apply {
            addActionListener { e ->
                // java -jar patch.jar lunar.jar out.jar
                val source = e.source<JButton>()
                source.isEnabled = false
                Thread {
                    val code = doPatch(lunarJar).start().waitFor()
                    source.text =
                        if (code == 0) t.getString("gui.addons.patch.done") else t.getString("gui.addons.patch.fail")
                }.start()
            }
        })
    }

    private fun doPatch(lunar: File): ProcessBuilder {
        val builder = ProcessBuilder()
        builder.command(currentJavaExec.path, "-jar", patch.path, lunar.path, lunar.path)
        return builder
    }
}
