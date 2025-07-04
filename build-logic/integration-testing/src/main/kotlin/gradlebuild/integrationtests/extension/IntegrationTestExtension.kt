/*
 * Copyright 2020 the original author or authors.
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

package gradlebuild.integrationtests.extension

import org.gradle.api.provider.Property


abstract class IntegrationTestExtension {
    /**
     * If enabled, there will be a `GenerateAutoTestedSamplesTestTask` task
     * that generates a subclass of `AbstractAutoTestedSamplesTest` to
     * test all snippets embedded in javadoc with `class='autoTested'`.
     */
    abstract val generateDefaultAutoTestedSamplesTest: Property<Boolean>
    abstract val testJvmXmx: Property<String>
}
