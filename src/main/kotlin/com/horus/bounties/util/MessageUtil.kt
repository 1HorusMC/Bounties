package com.horus.bounties.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration

/**
 * Utility for parsing and dispatching MiniMessage-formatted components.
 */
object MessageUtil {

    private val miniMessage = MiniMessage.miniMessage()

    fun parse(template: String, vararg resolvers: TagResolver): Component {
        return miniMessage.deserialize(template, *resolvers)
    }

    fun parseItemText(template: String, vararg resolvers: TagResolver): Component {
        return miniMessage.deserialize(template, *resolvers)
            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    fun formatMessage(
        config: FileConfiguration,
        key: String,
        vararg resolvers: TagResolver
    ): Component {
        val prefixRaw = config.getString("messages.prefix", "") ?: ""
        val messageRaw = config.getString("messages.$key", "<red>Mensaje faltante: $key</red>")
            ?: "<red>Mensaje faltante: $key</red>"

        val combinedResolvers = mutableListOf<TagResolver>(
            Placeholder.parsed("prefix", prefixRaw)
        )
        combinedResolvers.addAll(resolvers)

        return miniMessage.deserialize(messageRaw, *combinedResolvers.toTypedArray())
    }

    fun send(
        sender: CommandSender,
        config: FileConfiguration,
        key: String,
        vararg resolvers: TagResolver
    ) {
        sender.sendMessage(formatMessage(config, key, *resolvers))
    }

    fun broadcast(
        config: FileConfiguration,
        key: String,
        vararg resolvers: TagResolver
    ) {
        val component = formatMessage(config, key, *resolvers)
        Bukkit.getServer().broadcast(component)
    }
}
