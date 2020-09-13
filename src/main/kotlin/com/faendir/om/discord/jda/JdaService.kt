package com.faendir.om.discord.jda

import com.faendir.om.discord.commands.Command
import com.faendir.om.discord.config.JdaProperties
import com.faendir.om.discord.leaderboards.Leaderboard
import com.faendir.om.discord.model.Puzzle
import com.faendir.om.discord.model.Score
import com.faendir.om.discord.utils.GameLeaderboardsPair
import com.faendir.om.discord.utils.groupByGame
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy

@Service
class JdaService(jdaProperties: JdaProperties, private val commands: List<Command>, leaderboards: List<Leaderboard<*, *, *>>) : ListenerAdapter() {
    @Suppress("LeakingThis")
    private val jda: JDA = JDABuilder.createLight(jdaProperties.token, GatewayIntent.GUILD_MESSAGES).addEventListeners(this).build().awaitReady()
    private val games = leaderboards.groupByGame()

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.author.isBot) {
            val game = games.find { it.game.discordChannel == event.channel.name } ?: return
            val message = event.message.contentRaw
            handleMessage(message, event, game)
        }
    }

    private fun <S : Score<S, *>, P : Puzzle> handleMessage(message: String, event: GuildMessageReceivedEvent, game: GameLeaderboardsPair<S, P>) {
        commands.forEach { command ->
            if (message.startsWith("!${command.name}")) {
                event.channel.sendMessage("${event.author.asMention} ${
                    if (command.requiresRoles.isEmpty() || event.member?.roles?.map { it.name }?.containsAll(command.requiresRoles) == true) {
                        command.regex.find(message)?.let { result ->
                            command.handleMessage(game.game, game.leaderboards, event.author, event.channel, event.message, result)
                        } ?: "sorry, could not parse your command. Type `!help` to see the syntax."
                    } else {
                        "sorry, you do not have all required roles for this command ${command.requiresRoles.joinToString(separator = "`, `", prefix = "(`", postfix = "`)")}."
                    }
                }").mention(event.author).queue()
            }
        }
    }

    @PreDestroy
    fun stop() {
        jda.shutdown()
    }
}


