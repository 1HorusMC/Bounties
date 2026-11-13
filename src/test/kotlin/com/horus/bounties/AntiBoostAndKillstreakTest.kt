package com.horus.bounties

import com.horus.bounties.manager.AntiBoostManager
import com.horus.bounties.manager.AntiBoostSettings
import com.horus.bounties.manager.BoostCheckResult
import com.horus.bounties.manager.KillstreakManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class AntiBoostAndKillstreakTest {

    @Test
    fun `blocks same IP kills`() {
        val manager = AntiBoostManager(
            AntiBoostSettings(
                enabled = true,
                blockSameIp = true,
                uuidCooldownSeconds = 1800,
                ipCooldownSeconds = 3600
            )
        )

        val result = manager.checkKill(
            killerUuid = UUID.randomUUID(),
            victimUuid = UUID.randomUUID(),
            killerIp = "192.168.1.10",
            victimIp = "192.168.1.10",
            nowMillis = 1_000_000L
        )
        assertEquals(BoostCheckResult.SAME_IP, result)
    }

    @Test
    fun `enforces UUID and IP pair cooldowns`() {
        val manager = AntiBoostManager(
            AntiBoostSettings(
                enabled = true,
                blockSameIp = true,
                uuidCooldownSeconds = 60,
                ipCooldownSeconds = 120
            )
        )

        val killer = UUID.randomUUID()
        val victim = UUID.randomUUID()
        val altKiller = UUID.randomUUID()
        val t0 = 10_000_000L

        assertEquals(
            BoostCheckResult.ALLOWED,
            manager.checkKill(killer, victim, "10.0.0.1", "10.0.0.2", t0)
        )
        manager.recordKill(killer, victim, "10.0.0.1", "10.0.0.2", t0)

        // Same UUID pair within 60s -> UUID_COOLDOWN
        assertEquals(
            BoostCheckResult.UUID_COOLDOWN,
            manager.checkKill(killer, victim, "10.0.0.1", "10.0.0.2", t0 + 30_000L)
        )

        // Different UUID pair on same IP pair within 120s -> IP_COOLDOWN
        assertEquals(
            BoostCheckResult.IP_COOLDOWN,
            manager.checkKill(altKiller, victim, "10.0.0.1", "10.0.0.2", t0 + 90_000L)
        )

        // After both cooldowns expire -> ALLOWED
        assertEquals(
            BoostCheckResult.ALLOWED,
            manager.checkKill(killer, victim, "10.0.0.1", "10.0.0.2", t0 + 121_000L)
        )
    }

    @Test
    fun `killstreak triggers auto bounty and resets on death`() {
        val ks = KillstreakManager()
        val killer = UUID.randomUUID()

        assertEquals(1, ks.incrementStreak(killer))
        assertFalse(ks.shouldTriggerAutoBounty(1, killsRequired = 3, repeatEveryStreak = true))

        assertEquals(2, ks.incrementStreak(killer))
        assertEquals(3, ks.incrementStreak(killer))
        assertTrue(ks.shouldTriggerAutoBounty(3, killsRequired = 3, repeatEveryStreak = true))

        assertEquals(4, ks.incrementStreak(killer))
        assertFalse(ks.shouldTriggerAutoBounty(4, killsRequired = 3, repeatEveryStreak = true))

        assertEquals(5, ks.incrementStreak(killer))
        assertEquals(6, ks.incrementStreak(killer))
        assertTrue(ks.shouldTriggerAutoBounty(6, killsRequired = 3, repeatEveryStreak = true))
        assertFalse(ks.shouldTriggerAutoBounty(6, killsRequired = 3, repeatEveryStreak = false))

        ks.resetStreak(killer)
        assertEquals(0, ks.getStreak(killer))
    }
}
