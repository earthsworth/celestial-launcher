/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */
package org.cubewhy.celestial.game.addon

import org.cubewhy.celestial.JSON
import org.cubewhy.celestial.JavaagentConfiguration
import org.cubewhy.celestial.config
import org.cubewhy.celestial.configDir
import org.cubewhy.celestial.game.BaseAddon
import org.cubewhy.celestial.utils.GitUtils
import org.cubewhy.celestial.utils.javaExecUsedToLaunchGame
import org.jetbrains.annotations.Contract
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.jar.JarFile

private val log: Logger = LoggerFactory.getLogger(JavaAgent::class.java)

class JavaAgent : BaseAddon {
    /**
     * -- GETTER --
     * Get agent arg
     */
    var arg: String? = ""
        private set

    /**
     * -- GETTER --
     * Get a file of the javaagent
     */
    val file: File

    /**
     * Should add the Javaagent into classpath?
     * */
    var classpath: Boolean

    var agentManifest: JavaagentManifest? = null

    val hasExtraUi: Boolean
        get() = agentManifest?.extraConfigMain != null

    /**
     * Create an instance
     *
     * @param path Path to JavaAgent
     * @param arg  Arg of the JavaAgent
     */
    constructor(path: String, arg: String? = null, classpath: Boolean = true) {
        this.file = File(path)
        this.arg = arg
        this.classpath = classpath
        agentManifest = parseManifest()
    }

    /**
     * Create a instance
     *
     * @param file File of the JavaAgent
     * @param arg  Arg of the JavaAgent
     */
    constructor(file: File, arg: String? = null, classpath: Boolean = true) {
        this.file = file
        this.arg = arg
        this.classpath = classpath
        agentManifest = parseManifest()
    }

    val jvmArg: String
        /**
         * Get args which add to the jvm
         *
         * @return args for jvm
         */
        get() {
            var jvmArgs = "-javaagent:$file"
            if (arg?.isNotEmpty() == true) {
                jvmArgs += "=" + this.arg
            }
            return jvmArgs
        }

    override fun toString(): String {
        var result = (if (classpath) "" else "[*] ") + file.name
        if (arg?.isNotBlank() == true) {
            result += "=" + this.arg
        }
        return result
    }

    override val isEnabled: Boolean
        get() = file.name.endsWith(".jar")

    override fun toggle(): Boolean {
        if (isEnabled) {
            migrate(file.name, file.name + ".disabled")
        } else {
            migrate(file.name, file.name.substring(0, file.name.length - 9))
        }
        return toggle(file)
    }

    fun parseManifest(): JavaagentManifest? {
        val content = try {      // open the jar file
            val jar = JarFile(this.file)
            // get the entry
            val entry = jar.getEntry("celestial.manifest.json")
            jar.getInputStream(entry).readAllBytes().toString(Charsets.UTF_8)
        } catch (_: NullPointerException) {
            // ignored
            return null
        }
        try {
            // parse json
            return JSON.decodeFromString(JavaagentManifest.serializer(), content)
        } catch (e: RuntimeException) {
            // invalid manifest
            log.error("Invalid manifest of javaagent ${this.file}", e)
            return null
        }
    }

    fun openConfigWindow() {
        if (!this.hasExtraUi || this.agentManifest == null) return // there's no extra config ui defined in this agent
        val java = File(javaExecUsedToLaunchGame)
        val commandList = mutableListOf<String>()
        commandList.add(java.absolutePath)
        commandList.add("-cp")
        commandList.add(this.file.absolutePath)
        // celestial properties
        // Celestial version
        commandList.add("-Dcelestial.version=${GitUtils.buildVersion}")
        // agent arg
        this.arg?.let { commandList.add("-Dcelestial.agent.arg=$it") }
        // installation path
        commandList.add("-Dcelestial.game.installation=${config.installationDir}")
        // main class
        commandList.add(this.agentManifest!!.extraConfigMain!!)

        // build command
        val pb = ProcessBuilder(commandList)
            .redirectErrorStream(true)

        val process = pb.start()
        log.info("Started extra config ui (pid = ${process.pid()})")
        Thread {
            // redirect logs
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lines().forEach { line -> log.info(line) }
            }
        }.start()
    }

    fun toggleClasspath(): Boolean {
        val ja = config.game.javaagents
        if (!ja.containsKey(file.name)) {
            ja[file.name] = JavaagentConfiguration(arg, !classpath)
        } else {
            ja[file.name]?.classpath = !classpath
        }
        return ja[file.name]?.classpath == true
    }

    companion object {
        val javaAgentFolder: File = File(configDir, "javaagents") // share with LunarCN Launcher
        private val log: Logger = LoggerFactory.getLogger(JavaAgent::class.java)

        init {
            if (!javaAgentFolder.exists()) {
                log.info("Making javaagents folder")
                javaAgentFolder.mkdirs() // create the javaagents folder
            }
        }

        /**
         * Find all mods in the weave mods folder
         */
        @Contract(pure = true)
        fun findEnabled(): MutableList<JavaAgent> {
            val list: MutableList<JavaAgent> = ArrayList()
            if (javaAgentFolder.isDirectory) {
                for (file in Objects.requireNonNull<Array<File>>(javaAgentFolder.listFiles())) {
                    if (file.name.endsWith(".jar") && file.isFile) {
                        val javaagentConfiguration = findAgentConfig(file.name)
                        list.add(JavaAgent(file, javaagentConfiguration.arg, javaagentConfiguration.classpath))
                    }
                }
            }
            return list
        }

        private fun findDisabled(): List<JavaAgent> {
            val list: MutableList<JavaAgent> = ArrayList()
            if (javaAgentFolder.isDirectory) {
                for (file in Objects.requireNonNull<Array<File>>(javaAgentFolder.listFiles())) {
                    if (file.name.endsWith(".jar.disabled") && file.isFile) {
                        val jaConfig = findAgentConfig(file.name)
                        list.add(JavaAgent(file, jaConfig.arg, jaConfig.classpath))
                    }
                }
            }
            return list
        }


        fun findAll(): List<JavaAgent> {
            val list = findEnabled()
            list.addAll(findDisabled())
            return Collections.unmodifiableList(list)
        }

        /**
         * Set param for a Javaagent
         *
         * @param agent the agent
         * @param arg   param of the agent
         */

        fun setArgFor(agent: JavaAgent, arg: String?) {
            setArgFor(agent.file.name, arg)
        }

        /**
         * Set param for a Javaagent
         *
         * @param name name of the agent
         * @param arg  param of the agent
         */
        fun setArgFor(name: String, arg: String?) {
            val ja = config.game.javaagents
            if (!ja.containsKey(name)) {
                ja[name] = JavaagentConfiguration(arg)
            } else {
                ja[name]?.arg = arg
            }
        }

        private fun findAgentConfig(name: String): JavaagentConfiguration {
            val ja = config.game.javaagents
            if (!ja.containsKey(name)) {
                // create config for the agent
                ja[name] = JavaagentConfiguration() // leave empty
            }
            return ja[name] ?: JavaagentConfiguration()
        }


        fun add(file: File, arg: String?): JavaAgent? {
            val target = autoCopy(file, javaAgentFolder)
            if (arg != null) {
                setArgFor(file.name, arg)
            }
            return if ((target == null)) null else JavaAgent(target, arg)
        }

        /**
         * Migrate the arg of an agent
         *
         * @param old name of the old agent
         * @param n3w name of the new agent
         */
        fun migrate(old: String, n3w: String) {
            val ja = config.game.javaagents
            val arg = if (ja[old] == null) JavaagentConfiguration() else ja[old]
            ja[n3w] = arg // leave empty
            ja.remove(old)
        }
    }
}
