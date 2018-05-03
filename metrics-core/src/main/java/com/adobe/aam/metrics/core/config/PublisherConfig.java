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

package com.adobe.aam.metrics.core.config;

import com.adobe.aam.metrics.filter.MetricFilter;
import com.adobe.aam.metrics.filter.WhitelistMetricFilter;
import com.typesafe.config.Config;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.adobe.aam.metrics.core.config.ConfigUtils.getInt;

/**
 * Immutable class containing the configuration attributes for a publisher.
 * This instructs the client:
 * - to which backend host to connect
 * - how many metrics per connection it should send
 * - how many retries to attempt when sending metrics
 */

@Value.Immutable
public interface PublisherConfig {

    String type();

    String host();

    Optional<Integer> port();

    String name();

    /**
     * Specifies a batch size, which represents how many metric lines to send to the backend server in one connection.
     */
    @Value.Default
    default int batchSize() {
        return 500;
    }

    @Value.Default
    default int socketTimeout() {
        return 10000; // ms
    }

    @Value.Default
    default List<MetricFilter> metricFilters() {
        return Collections.emptyList();
    }

    @Value.Default
    default RetryPolicyConfig retryPolicyConfig() {
        return RetryPolicyConfig.defaultConfig();
    }

    @Value.Default
    default CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.defaultConfig();
    }

    static PublisherConfig fromConfig(Config config) {

        String name = config.getString("name");

        return ImmutablePublisherConfig.builder()
                .host(config.getString("host"))
                .port(getInt(config, "port"))
                .name(name)
                .type(config.getString("type"))
                .batchSize(getInt(config, "batch_size", 500))
                .socketTimeout(getInt(config, "socket_timeout", 10000))
                .retryPolicyConfig(RetryPolicyConfig.fromConfig(config))
                .circuitBreakerConfig(CircuitBreakerConfig.fromConfig(config, name))
                .addMetricFilters(generateMetricFilter(config, "whitelist"))
                .addMetricFilters(generateMetricFilter(config, "blacklist"))
                .build();
    }

    static MetricFilter generateMetricFilter(Config config, String key) {
        String path = "filter." + key;
        return config.hasPath(path)
                ? generateMetricFilter(config.getStringList(path))
                : MetricFilter.ALLOW_ALL;
    }

    static MetricFilter generateMetricFilter(List<String> whitelist) {
        return whitelist == null
                ? MetricFilter.ALLOW_ALL
                : new WhitelistMetricFilter(whitelist);
    }
}
