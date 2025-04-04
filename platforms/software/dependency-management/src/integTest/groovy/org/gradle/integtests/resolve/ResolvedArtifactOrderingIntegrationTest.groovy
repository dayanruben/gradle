/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.integtests.resolve

import com.google.common.collect.Lists
import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.test.fixtures.maven.MavenModule

class ResolvedArtifactOrderingIntegrationTest extends AbstractHttpDependencyResolutionTest {

    def setup() {
        settingsFile << """
            rootProject.name = 'root'
            dependencyResolutionManagement {
                repositories {
                    maven {
                        url = "${mavenRepo.uri}"
                    }
                }
            }
        """
        buildFile << """
            $header
            configurations {
                unordered { extendsFrom common }
                consumerFirst { extendsFrom common }
                dependencyFirst { extendsFrom common }
            }
            dependencies {
                common "org.test:A:1.0"
            }
            configurations.consumerFirst.resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.CONSUMER_FIRST)
            configurations.dependencyFirst.resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        """
    }

    String getHeader() {
        """
            configurations {
                common
                create("default") { extendsFrom common }
            }
            artifacts {
                common file("\${project.name}.jar")
            }
        """
    }

    private void checkOrdered(List<?> ordered) {
        checkArtifacts("consumerFirst", ordered)
        checkArtifacts("dependencyFirst", Lists.reverse(ordered))
        checkArtifacts("unordered", ordered)
    }

    private void checkArtifacts(String name, List<?> modules) {
        def fileNames = toFileNames(modules).join(',')
        buildFile << """
            task check${name} {
                def conf = configurations.${name}
                def files = conf
                def incomingFiles = conf.files
                def artifactFiles = conf.incoming.artifactView { }.files
                def artifacts = conf.incoming.artifactView { }.artifacts
                def filteredArtifactFiles = conf.incoming.artifactView { componentFilter { true } }.files
                def filteredArtifacts = conf.incoming.artifactView { componentFilter { true } }.artifacts
                doLast {
                    assert files.collect { it.name } == [${fileNames}]
                    assert incomingFiles.collect { it.name } == [${fileNames}]
                    assert artifactFiles.collect { it.name } == [${fileNames}]
                    assert artifacts.collect { it.file.name } == [${fileNames}]
                    assert filteredArtifactFiles.collect { it.name } == [${fileNames}]
                    assert filteredArtifacts.collect { it.file.name } == [${fileNames}]
                    if (${!GradleContextualExecuter.configCache}) {
                        // Don't check eager methods when CC is enabled
                        assert configurations.${name}.resolve().collect { it.name } == [${fileNames}]
                        assert configurations.${name}.files.collect { it.name } == [${fileNames}]
                    }
                }
            }
        """

        assert succeeds("check${name}")
    }

    private List<String> toFileNames(List<?> modules) {
        modules.collect { it instanceof MavenModule ? "'${it.artifactFile.name}'" : "'${it}'" }
    }

    def "artifact collection has resolved artifact files and metadata 1"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modD).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modB).dependsOn(modC).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata 2"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modC).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modB).dependsOn(modD).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata 3"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modC).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modD).dependsOn(modB).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata 4"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modD).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modB).dependsOn(modD).dependsOn(modC).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata 5"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modD).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modB).dependsOn(modC).dependsOn(modD).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata 6"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modD).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modD).dependsOn(modB).dependsOn(modC).publish()

        then:
        checkOrdered([modA, modB, modC, modD])
    }

    def "artifact collection has resolved artifact files and metadata cycle"() {
        when:
        def modD = mavenRepo.module("org.test", "D")
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modC).publish()
        modD.dependsOn(modB).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modB).publish()

        then:
        checkOrdered([modA, modC, modD, modB])
    }

    def "project and external and file dependencies are ordered"() {
        when:
        def modD = mavenRepo.module("org.test", "D").publish()
        def modC = mavenRepo.module("org.test", "C").dependsOn(modD).publish()
        def modB = mavenRepo.module("org.test", "B").dependsOn(modC).publish()
        def modA = mavenRepo.module("org.test", "A").dependsOn(modC).publish()

        settingsFile << "include 'a', 'b', 'c'"
        buildFile << """
            dependencies {
                common files("root-lib.jar")
                common project(":a")
            }
        """

        file("a/build.gradle") << """
            $header
            dependencies {
                common files("a-lib.jar")
                common project(":b")
                common "org.test:B:1.0"
            }
        """

        file("b/build.gradle") << """
            $header
            dependencies {
                common project(":c")
                common files("b-lib.jar")
            }
        """

        file("c/build.gradle") << """
            $header
            dependencies {
                common "org.test:C:1.0"
                common files("c-lib.jar")
            }
        """


        then:
        checkOrdered(['root-lib.jar', modA, 'a.jar', 'a-lib.jar', modB, 'b.jar', 'b-lib.jar', 'c.jar', 'c-lib.jar', modC, modD])
    }
}
