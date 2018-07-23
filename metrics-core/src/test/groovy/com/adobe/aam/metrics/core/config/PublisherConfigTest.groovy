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

package com.adobe.aam.metrics.core.config

import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Tags
import com.typesafe.config.Config
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class PublisherConfigTest extends Specification {

    @Shared
            tags = ImmutableTags.builder().appName("myapp").regionName("us-east-1").build()

    def "builder test from typesafe"() {
        setup:
        Config config = Mock(Config) {
            hasPath(_) >> { args ->
                if (!args[0].equals("relabel")) {
                    return true
                }
                return false
            }
        }

        when:
        PublisherConfig.fromConfig(config, tags)

        then:
        1 * config.getString("name") >> "myname"
        1 * config.getString("type") >> "graphite"
        1 * config.getString("host") >> "myhost"
        1 * config.getInt("port") >> 2003
        1 * config.getInt("batch_size") >> 500

        1 * config.getBoolean("retry_policy.enabled") >> true
        1 * config.getInt("retry_policy.retry_attempts") >> 3
        1 * config.getInt("retry_policy.retry_wait_time_ms") >> 1000

        1 * config.getBoolean("circuit_breaker.enabled")
        1 * config.getInt("circuit_breaker.success_threshold") >> 3
        1 * config.getInt("circuit_breaker.failure_threshold") >> 3
        1 * config.getDuration("circuit_breaker.connect_wait_time_ms", TimeUnit.MILLISECONDS) >> 1000
        1 * config.getDuration("circuit_breaker.request_timeout_ms", TimeUnit.MILLISECONDS) >> 1000
    }

}
