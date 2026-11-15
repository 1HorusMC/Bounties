package com.horus.bounties.listener

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.manager.BoostCheckResult
import com.horus.bounties.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

/**
 * Handles bounty payouts, anti-boost validation, and automatic killstreak bounties on player death.
 */
class CombatListener(private val plugin: HorusBountiesPlugin) : Listener {

    @EventHandler
    fun onQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        plugin.killstreakManager.resetStreak(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        plugin.killstreakManager.resetStreak(victim.uniqueId)

        val killer = victim.killer ?: return
        if (killer.uniqueId == victim.uniqueId) {
            return
        }

        val config = plugin.config
        val killerIp = killer.address?.address?.hostAddress
        val victimIp = victim.address?.address?.hostAddress

        val checkResult = plugin.antiBoostManager.checkKill(
            killerUuid = killer.uniqueId,
            victimUuid = victim.uniqueId,
            killerIp = killerIp,
            victimIp = victimIp
        )

        if (checkResult != BoostCheckResult.ALLOWED) {
            MessageUtil.send(killer, config, "anti-boost-blocked")
            return
        }

        plugin.antiBoostManager.recordKill(
            killerUuid = killer.uniqueId,
            victimUuid = victim.uniqueId,
            killerIp = killerIp,
            victimIp = victimIp
        )

        val claimed = plugin.bountyManager.claimBounty(victim.uniqueId)
        if (claimed != null && claimed.amount > 0.0) {
            if (!plugin.vaultHook.deposit(killer, claimed.amount)) {
                // Payout failed: restore the bounty instead of destroying the funds
                plugin.logger.severe("Vault deposit failed for ${killer.name}; restoring bounty on ${victim.name}.")
                plugin.bountyManager.addBounty(victim.uniqueId, victim.name, claimed.amount)
                return
            }
            MessageUtil.broadcast(
                config,
                "bounty-claimed",
                Placeholder.unparsed("killer", killer.name),
                Placeholder.unparsed("victim", victim.name),
                Placeholder.unparsed("amount", plugin.bountyManager.formatAmount(claimed.amount))
            )
        }

        if (!config.getBoolean("killstreak.enabled", true)) {
            return
        }

        val streak = plugin.killstreakManager.incrementStreak(killer.uniqueId)
        val killsRequired = config.getInt("killstreak.kills-required", 5)
        val repeatEveryStreak = config.getBoolean("killstreak.repeat-every-streak", true)
        val rewardAmount = config.getDouble("killstreak.reward-amount", 500.0)

        if (rewardAmount > 0.0 && plugin.killstreakManager.shouldTriggerAutoBounty(streak, killsRequired, repeatEveryStreak)) {
            plugin.bountyManager.addBounty(killer.uniqueId, killer.name, rewardAmount)
                .thenAccept { updated ->
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        MessageUtil.broadcast(
                            config,
                            "bounty-killstreak",
                            Placeholder.unparsed("player", killer.name),
                            Placeholder.unparsed("streak", streak.toString()),
                            Placeholder.unparsed("amount", plugin.bountyManager.formatAmount(rewardAmount)),
                            Placeholder.unparsed("total", plugin.bountyManager.formatAmount(updated.amount))
                        )
                    })
                }
                .exceptionally { ex ->
                    plugin.logger.severe("Failed to persist automatic killstreak bounty for ${killer.name}: ${ex.message}")
                    null
                }
        }
    }
}
