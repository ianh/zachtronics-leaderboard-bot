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

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(projects.common)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinBard)
    implementation(libs.discord4j)
    implementation(libs.kaseChange)
    implementation(libs.kotlin.compiler)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinCompileTesting)
    testImplementation(libs.strikt)
    testImplementation(libs.kotlin.scriptingCompiler)
}

tasks.withType<Test> {
    useJUnitPlatform()
}