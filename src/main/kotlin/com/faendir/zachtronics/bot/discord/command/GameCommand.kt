/*
 * Copyright (c) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.faendir.zachtronics.bot.discord.command

import com.faendir.discord4j.command.parse.CombinedParseResult
import com.faendir.zachtronics.bot.utils.user
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import discord4j.discordjson.json.ApplicationCommandRequest
import reactor.core.publisher.Mono

private const val COMMAND_KEY = "command"

interface GameCommand : TopLevelCommand<GameCommand.SubCommandWithParameters<*>> {
    val displayName: String

    val subCommands: List<SubCommand<*>>

    override fun buildData(): ApplicationCommandRequest {
        val request = ApplicationCommandRequest.builder()
            .name(commandName)
            .description(displayName)
        for (command in subCommands) {
            request.addOption(command.data)
        }
        return request.build()
    }

    override fun autoComplete(event: ChatInputAutoCompleteEvent): List<ApplicationCommandOptionChoiceData>? {
        val option = event.options.first()
        val subCommand = subCommands.find { it.data.name() == option.name } ?: return null
        return subCommand.autoComplete(event)
    }

    override fun map(parameters: Map<String, Any?>): SubCommandWithParameters<*>? {
        val command = subCommands.first { parameters.getValue(COMMAND_KEY) == it.data.name() }
        return command.mapImpl(parameters - COMMAND_KEY)
    }

    fun <T> SubCommand<T>.mapImpl(parameters: Map<String, Any?>): SubCommandWithParameters<T>? {
        val param = map(parameters - COMMAND_KEY)
        return param?.let { SubCommandWithParameters(this, param) }
    }

    override fun parse(event: ChatInputInteractionEvent): CombinedParseResult<SubCommandWithParameters<*>> {
        val option = event.options.first()
        val subCommand = subCommands.find { it.data.name() == option.name }
            ?: return CombinedParseResult.Failure(listOf("I did not recognize the command \"${option.name}\"."))
        if (subCommand.secured?.hasExecutionPermission(event.user()) == false) {
            return CombinedParseResult.Failure(listOf("sorry, you do not have the permission to use this command."))
        }
        return parseSubCommand(subCommand, event)
    }

    fun <T> parseSubCommand(
        subCommand: SubCommand<T>,
        event: ChatInputInteractionEvent
    ): CombinedParseResult<SubCommandWithParameters<*>> {
        return when (val result = subCommand.parse(event)) {
            is CombinedParseResult.Failure -> CombinedParseResult.Failure(result.messages)
            is CombinedParseResult.Ambiguous -> CombinedParseResult.Ambiguous(
                result.options,
                result.partialResult + (COMMAND_KEY to subCommand.data.name())
            )
            is CombinedParseResult.Success -> CombinedParseResult.Success(SubCommandWithParameters(subCommand, result.value))
        }
    }

    override fun handle(event: DeferrableInteractionEvent, parameters: SubCommandWithParameters<*>): Mono<Void> = parameters.handle(event)

    data class SubCommandWithParameters<T>(val subCommand: SubCommand<T>, val parameters: T) {
        fun handle(event: DeferrableInteractionEvent) = subCommand.handle(event, parameters)
    }
}