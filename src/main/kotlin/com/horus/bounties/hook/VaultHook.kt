package com.horus.bounties.hook

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

/**
 * Integrates with Vault Economy for withdrawing and depositing bounty funds.
 */
class VaultHook {

    private var economy: Economy? = null

    fun setupEconomy(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false
        }
        val registration = Bukkit.getServicesManager().getRegistration(Economy::class.java) ?: return false
        economy = registration.provider
        return economy != null
    }

    fun isReady(): Boolean = economy != null

    fun hasEnough(player: OfflinePlayer, amount: Double): Boolean {
        val eco = economy ?: return false
        return eco.has(player, amount)
    }

    fun withdraw(player: OfflinePlayer, amount: Double): Boolean {
        val eco = economy ?: return false
        return eco.withdrawPlayer(player, amount).transactionSuccess()
    }

    fun deposit(player: OfflinePlayer, amount: Double): Boolean {
        val eco = economy ?: return false
        return eco.depositPlayer(player, amount).transactionSuccess()
    }

    fun format(amount: Double): String {
        return economy?.format(amount) ?: amount.toString()
    }
}
