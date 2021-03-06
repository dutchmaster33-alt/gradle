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

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy

import org.gradle.api.internal.FeaturePreviews

class DefaultVersionComparatorWithUpdateKeywordsTest extends AbstractDefaultVersionComparatorTest {

    def setup() {
        def previews = new FeaturePreviews()
        previews.enableFeature(FeaturePreviews.Feature.VERSION_SORTING_V2)
        comparator = new DefaultVersionComparator(previews)
    }

    def "extends special qualifier treatment to snapshot, ga and sp"() {
        expect:
        compare(smaller, larger) < 0
        compare(larger, smaller) > 0
        compare(smaller, smaller) == 0
        compare(larger, larger) == 0

        where:
        smaller        | larger
        "1.0-rc"       | "1.0-snapshot"
        "1.0-snapshot" | "1.0-release"
        "1.0-release"  | "1.0-sp1"
        "1.0-snapshot" | "1.0-final"
        "1.0-snapshot" | "1.0-ga"
        "1.0-sp"       | "1.0"
        "1.0-sp1"      | "1.0"
    }

    def 'does sort ga, final and release alphabetically'() {
        expect:
        compare(smaller, larger) < 0
        compare(larger, smaller) > 0
        compare(smaller, smaller) == 0
        compare(larger, larger) == 0

        where:
        smaller | larger
        "1.0-final"   | "1.0-release"
        "1.0-final"   | "1.0-ga"
        "1.0-ga"   | "1.0-release"
    }
}
