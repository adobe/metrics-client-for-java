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
import com.adobe.aam.metrics.metric.Tags;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.typesafe.config.*;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.adobe.aam.metrics.core.config.ConfigUtils.getBoolean;
import static com.adobe.aam.metrics.core.config.ConfigUtils.getDurationMs;
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

    @Value.Default
    default int publishFrequencyMs() {
        return 60000;
    }

    Tags tags();

    List<RelabelConfig> relabelConfigs();

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
        return CircuitBreakerConfig.defaultConfig(name());
    }

    /**
     * if true, metrics that have not been updated in the last interval are not published.
     * This optimizes the amount of data sent to the backend(s).
     */
    @Value.Default
    default boolean sendOnlyRecentlyUpdatedMetrics() {
        return true;
    }

    /**
     * if true, the publisher will send the diff between the current counter and the value from the last iteration.
     */
    @Value.Default
    default boolean resetCounters() {
        return true;
    }

    static PublisherConfig fromConfig(Config config, Tags tags) {

        String name = config.getString("name");


        return ImmutablePublisherConfig.builder()
                .host(config.hasPath("host") ? config.getString("host") : "")
                .port(getInt(config, "port"))
                .name(name)
                .type(config.getString("type"))
                .batchSize(getInt(config, "batch_size", 500))
                .socketTimeout(getInt(config, "socket_timeout", 10000))
                .retryPolicyConfig(RetryPolicyConfig.fromConfig(config))
                .circuitBreakerConfig(CircuitBreakerConfig.fromConfig(config, name))
                .addMetricFilters(generateMetricFilter(config, "whitelist"))
                .addMetricFilters(generateMetricFilter(config, "blacklist"))
                .sendOnlyRecentlyUpdatedMetrics(getBoolean(config, "sendOnlyRecentlyUpdatedMetrics", false))
                .tags(tags)
                .resetCounters(getBoolean(config, "resetCounters", false))
                .publishFrequencyMs(getDurationMs(config, "publishFrequency", 60000))
                .relabelConfigs(getRelabelConfigs(config))
                .build();
    }

    static List<RelabelConfig> getRelabelConfigs(Config config) {
        if (!config.hasPath("relabel")) {
            return Collections.emptyList();
        }
        Config relabelConfig = config.getConfig("relabel");

        List<RelabelConfig> relabelConfigs = Lists.newArrayList();
        relabelConfig.entrySet()
                .forEach(entry -> {
                            List<String> keys = ConfigUtil.splitPath(entry.getKey());
                            Pattern regex = Pattern.compile(keys.get(0));
                            ImmutableRelabelConfig.Builder relabelConfigBuilder = ImmutableRelabelConfig.builder()
                                    .regex(regex);

                            ConfigList list = (ConfigList) entry.getValue();

                            list.forEach(
                                    e -> {
                                        ConfigObject item = (ConfigObject) e;
                                        item.forEach((labelName, groupId) -> {
                                            int groupIdInt = Integer.parseInt(((String) groupId.unwrapped()).replace("$", ""));
                                            relabelConfigBuilder.putGroupToLabelName(groupIdInt, labelName);
                                        });
                                    }
                            );

                            relabelConfigs.add(relabelConfigBuilder.build());
                        }
                );
        return relabelConfigs;
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
