package com.horus.bounties.command

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.util.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Implements /bounties to open the paginated bounty GUI.
 */
class BountiesCommand(private val plugin: HorusBountiesPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val config = plugin.config

        if (sender !is Player) {
            MessageUtil.send(sender, config, "player-only")
            return true
        }

        if (!sender.hasPermission("horusbounties.gui")) {
            MessageUtil.send(sender, config, "no-permission")
            return true
        }

        plugin.bountiesGui.open(sender, 0)
        return true
    }
}
