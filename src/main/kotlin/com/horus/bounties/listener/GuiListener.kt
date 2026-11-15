package com.horus.bounties.listener

import com.horus.bounties.gui.BountiesGui
import com.horus.bounties.gui.BountiesInventoryHolder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/**
 * Handles navigation clicks inside the paginated bounties GUI.
 */
class GuiListener(private val bountiesGui: BountiesGui) : Listener {

    // Prevents dragging items into/out of the GUI (item insertion/duplication exploits).
    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is BountiesInventoryHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? BountiesInventoryHolder ?: return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == Material.AIR) return

        when (event.rawSlot) {
            BountiesGui.PREV_SLOT -> {
                if (clickedItem.type == Material.ARROW && holder.page > 0) {
                    bountiesGui.open(player, holder.page - 1)
                }
            }
            BountiesGui.CLOSE_SLOT -> {
                if (clickedItem.type == Material.BARRIER) {
                    player.closeInventory()
                }
            }
            BountiesGui.NEXT_SLOT -> {
                if (clickedItem.type == Material.ARROW) {
                    bountiesGui.open(player, holder.page + 1)
                }
            }
        }
    }
}
