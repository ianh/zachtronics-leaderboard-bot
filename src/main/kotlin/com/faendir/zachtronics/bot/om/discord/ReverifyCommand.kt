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

package com.faendir.zachtronics.bot.om.discord

import com.faendir.discord4j.command.annotation.ApplicationCommand
import com.faendir.discord4j.command.annotation.AutoComplete
import com.faendir.discord4j.command.annotation.Converter
import com.faendir.discord4j.command.parse.ApplicationCommandParser
import com.faendir.zachtronics.bot.discord.DiscordUser
import com.faendir.zachtronics.bot.discord.DiscordUserSecured
import com.faendir.zachtronics.bot.discord.command.AbstractSubCommand
import com.faendir.zachtronics.bot.discord.command.Secured
import com.faendir.zachtronics.bot.model.DisplayContext
import com.faendir.zachtronics.bot.om.JNISolutionVerifier
import com.faendir.zachtronics.bot.om.OmQualifier
import com.faendir.zachtronics.bot.om.getMetrics
import com.faendir.zachtronics.bot.om.model.OmPuzzle
import com.faendir.zachtronics.bot.om.model.OmRecord
import com.faendir.zachtronics.bot.om.model.OmScore
import com.faendir.zachtronics.bot.om.model.OmScorePart
import com.faendir.zachtronics.bot.om.model.OmType
import com.faendir.zachtronics.bot.om.repository.OmSolutionRepository
import com.faendir.zachtronics.bot.utils.SafeEmbedMessageBuilder
import com.faendir.zachtronics.bot.utils.orEmpty
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import discord4j.discordjson.json.ApplicationCommandOptionData
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@OmQualifier
class ReverifyCommand(private val repository: OmSolutionRepository) : AbstractSubCommand<Reverify>(),
    Secured by DiscordUserSecured(DiscordUser.BOT_OWNERS),
    ApplicationCommandParser<Reverify, ApplicationCommandOptionData> by ReverifyParser {

    override fun handle(event: DeferrableInteractionEvent, parameters: Reverify): Mono<Void> {
        val puzzles = (parameters.puzzle?.let { listOf(it) } ?: OmPuzzle.values().toList()).filter {
            parameters.type == null || parameters.type == it.type
        }
        val parts = parameters.part?.let { listOf(it) } ?: OmScorePart.values().toList()
        val overrideExisting = parameters.overrideExisting == true
        val overrideRecords = mutableListOf<Pair<OmRecord, OmScore>>()
        val errors = mutableListOf<String>()
        for (puzzle in puzzles) {
            if (puzzle.file == null) {
                errors.add("No puzzle file for ${puzzle.displayName}")
                continue
            }

            val records = repository.findCategoryHolders(puzzle, true)
            for ((record, _) in records) {
                if (record.dataPath == null) {
                    errors.add("No solution file for ${record.toDisplayString(DisplayContext.discord())}")
                    continue
                }
                if (overrideExisting || parts.any { it.getValue(record.score) == null }) {
                    val computed = JNISolutionVerifier.open(puzzle.file, record.dataPath.toFile())
                        .use { it.getMetrics(parts) { e, part -> errors.add("Failed to get ${part.displayName} for ${record.toDisplayString(DisplayContext.discord())}: ${e.message}") } }
                    if (computed.size < parts.size) {
                        errors.add(
                            "Wanted ${parts.joinToString { it.displayName }}, but got ${computed.keys.joinToString { it.displayName }} for ${
                                record.toDisplayString(
                                    DisplayContext.discord()
                                )
                            }"
                        )
                    }
                    val newScore = if (overrideExisting) {
                        OmScore(
                            cost = computed[OmScorePart.COST]?.toInt() ?: record.score.cost,
                            cycles = computed[OmScorePart.CYCLES]?.toInt() ?: record.score.cycles,
                            area = computed[OmScorePart.AREA]?.toInt() ?: record.score.area,
                            instructions = computed[OmScorePart.INSTRUCTIONS]?.toInt() ?: record.score.instructions,
                            height = computed[OmScorePart.HEIGHT]?.toInt() ?: record.score.height,
                            width = computed[OmScorePart.WIDTH]?.toDouble() ?: record.score.width,
                            rate = computed[OmScorePart.RATE]?.toDouble() ?: record.score.rate,
                            trackless = record.score.trackless,
                            overlap = record.score.overlap,
                        )
                    } else {
                        OmScore(
                            cost = record.score.cost ?: computed[OmScorePart.COST]?.toInt(),
                            cycles = record.score.cycles ?: computed[OmScorePart.CYCLES]?.toInt(),
                            area = record.score.area ?: computed[OmScorePart.AREA]?.toInt(),
                            instructions = record.score.instructions ?: computed[OmScorePart.INSTRUCTIONS]?.toInt(),
                            height = record.score.height ?: computed[OmScorePart.HEIGHT]?.toInt(),
                            width = record.score.width ?: computed[OmScorePart.WIDTH]?.toDouble(),
                            rate = record.score.rate ?: computed[OmScorePart.RATE]?.toDouble(),
                            trackless = record.score.trackless,
                            overlap = record.score.overlap,
                        )
                    }
                    if (newScore != record.score) {
                        overrideRecords.add(record to newScore)
                    }
                }
            }
        }
        if (overrideRecords.isNotEmpty()) {
            repository.overrideScores(overrideRecords)
        }
        return SafeEmbedMessageBuilder()
            .title(
                "Reverify" +
                        parameters.type?.displayName?.orEmpty(" ") +
                        parameters.puzzle?.displayName?.orEmpty(" ") +
                        parameters.part?.displayName?.orEmpty(" ") +
                        if (parameters.overrideExisting == true) " overriding existing values!" else ""
            )
            .description("**Modified Records:** ${overrideRecords.size}\n\n**Errors:**\n${errors.joinToString("\n")}")
            .send(event)
    }

    companion object {
        private val BOT_OWNERS = setOf(
            295868901042946048L,  // 12345ieee
            288766560938622976L, // F43nd1r
        )
    }
}

@ApplicationCommand(name = "reverify", description = "Recompute metrics based on filters", subCommand = true)
data class Reverify(
    val type: OmType?,
    @Converter(PuzzleConverter::class)
    @AutoComplete(PuzzleAutoCompletionProvider::class)
    val puzzle: OmPuzzle?,
    val part: OmScorePart?,
    val overrideExisting: Boolean?,
)