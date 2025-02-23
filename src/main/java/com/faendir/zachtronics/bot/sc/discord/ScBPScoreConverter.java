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

package com.faendir.zachtronics.bot.sc.discord;

import com.faendir.discord4j.command.parse.OptionConverter;
import com.faendir.discord4j.command.parse.SingleParseResult;
import com.faendir.zachtronics.bot.sc.model.ScScore;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class ScBPScoreConverter implements OptionConverter<String, ScScore> {
    @NotNull
    @Override
    public SingleParseResult<ScScore> fromValue(@NotNull ChatInputInteractionEvent context, @NotNull String value) {
        ScScore score = ScScore.parseBPScore(value);
        if (score == null)
            return new SingleParseResult.Failure<>("Invalid score: " + value);
        else
            return new SingleParseResult.Success<>(score);
    }
}
