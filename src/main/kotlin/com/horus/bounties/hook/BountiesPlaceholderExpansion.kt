package com.horus.bounties.hook

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.manager.BountyManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

/**
 * PlaceholderAPI expansion exposing player bounties and top bounty rankings.
 */
class BountiesPlaceholderExpansion(
    private val plugin: HorusBountiesPlugin,
    private val bountyManager: BountyManager
) : PlaceholderExpansion() {

    private val topNameRegex = Regex("^top_(\\d+)_name$", RegexOption.IGNORE_CASE)
    private val topAmountRegex = Regex("^top_(\\d+)_amount$", RegexOption.IGNORE_CASE)

    override fun getIdentifier(): String = "horusbounties"

    override fun getAuthor(): String = plugin.pluginMeta.authors.joinToString(", ").ifBlank { "Horus" }

    override fun getVersion(): String = plugin.pluginMeta.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (params.equals("amount", ignoreCase = true)) {
            val amount = player?.uniqueId?.let { bountyManager.getBountyAmount(it) } ?: 0.0
            return bountyManager.formatAmount(amount)
        }

        topNameRegex.matchEntire(params)?.let { match ->
            val rank = match.groupValues[1].toIntOrNull() ?: return "Ninguno"
            if (rank <= 0) return "Ninguno"
            val topList = bountyManager.getTopBounties()
            return topList.getOrNull(rank - 1)?.targetName ?: "Ninguno"
        }

        topAmountRegex.matchEntire(params)?.let { match ->
            val rank = match.groupValues[1].toIntOrNull() ?: return "0"
            if (rank <= 0) return "0"
            val topList = bountyManager.getTopBounties()
            val entry = topList.getOrNull(rank - 1) ?: return "0"
            return bountyManager.formatAmount(entry.amount)
        }

        return null
    }
}
