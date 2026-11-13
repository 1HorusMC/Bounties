package com.horus.bounties.model

import java.util.UUID

/**
 * Represents an active bounty entry stored in memory and persisted in the database.
 */
data class BountyEntry(
    val targetUuid: UUID,
    val targetName: String,
    val amount: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
