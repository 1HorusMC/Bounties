package com.horus.bounties

import com.horus.bounties.command.BountiesCommand
import com.horus.bounties.command.BountyAdminCommand
import com.horus.bounties.command.BountyCommand
import com.horus.bounties.database.DatabaseConfig
import com.horus.bounties.database.DatabaseManager
import com.horus.bounties.database.DatabaseType
import com.horus.bounties.gui.BountiesGui
import com.horus.bounties.hook.BountiesPlaceholderExpansion
import com.horus.bounties.hook.VaultHook
import com.horus.bounties.listener.CombatListener
import com.horus.bounties.listener.GuiListener
import com.horus.bounties.manager.AntiBoostManager
import com.horus.bounties.manager.AntiBoostSettings
import com.horus.bounties.manager.BountyManager
import com.horus.bounties.manager.KillstreakManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Main plugin entry point for HorusBounties.
 */
class HorusBountiesPlugin : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var bountyManager: BountyManager
        private set
    lateinit var killstreakManager: KillstreakManager
        private set
    lateinit var antiBoostManager: AntiBoostManager
        private set
    lateinit var vaultHook: VaultHook
        private set
    lateinit var bountiesGui: BountiesGui
        private set

    override fun onEnable() {
        saveDefaultConfig()

        vaultHook = VaultHook()
        if (!vaultHook.setupEconomy()) {
            logger.severe("Vault economy provider not found! Disabling HorusBounties.")
            server.pluginManager.disablePlugin(this)
            return
        }

        val dbConfig = buildDatabaseConfig()
        try {
            databaseManager = DatabaseManager(dbConfig)
            bountyManager = BountyManager(databaseManager)
            // Load synchronously: commands/listeners must never run against a half-loaded cache
            bountyManager.loadFromDatabase().get(30, java.util.concurrent.TimeUnit.SECONDS)
        } catch (ex: Exception) {
            logger.severe("Failed to initialize database or load bounties: ${ex.message}. Disabling HorusBounties.")
            server.pluginManager.disablePlugin(this)
            return
        }
        logger.info("Loaded ${bountyManager.getTopBounties().size} active bounties from ${dbConfig.type}.")

        killstreakManager = KillstreakManager()
        antiBoostManager = AntiBoostManager(buildAntiBoostSettings())
        bountiesGui = BountiesGui(this)

        registerCommands()
        registerListeners()
        registerPlaceholderApi()

        logger.info("HorusBounties enabled successfully.")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
    }

    private fun buildDatabaseConfig(): DatabaseConfig {
        val dbType = DatabaseType.fromConfig(config.getString("database.type", "SQLITE"))
        val rawName = config.getString("database.sqlite.file", "bounties.db") ?: "bounties.db"
        // Confine the SQLite file to the plugin folder (prevents path traversal via config)
        val sqliteFileName = File(rawName).name.ifBlank { "bounties.db" }
        return DatabaseConfig(
            type = dbType,
            sqliteFile = File(dataFolder, sqliteFileName),
            host = config.getString("database.mariadb.host", "localhost") ?: "localhost",
            port = config.getInt("database.mariadb.port", 3306),
            database = config.getString("database.mariadb.database", "horusbounties") ?: "horusbounties",
            username = config.getString("database.mariadb.username", "root") ?: "root",
            password = config.getString("database.mariadb.password", "") ?: "",
            poolSize = config.getInt("database.mariadb.pool-size", 10)
        )
    }

    private fun buildAntiBoostSettings(): AntiBoostSettings {
        return AntiBoostSettings(
            enabled = config.getBoolean("anti-boost.enabled", true),
            blockSameIp = config.getBoolean("anti-boost.block-same-ip", true),
            uuidCooldownSeconds = config.getLong("anti-boost.uuid-cooldown-seconds", 1800L),
            ipCooldownSeconds = config.getLong("anti-boost.ip-cooldown-seconds", 3600L)
        )
    }

    private fun registerCommands() {
        val bountyCommand = BountyCommand(this)
        getCommand("bounty")?.apply {
            setExecutor(bountyCommand)
            tabCompleter = bountyCommand
        }

        getCommand("bounties")?.setExecutor(BountiesCommand(this))

        val adminCommand = BountyAdminCommand(this)
        getCommand("bountyadmin")?.apply {
            setExecutor(adminCommand)
            tabCompleter = adminCommand
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(CombatListener(this), this)
        server.pluginManager.registerEvents(GuiListener(bountiesGui), this)
    }

    private fun registerPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            BountiesPlaceholderExpansion(this, bountyManager).register()
            logger.info("Registered PlaceholderAPI expansion: horusbounties")
        }
    }
}
