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

subprojects {
    apply(plugin = "groovy")
    repositories {
        jcenter()
    }
    dependencies {
        "implementation"(localGroovy())
    }
}

project(":astTransformationConsumer") {
// tag::groovy-compilation-avoidance[]
    val astTransformation by configurations.creating
    dependencies {
        astTransformation(project(":astTransformation"))
    }
    tasks.withType<GroovyCompile>().configureEach {
        astTransformationClasspath.from(astTransformation)
    }
// end::groovy-compilation-avoidance[]
}
