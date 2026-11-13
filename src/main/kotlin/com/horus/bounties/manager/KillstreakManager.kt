package com.horus.bounties.manager

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks consecutive player kills to award automatic killstreak bounties.
 */
class KillstreakManager {

    private val streaks = ConcurrentHashMap<UUID, Int>()

    fun incrementStreak(killerUuid: UUID): Int {
        return streaks.compute(killerUuid) { _, current -> (current ?: 0) + 1 } ?: 1
    }

    fun resetStreak(victimUuid: UUID) {
        streaks.remove(victimUuid)
    }

    fun getStreak(playerUuid: UUID): Int = streaks[playerUuid] ?: 0

    fun shouldTriggerAutoBounty(streak: Int, killsRequired: Int, repeatEveryStreak: Boolean): Boolean {
        if (killsRequired <= 0 || streak < killsRequired) {
            return false
        }
        return if (repeatEveryStreak) {
            streak % killsRequired == 0
        } else {
            streak == killsRequired
        }
    }
}
