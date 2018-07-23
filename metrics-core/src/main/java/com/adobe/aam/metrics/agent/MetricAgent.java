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

import com.adobe.aam.metrics.BufferedMetricClient;
import com.adobe.aam.metrics.core.MetricRegistryReporter;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.SimpleMetric;
import com.adobe.aam.metrics.metric.bucket.MetricBucket;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The MetricAgent periodically reports the metric values to the provided metric client.
 */
public class MetricAgent extends AbstractScheduledService {

    private final static Logger logger = LoggerFactory.getLogger(MetricAgent.class);

    private final BufferedMetricClient metricClient;
    private final Collection<Metric> metrics;
    private final Collection<MetricBucket> metricBuckets;
    private final Collection<MetricRegistryReporter> codahaleMetricRegistryReporters;
    private final Duration collectFrequency;
    private final Map<Metric, ValueProvider> metricValueProviders;

    public MetricAgent(BufferedMetricClient metricClient, MetricAgentConfig config) {
        this.metricClient = metricClient;
        this.collectFrequency = config.getCollectFrequency();
        this.metrics = config.getMetrics();
        this.metricBuckets = config.getMetricBuckets();
        this.codahaleMetricRegistryReporters = config.getMetricRegistries();
        this.metricValueProviders = config.getMetricValueProviders();
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        logger.info("Starting metric agent.");
    }

    @Override
    protected void shutDown() throws Exception {
        logger.info("Stopping metric agent.");
        metricClient.shutdown();
        super.shutDown();
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(
                0,
                collectFrequency.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected void runOneIteration() {

        ImmutableSet.Builder<Metric> allMetrics = ImmutableSet.<Metric>builder()
                .addAll(metrics);
        metricBuckets
                .forEach(bucket -> allMetrics.addAll(bucket.getMetrics()));

        Stream<Metric> metrics = getWithProvider(Sets.union(allMetrics.build(), metricValueProviders.keySet()));
        Stream<Metric> metrics2 = getFromMetricRegistries();
        Set<Metric> combinedMetrics = Stream.concat(metrics, metrics2)
                .map(metric -> shouldResetMetric(metric)
                        ? new SimpleMetric(metric.getName(), metric.getType(), metric.getAndReset(), metric.getLastTrackTime())
                        : metric)
                .collect(Collectors.toSet());
        metricClient.send(combinedMetrics);
        metricClient.flush();
    }

    private boolean shouldResetMetric(Metric metric) {
        return metric.getType() != Metric.Type.COUNT;
    }

    private Stream<Metric> getWithProvider(Collection<Metric> metrics) {
        return metrics.stream()
                .map(this::getWithValueProvider)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<Metric> getWithValueProvider(Metric metric) {
        boolean hasValueProvider = metricValueProviders.containsKey(metric);
        return hasValueProvider
                ? reportMetricWithValueProvider(metric, metricValueProviders.get(metric))
                : Optional.of(metric);
    }

    private Optional<Metric> reportMetricWithValueProvider(Metric metric, ValueProvider valueProvider) {
        Optional<Double> value = valueProvider.getValue();

        if (!value.isPresent()) {
            logger.trace("Metric does not have a value. Not sending over the network. {}", metric);
        }

        return value.map(val -> new SimpleMetric(metric.getName(), metric.getType(), val));
    }

    private Stream<Metric> getFromMetricRegistries() {

        return codahaleMetricRegistryReporters
                .stream()
                .flatMap(reporter -> reporter.getMetrics().stream());
    }
}
