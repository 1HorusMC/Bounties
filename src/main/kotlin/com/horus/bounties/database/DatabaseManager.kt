package com.horus.bounties.database

import com.horus.bounties.model.BountyEntry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class DatabaseType {
    SQLITE,
    MARIADB;

    companion object {
        fun fromConfig(raw: String?): DatabaseType {
            return when (raw?.trim()?.uppercase()) {
                "MARIADB", "MYSQL" -> MARIADB
                else -> SQLITE
            }
        }
    }
}

data class DatabaseConfig(
    val type: DatabaseType,
    val sqliteFile: File,
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "horusbounties",
    val username: String = "root",
    val password: String = "",
    val poolSize: Int = 10
)

/**
 * Manages asynchronous database operations over SQLite or MariaDB via HikariCP.
 */
class DatabaseManager(private val config: DatabaseConfig) {

    private val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    private val dataSource: HikariDataSource

    init {
        val hikariConfig = HikariConfig().apply {
            poolName = "HorusBounties-Pool"
            connectionTimeout = 10_000L
            leakDetectionThreshold = 30_000L

            when (config.type) {
                DatabaseType.SQLITE -> {
                    config.sqliteFile.parentFile?.mkdirs()
                    jdbcUrl = "jdbc:sqlite:${config.sqliteFile.absolutePath}"
                    driverClassName = "org.sqlite.JDBC"
                    maximumPoolSize = 1
                    connectionInitSql = "PRAGMA journal_mode=WAL;"
                }
                DatabaseType.MARIADB -> {
                    jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.database}"
                    driverClassName = resolveMariaDbDriverClass()
                    username = config.username
                    password = config.password
                    maximumPoolSize = config.poolSize.coerceAtLeast(1)
                }
            }
        }

        dataSource = HikariDataSource(hikariConfig)
    }

    private fun resolveMariaDbDriverClass(): String {
        val relocated = "com.horus.bounties.libs.mariadb.Driver"
        return try {
            Class.forName(relocated)
            relocated
        } catch (_: ClassNotFoundException) {
            "org.mariadb.jdbc.Driver"
        }
    }

    fun initTables(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val sql = """
                CREATE TABLE IF NOT EXISTS horus_bounties (
                    target_uuid VARCHAR(36) PRIMARY KEY,
                    target_name VARCHAR(64) NOT NULL,
                    amount DOUBLE NOT NULL,
                    updated_at BIGINT NOT NULL
                );
            """.trimIndent()

            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(sql)
                }
            }
        }, executor)
    }

    fun loadAllBounties(): CompletableFuture<List<BountyEntry>> {
        return CompletableFuture.supplyAsync({
            val entries = mutableListOf<BountyEntry>()
            val sql = "SELECT target_uuid, target_name, amount, updated_at FROM horus_bounties"
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val uuid = UUID.fromString(rs.getString("target_uuid"))
                            val name = rs.getString("target_name")
                            val amount = rs.getDouble("amount")
                            val updatedAt = rs.getLong("updated_at")
                            entries.add(BountyEntry(uuid, name, amount, updatedAt))
                        }
                    }
                }
            }
            entries
        }, executor)
    }

    fun upsertBounty(entry: BountyEntry): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val sql = when (config.type) {
                DatabaseType.MARIADB -> """
                    INSERT INTO horus_bounties (target_uuid, target_name, amount, updated_at)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        target_name = VALUES(target_name),
                        amount = VALUES(amount),
                        updated_at = VALUES(updated_at)
                """.trimIndent()

                DatabaseType.SQLITE -> """
                    INSERT INTO horus_bounties (target_uuid, target_name, amount, updated_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(target_uuid) DO UPDATE SET
                        target_name = excluded.target_name,
                        amount = excluded.amount,
                        updated_at = excluded.updated_at
                """.trimIndent()
            }

            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, entry.targetUuid.toString())
                    stmt.setString(2, entry.targetName)
                    stmt.setDouble(3, entry.amount)
                    stmt.setLong(4, entry.updatedAt)
                    stmt.executeUpdate()
                }
            }
        }, executor)
    }

    fun deleteBounty(targetUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            val sql = "DELETE FROM horus_bounties WHERE target_uuid = ?"
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, targetUuid.toString())
                    stmt.executeUpdate() > 0
                }
            }
        }, executor)
    }

    fun close() {
        executor.shutdown()
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }
}
