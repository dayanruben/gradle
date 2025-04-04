/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("gradlebuild.internal.java")
    id("application")
}

description = "Example client application using the build cache library"

dependencies {
    implementation(projects.buildCache)
    implementation(projects.buildCacheBase)
    implementation(projects.buildCacheLocal)
    implementation(projects.buildCachePackaging)
    implementation(projects.buildCacheSpi)
    implementation(projects.buildOperations)
    implementation(projects.concurrent)
    implementation(projects.fileTemp)
    implementation(projects.files)
    implementation(projects.functional)
    implementation(projects.hashing)
    implementation(projects.stdlibJavaExtensions)
    implementation(projects.persistentCache)
    implementation(projects.snapshots)
    implementation(projects.time)

    implementation(libs.commonsIo)
    implementation(libs.guava)
    implementation(libs.guice)
    implementation(libs.jspecify)
    implementation(libs.slf4jApi)
}

application {
    mainClass = "org.gradle.caching.example.ExampleBuildCacheClient"
}
