/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.plugin.devel.impldeps

import spock.lang.Ignore

class GradleImplDepsGenerationIntegrationTest extends BaseGradleImplDepsIntegrationTest {

    def "Gradle API is not generated if not declared by build"() {
        given:
        requireOwnGradleUserHomeDir()
        buildFile << applyJavaPlugin()

        when:
        succeeds 'build'

        then:
        assertNoGenerationOutput(output, API_JAR_GENERATION_OUTPUT_REGEX)
    }

    @Ignore("Requires swapping out the gradleApi() reference to the generated fat JAR")
    def "buildSrc project implicitly forces generation of Gradle API JAR"() {
        given:
        requireOwnGradleUserHomeDir()
        buildFile << applyJavaPlugin()
        temporaryFolder.createFile('buildSrc/src/main/groovy/MyPlugin.groovy') << customGroovyPlugin()

        when:
        succeeds 'build'

        then:
        assertSingleGenerationOutput(output, API_JAR_GENERATION_OUTPUT_REGEX)
    }

    def "Gradle API dependency resolves the expected JAR files"() {
        expect:
        buildFile << """
            configurations {
                deps
            }

            dependencies {
                deps fatGradleApi()
            }

            task resolveDependencyArtifacts {
                doLast {
                    def resolvedArtifacts = configurations.deps.incoming.files.files
                    assert resolvedArtifacts.size() == 3
                    assert resolvedArtifacts.find { (it.name =~ 'gradle-api-(.*)\\\\.jar').matches() }
                    assert resolvedArtifacts.find { (it.name =~ 'gradle-installation-beacon-(.*)\\\\.jar').matches() }
                    assert resolvedArtifacts.find { (it.name =~ 'groovy-all-(.*)\\\\.jar').matches() }
                }
            }
        """

        succeeds 'resolveDependencyArtifacts'
    }
}
