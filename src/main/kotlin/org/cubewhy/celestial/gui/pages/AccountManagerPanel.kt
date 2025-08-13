/*
 * Celestial Launcher <me@lunarclient.top>
 * License under GPLv3
 * Do NOT remove this note if you want to copy this file.
 */

package org.cubewhy.celestial.gui.pages

import org.cubewhy.celestial.entities.LunarAccount
import org.cubewhy.celestial.game.AccountManager
import org.cubewhy.celestial.gui.elements.PlaceholderTextField
import org.cubewhy.celestial.t
import org.cubewhy.celestial.utils.safeConvertStringToUuid
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.border.TitledBorder

private data class LunarAccountState(
    val lunarAccount: LunarAccount,
    val isActive: Boolean,
) {
    override fun toString(): String {
        val prefix = mutableListOf<String>()
        if (this.isActive) {
            prefix.add("[ACTIVE]")
        }
        if (this.lunarAccount.offline) {
            prefix.add("[OFFLINE]")
        }
        return "${prefix.joinToString(" ")} ${lunarAccount.username} (${lunarAccount.minecraftProfile.id})"
    }
}

class AccountManagerPanel : JPanel() {
    private val tab: JTabbedPane
    private val accountList = DefaultListModel<LunarAccountState>()

    init {
        this.name = "account-manager"
        this.border = TitledBorder(
            null,
            t.getString("gui.account-manager.title"),
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            null,
            Color.orange
        )
        this.tab = JTabbedPane()
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.init()
    }

    private fun init() {
        this.add(tab)
        val accountPanel = JPanel()
        accountPanel.layout = BorderLayout()
        tab.addTab("Accounts", accountPanel)

        AccountManager.accountConfig.accounts.values.forEach { account ->
            this.addAccountToUi(account, AccountManager.accountConfig.activeAccountLocalId == account.localId)
        }

        accountPanel.add(JList(this.accountList), BorderLayout.CENTER)
        makeOfflineAccountPanel(accountPanel)
    }

    private fun makeOfflineAccountPanel(accountPanel: JPanel) {
        val createOfflineAccountPanel = JPanel(GridBagLayout()).apply {
            border = TitledBorder(
                null,
                "Offline Account",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                null,
                Color.ORANGE
            )
        }

        val pad = Insets(6, 8, 6, 8)

        fun gbc(
            x: Int, y: Int,
            wx: Double = 0.0, wy: Double = 0.0,
            fill: Int = GridBagConstraints.NONE,
            anchor: Int = GridBagConstraints.WEST,
            gw: Int = 1
        ) = GridBagConstraints().apply {
            gridx = x; gridy = y
            weightx = wx; weighty = wy
            this.fill = fill; this.anchor = anchor
            gridwidth = gw
            insets = pad
        }

        val usernameLabel = JLabel("Username:")
        val usernameField = JTextField(20)
        val uuidLabel = JLabel("UUID:")
        val uuidField = PlaceholderTextField(20)
        uuidField.placeholder = "<auto generated>"
        val btnCreateOfflineAccount = JButton("Add Account")

        createOfflineAccountPanel.add(usernameLabel, gbc(0, 0))
        createOfflineAccountPanel.add(
            usernameField, gbc(
                x = 1, y = 0, wx = 1.0, fill = GridBagConstraints.HORIZONTAL
            )
        )

        createOfflineAccountPanel.add(uuidLabel, gbc(0, 1))
        createOfflineAccountPanel.add(
            uuidField, gbc(
                x = 1, y = 1, wx = 1.0, fill = GridBagConstraints.HORIZONTAL
            )
        )

        createOfflineAccountPanel.add(
            btnCreateOfflineAccount, gbc(
                x = 0, y = 2, gw = 2, anchor = GridBagConstraints.EAST
            )
        )

        createOfflineAccountPanel.add(
            JLabel("Note: You must add the browser debugger agent to use offline accounts"), gbc(
                x = 0, y = 2, gw = 2, anchor = GridBagConstraints.WEST
            )
        )

        btnCreateOfflineAccount.addActionListener {
            val username = usernameField.text
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val uuid = safeConvertStringToUuid(uuidField.text)
                ?: if (uuidField.text.isNullOrEmpty()) UUID.randomUUID() else null
            // valid uuid
            if (uuid == null) {
                JOptionPane.showMessageDialog(this, "Bad UUID format", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }

            if (AccountManager.accountConfig.accounts.any {
                    it.value.username.contentEquals(
                        username,
                        true
                    ) || it.value.minecraftProfile.id == uuid.toString()
                }) {
                JOptionPane.showMessageDialog(
                    this,
                    "Account with same username or uuid already exist",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return@addActionListener
            }

            if (this.addAccount(username, uuid)) {
                // clean fields
                usernameField.text = ""
                uuidField.text = ""
            }

        }

        accountPanel.add(createOfflineAccountPanel, BorderLayout.NORTH)
    }

    private fun addAccount(username: String, uuid: UUID): Boolean {
        val account = AccountManager.addOfflineAccount(username, uuid)
        this.accountList.addElement(LunarAccountState(account, false))
        return true
    }


    private fun addAccountToUi(account: LunarAccount, isActive: Boolean) {
        accountList.addElement(LunarAccountState(account, isActive))
    }
}