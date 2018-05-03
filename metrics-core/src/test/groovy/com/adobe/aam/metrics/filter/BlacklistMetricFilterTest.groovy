/*
 * Copyright 2018 Adobe Systems Incorporated. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 */

package com.adobe.aam.metrics.filter

import spock.lang.Specification

import static com.adobe.aam.metrics.Fixtures.metricSnapshot
import static com.adobe.aam.metrics.metric.Metric.Type.*

class BlacklistMetricFilterTest extends Specification {

    def "test blacklist filter"(blacklisted, metric, expectedIsAllowed) {
        setup:
        def filter = new BlacklistMetricFilter(blacklisted)

        when:
        def isAllowed = filter.isAllowed(metric)

        then:
        isAllowed == expectedIsAllowed

        where:
        blacklisted = ["*p98", ".*p50", "ugly"]

        metric                                           | expectedIsAllowed
        metricSnapshot("good")                     | true
        metricSnapshot("my.metric", PERCENTILE_99) | true
        metricSnapshot("my.metric", PERCENTILE_98) | false
        metricSnapshot("my.metric", PERCENTILE_95) | true
        metricSnapshot("my.metric", PERCENTILE_50) | false
        metricSnapshot("my.ugly.metric")           | false
        metricSnapshot("my.good.metric")           | true
    }
}
