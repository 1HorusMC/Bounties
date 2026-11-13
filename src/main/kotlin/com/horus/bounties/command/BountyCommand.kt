package com.horus.bounties.command

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Implements /bounty <jugador> <cantidad> using Vault economy.
 */
class BountyCommand(private val plugin: HorusBountiesPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val config = plugin.config

        if (sender !is Player) {
            MessageUtil.send(sender, config, "player-only")
            return true
        }

        if (!sender.hasPermission("horusbounties.use")) {
            MessageUtil.send(sender, config, "no-permission")
            return true
        }

        if (args.size < 2) {
            MessageUtil.send(sender, config, "usage-bounty")
            return true
        }

        val targetInput = args[0]
        val target = resolveTarget(targetInput)
        if (target == null || (!target.isOnline && !target.hasPlayedBefore())) {
            MessageUtil.send(
                sender,
                config,
                "player-not-found",
                Placeholder.unparsed("target", targetInput)
            )
            return true
        }

        val allowSelf = config.getBoolean("bounty.allow-self-bounty", false)
        if (!allowSelf && target.uniqueId == sender.uniqueId) {
            MessageUtil.send(sender, config, "self-bounty")
            return true
        }

        val amount = args[1].toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            MessageUtil.send(sender, config, "invalid-amount")
            return true
        }

        val minAmount = config.getDouble("bounty.min-amount", 100.0)
        if (amount < minAmount) {
            MessageUtil.send(
                sender,
                config,
                "amount-too-low",
                Placeholder.unparsed("min", plugin.bountyManager.formatAmount(minAmount))
            )
            return true
        }

        val maxAmount = config.getDouble("bounty.max-amount", 1_000_000.0)
        if (amount > maxAmount) {
            MessageUtil.send(
                sender,
                config,
                "amount-too-high",
                Placeholder.unparsed("max", plugin.bountyManager.formatAmount(maxAmount))
            )
            return true
        }

        if (!plugin.vaultHook.hasEnough(sender, amount)) {
            MessageUtil.send(
                sender,
                config,
                "not-enough-money",
                Placeholder.unparsed("amount", plugin.bountyManager.formatAmount(amount))
            )
            return true
        }

        if (!plugin.vaultHook.withdraw(sender, amount)) {
            MessageUtil.send(
                sender,
                config,
                "not-enough-money",
                Placeholder.unparsed("amount", plugin.bountyManager.formatAmount(amount))
            )
            return true
        }

        val targetName = target.name ?: targetInput
        plugin.bountyManager.addBounty(target.uniqueId, targetName, amount)
            .thenAccept { updated ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    MessageUtil.broadcast(
                        config,
                        "bounty-placed",
                        Placeholder.unparsed("sender", sender.name),
                        Placeholder.unparsed("target", updated.targetName),
                        Placeholder.unparsed("amount", plugin.bountyManager.formatAmount(amount)),
                        Placeholder.unparsed("total", plugin.bountyManager.formatAmount(updated.amount))
                    )
                })
            }
            .exceptionally { ex ->
                plugin.logger.severe("Failed to persist bounty for $targetName: ${ex.message}")
                null
            }

        return true
    }

    private fun resolveTarget(name: String): OfflinePlayer? {
        return Bukkit.getPlayerExact(name) ?: Bukkit.getOfflinePlayerIfCached(name)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[0], ignoreCase = true) }
        }
        if (args.size == 2) {
            return listOf("100", "500", "1000", "5000")
                .filter { it.startsWith(args[1]) }
        }
        return emptyList()
    }
}
