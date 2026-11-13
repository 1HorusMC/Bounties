package com.horus.bounties.gui

import com.horus.bounties.HorusBountiesPlugin
import com.horus.bounties.util.MessageUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class BountiesInventoryHolder(val page: Int) : InventoryHolder {
    private lateinit var inv: Inventory

    fun bind(inventory: Inventory) {
        this.inv = inventory
    }

    override fun getInventory(): Inventory = inv
}

/**
 * Renders the paginated GUI listing active bounties sorted from highest to lowest.
 */
class BountiesGui(private val plugin: HorusBountiesPlugin) {

    companion object {
        const val PAGE_SIZE = 45
        const val INVENTORY_SIZE = 54
        const val PREV_SLOT = 45
        const val CLOSE_SLOT = 49
        const val NEXT_SLOT = 53
    }

    fun open(player: Player, requestedPage: Int = 0) {
        val bounties = plugin.bountyManager.getTopBounties()
        val maxPage = maxOf(1, (bounties.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val page = requestedPage.coerceIn(0, maxPage - 1)

        val config = plugin.config
        val titleTemplate = config.getString(
            "gui.title",
            "<gradient:#ffaa00:#ff5555><bold>Recompensas</bold></gradient> <dark_gray>(<gray>Página <page>/<max_page></gray>)"
        ) ?: "Recompensas"

        val title = MessageUtil.parse(
            titleTemplate,
            Placeholder.unparsed("page", (page + 1).toString()),
            Placeholder.unparsed("max_page", maxPage.toString())
        )

        val holder = BountiesInventoryHolder(page)
        val inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title)
        holder.bind(inventory)

        val startIndex = page * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, bounties.size)

        val headNameTemplate = config.getString("gui.head-name", "<gold><bold><player></bold></gold>")
            ?: "<gold><bold><player></bold></gold>"
        val headLoreTemplates = config.getStringList("gui.head-lore")

        for ((slot, index) in (startIndex until endIndex).withIndex()) {
            val entry = bounties[index]
            val formattedAmount = plugin.bountyManager.formatAmount(entry.amount)

            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as? SkullMeta
            if (meta != null) {
                meta.owningPlayer = Bukkit.getOfflinePlayer(entry.targetUuid)
                meta.displayName(
                    MessageUtil.parseItemText(
                        headNameTemplate,
                        Placeholder.unparsed("player", entry.targetName),
                        Placeholder.unparsed("amount", formattedAmount)
                    )
                )
                if (headLoreTemplates.isNotEmpty()) {
                    meta.lore(
                        headLoreTemplates.map { line ->
                            MessageUtil.parseItemText(
                                line,
                                Placeholder.unparsed("player", entry.targetName),
                                Placeholder.unparsed("amount", formattedAmount)
                            )
                        }
                    )
                }
                head.itemMeta = meta
            }
            inventory.setItem(slot, head)
        }

        val filler = createNavItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "))
        for (slot in PAGE_SIZE until INVENTORY_SIZE) {
            inventory.setItem(slot, filler)
        }

        if (page > 0) {
            val prevName = config.getString("gui.previous-page-name", "<yellow>« Página Anterior</yellow>")
                ?: "<yellow>« Página Anterior</yellow>"
            inventory.setItem(PREV_SLOT, createNavItem(Material.ARROW, MessageUtil.parseItemText(prevName)))
        }

        val closeName = config.getString("gui.close-name", "<red>Cerrar</red>") ?: "<red>Cerrar</red>"
        inventory.setItem(CLOSE_SLOT, createNavItem(Material.BARRIER, MessageUtil.parseItemText(closeName)))

        if (page + 1 < maxPage) {
            val nextName = config.getString("gui.next-page-name", "<yellow>Página Siguiente »</yellow>")
                ?: "<yellow>Página Siguiente »</yellow>"
            inventory.setItem(NEXT_SLOT, createNavItem(Material.ARROW, MessageUtil.parseItemText(nextName)))
        }

        player.openInventory(inventory)
    }

    private fun createNavItem(material: Material, name: Component): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) {
            meta.displayName(name)
            item.itemMeta = meta
        }
        return item
    }
}
