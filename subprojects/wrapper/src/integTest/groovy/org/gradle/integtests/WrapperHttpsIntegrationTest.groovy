/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.integtests

import org.gradle.api.JavaVersion
import org.gradle.integtests.fixtures.TestResources
import org.gradle.test.fixtures.keystore.TestKeyStore
import org.gradle.test.fixtures.server.http.BlockingHttpsServer
import org.gradle.test.fixtures.server.http.TestProxyServer
import org.gradle.wrapper.Download
import org.junit.Rule
import spock.lang.Issue

import static org.gradle.test.matchers.UserAgentMatcher.matchesNameAndVersion
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat

class WrapperHttpsIntegrationTest extends AbstractWrapperIntegrationSpec {
    @Rule BlockingHttpsServer server = new BlockingHttpsServer()
    @Rule TestProxyServer proxyServer = new TestProxyServer()
    @Rule TestResources resources = new TestResources(temporaryFolder)

    def setup() {
        TestKeyStore keyStore = TestKeyStore.init(resources.dir)
        // We need to set the SSL properties as arguments here even for non-embedded test mode
        // because we want them to be set on the wrapper client JVM, not the daemon one
        wrapperExecuter.withArgument("-Djavax.net.ssl.trustStore=$keyStore.keyStore.path")
        wrapperExecuter.withArgument("-Djavax.net.ssl.trustStorePassword=$keyStore.keyStorePassword")
        server.configure(keyStore)

        file("build.gradle") << """
    task hello {
        doLast {
            println 'hello'
        }
    }

    task echoProperty {
        doLast {
            println "fooD=" + project.properties["fooD"]
        }
    }
"""
    }

    private prepareWrapper(String baseUrl) {
        prepareWrapper(new URI("${baseUrl}/gradlew/dist"))
    }

    def "does not warn about using basic authentication over secure connection"() {
        given:
        server.withBasicAuthentication("jdoe", "changeit")
        server.start()
        prepareWrapper("https://jdoe:changeit@localhost:${server.port}")
        server.expect(server.get("/gradlew/dist")
                .expectUserAgent(matchesNameAndVersion("gradlew", Download.UNKNOWN_VERSION))
                .sendFile(distribution.binDistribution))


        when:
        result = wrapperExecuter.withTasks('hello').run()

        then:
        outputDoesNotContain('Please consider using HTTPS.')
    }

    def "downloads wrapper via proxy"() {
        given:
        server.start()
        proxyServer.configureProxy(executer, 'https')
        proxyServer.start()
        prepareWrapper(server.uri.toString())
        file("gradle.properties") << """
    systemProp.https.proxyHost=localhost
    systemProp.https.proxyPort=${proxyServer.port}
    systemProp.https.nonProxyHosts=${JavaVersion.current() >= JavaVersion.VERSION_1_7 ? '' : '~localhost'}
"""
        server.expect(server.get("/gradlew/dist").sendFile(distribution.binDistribution))

        when:
        def result = wrapperExecuter.withTasks('hello').run()

        then:
        assertThat(result.output, containsString('hello'))

        and:
        proxyServer.requestCount == 1
    }

    @Issue('https://github.com/gradle/gradle/issues/5052')
    def "downloads wrapper via authenticated proxy"() {
        given:
        server.start()
        proxyServer.configureProxy(executer, 'https', 'my_user', 'my_password')
        proxyServer.start()

        and:
        prepareWrapper(server.uri.toString())
        server.expect(server.get("/gradlew/dist").sendFile(distribution.binDistribution))
        file("gradle.properties") << """
    systemProp.https.proxyHost=localhost
    systemProp.https.proxyPort=${proxyServer.port}
    systemProp.https.nonProxyHosts=${JavaVersion.current() >= JavaVersion.VERSION_1_7 ? '' : '~localhost'}
    systemProp.https.proxyUser=my_user
    systemProp.https.proxyPassword=my_password
"""

        when:
        def result = wrapperExecuter.withTasks('hello').run()

        then:
        assertThat(result.output, containsString('hello'))

        and:
        proxyServer.requestCount == 1
    }
}
