package com.horus.bounties.manager

import com.horus.bounties.database.DatabaseManager
import com.horus.bounties.model.BountyEntry
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache and persistence coordinator for active player bounties.
 */
class BountyManager(private val databaseManager: DatabaseManager) {

    private val cache = ConcurrentHashMap<UUID, BountyEntry>()
    private val amountFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun loadFromDatabase(): CompletableFuture<Void> {
        return databaseManager.initTables().thenCompose {
            databaseManager.loadAllBounties().thenAccept { entries ->
                cache.clear()
                for (entry in entries) {
                    if (entry.amount > 0.0) {
                        cache[entry.targetUuid] = entry
                    }
                }
            }
        }
    }

    fun getBounty(targetUuid: UUID): BountyEntry? = cache[targetUuid]

    fun getBountyAmount(targetUuid: UUID): Double = cache[targetUuid]?.amount ?: 0.0

    fun getTopBounties(): List<BountyEntry> {
        return cache.values.sortedWith(
            compareByDescending<BountyEntry> { it.amount }
                .thenBy { it.updatedAt }
        )
    }

    fun addBounty(targetUuid: UUID, targetName: String, amountToAdd: Double): CompletableFuture<BountyEntry> {
        val now = System.currentTimeMillis()
        val updated = cache.compute(targetUuid) { _, existing ->
            val newAmount = (existing?.amount ?: 0.0) + amountToAdd
            BountyEntry(targetUuid, targetName, newAmount, now)
        }!!

        return databaseManager.upsertBounty(updated).thenApply { updated }
    }

    fun claimBounty(targetUuid: UUID): BountyEntry? {
        val removed = cache.remove(targetUuid) ?: return null
        databaseManager.deleteBounty(targetUuid)
        return removed
    }

    fun clearBounty(targetUuid: UUID): CompletableFuture<Boolean> {
        val removed = cache.remove(targetUuid)
        return databaseManager.deleteBounty(targetUuid).thenApply { dbDeleted ->
            removed != null || dbDeleted
        }
    }

    fun findByName(name: String): BountyEntry? {
        return cache.values.firstOrNull { it.targetName.equals(name, ignoreCase = true) }
    }

    fun formatAmount(amount: Double): String = amountFormat.format(amount)
}
