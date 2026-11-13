package com.horus.bounties

import com.horus.bounties.database.DatabaseConfig
import com.horus.bounties.database.DatabaseManager
import com.horus.bounties.database.DatabaseType
import com.horus.bounties.manager.BountyManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

class DatabaseAndBountyManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var databaseManager: DatabaseManager
    private lateinit var bountyManager: BountyManager

    @BeforeEach
    fun setUp() {
        val dbFile = File(tempDir.toFile(), "test-bounties.db")
        databaseManager = DatabaseManager(
            DatabaseConfig(
                type = DatabaseType.SQLITE,
                sqliteFile = dbFile
            )
        )
        bountyManager = BountyManager(databaseManager)
        bountyManager.loadFromDatabase().join()
    }

    @AfterEach
    fun tearDown() {
        databaseManager.close()
    }

    @Test
    fun `adds accumulates sorts and persists bounties`() {
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()
        val charlie = UUID.randomUUID()

        bountyManager.addBounty(alice, "Alice", 250.0).join()
        bountyManager.addBounty(alice, "Alice", 150.0).join()
        bountyManager.addBounty(bob, "Bob", 900.0).join()
        bountyManager.addBounty(charlie, "Charlie", 100.0).join()

        assertEquals(400.0, bountyManager.getBountyAmount(alice))
        val top = bountyManager.getTopBounties()
        assertEquals(3, top.size)
        assertEquals("Bob", top[0].targetName)
        assertEquals(900.0, top[0].amount)
        assertEquals("Alice", top[1].targetName)
        assertEquals("Charlie", top[2].targetName)

        // Reload from SQLite into a fresh BountyManager to verify persistence
        val reloaded = BountyManager(databaseManager)
        reloaded.loadFromDatabase().join()
        assertEquals(400.0, reloaded.getBountyAmount(alice))
        assertEquals(900.0, reloaded.getBountyAmount(bob))
        assertEquals(100.0, reloaded.getBountyAmount(charlie))
    }

    @Test
    fun `claimBounty and clearBounty remove from cache and database`() {
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()

        bountyManager.addBounty(alice, "Alice", 500.0).join()
        bountyManager.addBounty(bob, "Bob", 300.0).join()

        val claimed = bountyManager.claimBounty(alice)
        assertNotNull(claimed)
        assertEquals(500.0, claimed?.amount)
        assertNull(bountyManager.getBounty(alice))

        val cleared = bountyManager.clearBounty(bob).join()
        assertTrue(cleared)
        assertNull(bountyManager.getBounty(bob))

        val clearMissing = bountyManager.clearBounty(UUID.randomUUID()).join()
        assertFalse(clearMissing)

        // Wait briefly for async claim deletion and verify fresh load is empty
        Thread.sleep(100)
        val reloaded = BountyManager(databaseManager)
        reloaded.loadFromDatabase().join()
        assertTrue(reloaded.getTopBounties().isEmpty())
    }
}
