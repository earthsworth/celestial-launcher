/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.gui.elements

import com.sun.tools.attach.AttachNotSupportedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import org.cubewhy.celestial.*
import org.cubewhy.celestial.event.EventManager
import org.cubewhy.celestial.event.EventTarget
import org.cubewhy.celestial.event.impl.APIReadyEvent
import org.cubewhy.celestial.event.impl.GameStartEvent
import org.cubewhy.celestial.event.impl.GameTerminateEvent
import org.cubewhy.celestial.game.GameProperties
import org.cubewhy.celestial.game.LaunchCommandJson
import org.cubewhy.celestial.game.addon.LunarCNMod
import org.cubewhy.celestial.game.addon.WeaveMod
import org.cubewhy.celestial.game.thirdparty.LunarQT
import org.cubewhy.celestial.gui.LauncherMainWindow.Companion.statusBar
import org.cubewhy.celestial.utils.*
import org.cubewhy.celestial.utils.lunar.GameArtifactInfo
import org.cubewhy.celestial.utils.lunar.LunarApiClient.Companion.getMainClass
import org.cubewhy.celestial.utils.lunar.LunarApiClient.Companion.getSupportModules
import org.cubewhy.celestial.utils.lunar.LunarApiClient.Companion.getSupportVersions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.GridLayout
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.filechooser.FileNameExtensionFilter

private val log: Logger = LoggerFactory.getLogger(GuiVersionList::class.java)

class GuiVersionList : JPanel() {
    private val versionSelect = JComboBox<String>()
    private val moduleSelect = JComboBox<String>()
    private val branchInput = JTextField()
    private var isFinishOk = false
    private val btnOnline: JButton = JButton(t.getString("gui.version.online"))
    private val btnOffline: JButton = JButton(t.getString("gui.version.offline"))
    private var isLaunching = false

    init {
        EventManager.register(
            this
        )
        this.border = TitledBorder(
            null,
            t.getString("gui.version-select.title"),
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null,
            Color.orange
        )
        this.layout = GridLayout(5, 2, 5, 5)
    }

    @EventTarget
    fun onAPIReady(e: APIReadyEvent) {
        this.removeAll()
        this.isFinishOk = false
        versionSelect.removeAllItems()
        initGui()
    }

    private fun initGui() {
        this.add(JLabel(t.getString("gui.version-select.label.version")))
        this.add(versionSelect)
        this.add(JLabel(t.getString("gui.version-select.label.module")))
        this.add(moduleSelect)
        this.add(JLabel(t.getString("gui.version-select.label.branch")))
        this.add(branchInput)

        // add items
        val map = getSupportVersions(metadata)
        @Suppress("UNCHECKED_CAST") val supportVersions: List<String> = map["versions"] as ArrayList<String>
        for (version in supportVersions) {
            versionSelect.addItem(version)
        }
        versionSelect.addActionListener {
            try {
                refreshModuleSelect(this.isFinishOk)
                if (this.isFinishOk) {
                    saveVersion()
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }
        moduleSelect.addActionListener {
            if (this.isFinishOk) {
                saveModule()
            }
        }
        refreshModuleSelect(false)
        // get is first launch
        if (config.game.target == null) {
            val game = GameVersionInfo(
                versionSelect.selectedItem as String,
                moduleSelect.selectedItem as String,
                "master"
            )
            config.game.target = game
            versionSelect.selectedItem = map["default"]
        }
        initInput(versionSelect, moduleSelect, branchInput)
        isFinishOk = true

        // add launch buttons
        btnOnline.addActionListener {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    this@GuiVersionList.onlineLaunch()
                }
            } catch (e: Exception) {
                log.error(e.stackTraceToString())
            }
        }
        this.add(btnOnline)

        this.add(btnOffline)
        btnOffline.addActionListener {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    this@GuiVersionList.offlineLaunch()
                }
            } catch (e: IOException) {
                log.error(e.stackTraceToString())
            } catch (e: InterruptedException) {
                log.error(e.stackTraceToString())
            } catch (_: AttachNotSupportedException) {
                log.warn("Failed to attach to the game process")
            }
        }

        val btnWipeCache = JButton(t.getString("gui.version.cache.wipe"))

        btnWipeCache.addActionListener {
            if (JOptionPane.showConfirmDialog(
                    this,
                    t.getString("gui.version.cache.warn"),
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                t.getString("gui.version.cache.start").updateStatusText()
                try {
                    if (wipeCache(null)) {
                        t.getString("gui.version.cache.success").updateStatusText()
                    } else {
                        t.getString("gui.version.cache.failure").updateStatusText()
                    }
                } catch (ex: IOException) {
                    throw RuntimeException(ex)
                }
            }
        }
        this.add(btnWipeCache)

        val btnFetchJson = JButton(t.getString("gui.version.fetch"))

        btnFetchJson.addActionListener {
            // open file save dialog
            CoroutineScope(Dispatchers.IO).launch {
                val file = saveFile(FileNameExtensionFilter("Json (*.json)", "json"))
                file?.apply {
                    log.info("Fetching version json...")

                    val json = lunarApiClient.launchVersion(
                        versionSelect.selectedItem as String,
                        branchInput.text,
                        moduleSelect.selectedItem as String,
                    )
                    var file1 = this
                    if (!this.name.endsWith(".json")) {
                        file1 = file + ".json" // add extension
                    }
                    log.info("Fetch OK! Dumping to ${file1.path}")
                    FileUtils.write(
                        file1,
                        JSON.encodeToString(GameArtifactInfo.serializer(), json),
                        StandardCharsets.UTF_8
                    )
                }
            }
        }

        this.add(btnFetchJson)
    }


    private suspend fun beforeLaunch() {
        if (gamePid.get() != 0L) {
            if (findJava(/*if (config.celeWrap.state) CeleWrap.MAIN_CLASS else */getMainClass(null)) != null) {
                JOptionPane.showMessageDialog(
                    this,
                    t.getString("gui.version.launched.message"),
                    t.getString("gui.version.launched.title"),
                    JOptionPane.WARNING_MESSAGE
                )
            } else {
                gamePid.set(0)
                statusBar.isRunningGame = false
            }
        }
        // check update for loaders
        val weave = config.addon.weave
        val cn = config.addon.lunarcn
        var checkUpdate = false

        try {
            if (weave.state && weave.checkUpdate) {
                log.info("Checking update for Weave loader")
                checkUpdate = WeaveMod.checkUpdate()
            }
            if (cn.state && cn.checkUpdate) {
                log.info("Checking update for LunarCN loader")
                checkUpdate = LunarCNMod.checkUpdate()
            }
        } catch (e: Exception) {
            log.error("Failed to check loader updates")
            log.error(e.stackTraceToString())
            if (!config.proxy.mirror.containsKey("github.com:443") && JOptionPane.showConfirmDialog(
                    this,
                    t.getString("gui.proxy.suggest.gh"),
                    "Apply GitHub Mirror",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                log.info("Applying GitHub mirror")
                // TODO github.ink is died
                config.proxy.mirror["github.com:443"] = "github.ink:443"
            }
        }

        try {
            if (config.addon.lcqt.state && config.addon.lcqt.checkUpdate) {
                log.info("Checking update for LunarQT")
                checkUpdate = LunarQT.checkUpdate()
            }
        } catch (e: Exception) {
            log.error("Failed to check lcqt updates")
            log.error(e.stackTraceToString())
        }

        if (checkUpdate) {
            t.getString("gui.addon.update").updateStatusText()
        }
    }

    @EventTarget
    fun onGameStart(event: GameStartEvent) {
        t.format("status.launch.started", event.pid).updateStatusText()
    }

    @EventTarget
    fun onGameTerminate(event: GameTerminateEvent) {
        t.getString("status.launch.terminated").updateStatusText()
        if (event.code != 0) {
            // upload crash report
            t.getString("status.launch.crashed").updateStatusText()
            log.info("Client looks crashed (code ${event.code})")
            JOptionPane.showMessageDialog(
                this,
                String.format(
                    t.getString("gui.message.clientCrash2"),
                    launcherLogFile.path,
                    t.getString("gui.version.crash.tip")
                ),
                "Game crashed!",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }


    private suspend fun onlineLaunch() {
        beforeLaunch()
        val version = versionSelect.selectedItem as String
        val module = moduleSelect.selectedItem as String
        val branch = branchInput.text
        val launchCommand = getArgs(
            version, branch, module, File(config.installationDir),
            gameProperties = GameProperties(
                config.game.resize.width,
                config.game.resize.height,
                File(config.game.gameDir)
            )
        )
        // save launch command
        log.info("Saving launch command to $launchJson")
        launchJson.writeText(
            JSON.encodeToString(
                LaunchCommandJson.serializer(),
                LaunchCommandJson.create(launchCommand)
            )
        )
        log.info("Generating launch scripts...")
        launchScript.writeText(generateScripts())

        CoroutineScope(Dispatchers.IO).launch {
            isLaunching = true
            t.getString("status.launch.begin").updateStatusText()
            try {
                checkUpdate(
                    (versionSelect.selectedItem as String),
                    moduleSelect.selectedItem as String,
                    branchInput.text
                )
            } catch (e: Exception) {
                log.error("Failed to check update")
                val trace = e.stackTraceToString()
                log.error(trace)
                JOptionPane.showMessageDialog(
                    null,
                    t.format("gui.check-update.error.message", trace),
                    t.getString("gui.check-update.error.title"),
                    JOptionPane.ERROR_MESSAGE
                )
            }
            log.info("Everything is OK, starting game...")
            isLaunching = false
            launch(launchCommand).waitFor()
        }
    }

    // TODO: move to another class
    private suspend fun offlineLaunch() {
        beforeLaunch()
        Thread {
            t.getString("status.launch.call-process").updateStatusText()
            launchPrevious().waitFor()
        }.start()
    }

    private fun initInput(versionSelect: JComboBox<String>, moduleSelect: JComboBox<String>, branchInput: JTextField) {
        val game = config.game.target!!
        versionSelect.selectedItem = game.version
        moduleSelect.selectedItem = game.module
        branchInput.text = game.branch
    }

    private fun saveVersion() {
        val version = versionSelect.selectedItem as String
        log.info("Select version -> $version")
        config.game.target?.version = version
    }

    private fun saveModule() {
        if (moduleSelect.selectedItem == null) {
            return
        }
        val module = moduleSelect.selectedItem as String
        log.info("Select module -> $module")
        config.game.target?.module = module
    }


    private fun refreshModuleSelect(reset: Boolean) {
        moduleSelect.removeAllItems()
        if (versionSelect.selectedItem == null) {
            return
        }
        val map = getSupportModules(metadata, (versionSelect.selectedItem as String))
        @Suppress("UNCHECKED_CAST") val modules: List<String> = map["modules"] as ArrayList<String>
        val defaultValue = map["default"] as String?
        for (module in modules) {
            moduleSelect.addItem(module)
        }
        if (reset) {
            moduleSelect.selectedItem = defaultValue
        }
    }
}

fun File.unzipNatives(baseDir: File) {
    log.info("Unzipping natives ${this.path}")
    val dir = File(baseDir, "natives")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    this.openAsZip().unzipTo(dir)
    log.info("Natives unzipped successful")
}

fun File.unzipUi(baseDir: File) {
    log.info("Unzipping ui.zip ${this.path}")
    val dir = File(baseDir, "ui")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    this.openAsZip().unzipTo(dir)
    log.info("Ui unzipped successful")
}

private operator fun File.plus(s: String): File {
    return File(this.path + s)
}
