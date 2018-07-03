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

package com.adobe.aam.metrics.agent;

import com.adobe.aam.metrics.core.MetricRegistryReporter;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.bucket.MetricBucket;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * The MetricAgent will monitor a set of metrics. On each cycle, it will retrieve the latest values and will send
 * the metrics to the backend using a MetricClient.
 */
@Value.Immutable
public abstract class MetricAgentConfig {

    /**
     * Instructs the MetricAgent how often to collect the metric values and send these to the backend(s).
     *
     * @return the MetricAgent cycle duration.
     */
    @Value.Default
    public Duration getCollectFrequency() {
        return Duration.ofMillis(60 * 1000);
    }

    /**
     * @return The list of monitored metrics.
     */
    public abstract Set<Metric> getMetrics();

    /**
     * @return The list of monitored metric buckets.
     */
    public abstract Set<MetricBucket> getMetricBuckets();

    /**
     * @return The list of monitored Codahale metric registries.
     */
    public abstract Set<MetricRegistryReporter> getMetricRegistries();

    /**
     * Instructs the MetricAgent to use a ValueProvider for a specific Metric. This means that instead of doing
     * a metric.getAndReset(), it will actually fetch the value from the ValueProvider.
     */
    public abstract Map<Metric, ValueProvider> getMetricValueProviders();

    /**
     * If this is set to true, then the MetricAgent ignore those metrics that have not been updated
     * since the last cycle. This optimizes the amount of data sent to the backend(s).
     */
    @Value.Default
    public boolean sendOnlyRecentlyUpdatedMetrics() {
        return true;
    }

}
