package com.horus.bounties.command

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Implements /bountyadmin clear <jugador> to remove active bounties.
 */
class BountyAdminCommand(private val plugin: HorusBountiesPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val config = plugin.config

        if (!sender.hasPermission("horusbounties.admin")) {
            MessageUtil.send(sender, config, "no-permission")
            return true
        }

        if (args.size < 2 || !args[0].equals("clear", ignoreCase = true)) {
            MessageUtil.send(sender, config, "usage-bountyadmin")
            return true
        }

        val targetInput = args[1]
        val cachedEntry = plugin.bountyManager.findByName(targetInput)
        val targetUuid = cachedEntry?.targetUuid
            ?: Bukkit.getPlayerExact(targetInput)?.uniqueId
            ?: Bukkit.getOfflinePlayerIfCached(targetInput)?.uniqueId

        if (targetUuid == null) {
            MessageUtil.send(
                sender,
                config,
                "no-bounty",
                Placeholder.unparsed("target", targetInput)
            )
            return true
        }

        val resolvedName = cachedEntry?.targetName ?: Bukkit.getOfflinePlayer(targetUuid).name ?: targetInput
        plugin.bountyManager.clearBounty(targetUuid)
            .thenAccept { removed ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (removed) {
                        MessageUtil.send(
                            sender,
                            config,
                            "bounty-cleared",
                            Placeholder.unparsed("target", resolvedName)
                        )
                    } else {
                        MessageUtil.send(
                            sender,
                            config,
                            "no-bounty",
                            Placeholder.unparsed("target", resolvedName)
                        )
                    }
                })
            }
            .exceptionally { ex ->
                plugin.logger.severe("Failed to clear bounty for $resolvedName: ${ex.message}")
                null
            }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("horusbounties.admin")) {
            return emptyList()
        }
        if (args.size == 1) {
            return listOf("clear").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        if (args.size == 2 && args[0].equals("clear", ignoreCase = true)) {
            return plugin.bountyManager.getTopBounties()
                .map { it.targetName }
                .filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }
}
