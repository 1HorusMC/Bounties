package com.horus.bounties.manager

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class BoostCheckResult {
    ALLOWED,
    SAME_IP,
    UUID_COOLDOWN,
    IP_COOLDOWN
}

data class AntiBoostSettings(
    val enabled: Boolean,
    val blockSameIp: Boolean,
    val uuidCooldownSeconds: Long,
    val ipCooldownSeconds: Long
)

/**
 * Prevents bounty and killstreak farming by enforcing same-IP blocks and per-pair cooldowns.
 */
class AntiBoostManager(private var settings: AntiBoostSettings) {

    private val uuidPairKills = ConcurrentHashMap<String, Long>()
    private val ipPairKills = ConcurrentHashMap<String, Long>()

    fun updateSettings(newSettings: AntiBoostSettings) {
        this.settings = newSettings
    }

    fun checkKill(
        killerUuid: UUID,
        victimUuid: UUID,
        killerIp: String?,
        victimIp: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): BoostCheckResult {
        if (!settings.enabled) {
            return BoostCheckResult.ALLOWED
        }

        if (settings.blockSameIp && !killerIp.isNullOrBlank() && killerIp == victimIp) {
            return BoostCheckResult.SAME_IP
        }

        if (settings.uuidCooldownSeconds > 0) {
            val uuidKey = "$killerUuid:$victimUuid"
            val lastKill = uuidPairKills[uuidKey]
            if (lastKill != null && (nowMillis - lastKill) < settings.uuidCooldownSeconds * 1000L) {
                return BoostCheckResult.UUID_COOLDOWN
            }
        }

        if (settings.ipCooldownSeconds > 0 && !killerIp.isNullOrBlank() && !victimIp.isNullOrBlank()) {
            val ipKey = "$killerIp:$victimIp"
            val lastIpKill = ipPairKills[ipKey]
            if (lastIpKill != null && (nowMillis - lastIpKill) < settings.ipCooldownSeconds * 1000L) {
                return BoostCheckResult.IP_COOLDOWN
            }
        }

        return BoostCheckResult.ALLOWED
    }

    fun recordKill(
        killerUuid: UUID,
        victimUuid: UUID,
        killerIp: String?,
        victimIp: String?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!settings.enabled) {
            return
        }

        if (settings.uuidCooldownSeconds > 0) {
            uuidPairKills["$killerUuid:$victimUuid"] = nowMillis
        }

        if (settings.ipCooldownSeconds > 0 && !killerIp.isNullOrBlank() && !victimIp.isNullOrBlank()) {
            ipPairKills["$killerIp:$victimIp"] = nowMillis
        }

        prune(nowMillis)
    }

    /** Drops expired entries so the maps cannot grow without bound (memory exhaustion). */
    private fun prune(nowMillis: Long) {
        val uuidTtl = settings.uuidCooldownSeconds * 1000L
        val ipTtl = settings.ipCooldownSeconds * 1000L
        uuidPairKills.entries.removeIf { nowMillis - it.value >= uuidTtl }
        ipPairKills.entries.removeIf { nowMillis - it.value >= ipTtl }
    }
}
