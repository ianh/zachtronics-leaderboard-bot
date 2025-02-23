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

package com.faendir.zachtronics.bot.om;


import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JNISolutionVerifierTest {

    @Test
    void getHeight() {
        try(JNISolutionVerifier verifier = JNISolutionVerifier.open(
                new File(getClass().getClassLoader().getResource("P009.puzzle").getFile()),
                new File(getClass().getClassLoader().getResource("Face_Powder_Height_1.solution").getFile()))) {
            int height = verifier.getMetric(JNISolutionVerifier.Metrics.HEIGHT);
            assertEquals(1, height);
        }
    }
}