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

package com.adobe.aam.metrics.codahale;

import com.adobe.aam.metrics.MetricClient;
import com.adobe.aam.metrics.core.MetricRegistryReporter;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.SimpleMetric;
import com.codahale.metrics.*;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A Codahale opinionated wrapper for 'MetricClient' making use of metric registries
 */
public class CodahaleMetricRegistryReporter implements MetricRegistryReporter {

    private static final Logger LOG = LoggerFactory.getLogger(CodahaleMetricRegistryReporter.class);
    private static final MetricReporterConfig EMPTY_CONFIG = ImmutableMetricReporterConfig.builder().build();

    @Value.Immutable
    public interface MetricReporterConfig {
        Optional<String> prefix();

        @Value.Default
        default MetricFilter metricFilter() {
            return MetricFilter.ALL;
        }

        @Value.Default
        default double durationFactor() {
            return 1.0 / TimeUnit.MILLISECONDS.toNanos(1);
        }
    }

    private final MetricRegistry metricRegistry;
    private final MetricReporterConfig config;

    public CodahaleMetricRegistryReporter(MetricRegistry metricRegistry, MetricReporterConfig config) {
        this.metricRegistry = metricRegistry;
        this.config = config;
    }

    public CodahaleMetricRegistryReporter(MetricRegistry metricRegistry) {
        this(metricRegistry, EMPTY_CONFIG);
    }

    public CodahaleMetricRegistryReporter(MetricRegistry metricRegistry, String prefix) {
        this(metricRegistry,
                ImmutableMetricReporterConfig.builder().prefix(prefix).build()
        );
    }

    /**
     * Report configured metric registries to the provided metric client
     */
    public synchronized void reportTo(MetricClient metricClient) {

        long timestamp = System.currentTimeMillis() / 1000;

        metricRegistry.getCounters(config.metricFilter())
                .forEach((name, counter) -> reportCounter(metricClient, name, counter, timestamp));

        metricRegistry.getHistograms(config.metricFilter())
                .forEach((name, histogram) -> reportHistogram(metricClient, name, histogram, timestamp));

        metricRegistry.getMeters(config.metricFilter())
                .forEach((name, meter) -> reportMetered(metricClient, name, meter, timestamp));

        metricRegistry.getTimers(config.metricFilter())
                .forEach((name, timer) -> reportTimer(metricClient, name, timer, timestamp));

        metricRegistry.getGauges(config.metricFilter())
                .forEach((name, gauge) -> reportGauge(metricClient, name, gauge, timestamp));
    }

    private void reportHistogram(MetricClient metricClient, String name, Histogram histogram, long timestamp) {
        final Snapshot snapshot = histogram.getSnapshot();
        sendMetric(metricClient, name, Metric.Type.COUNT, getNewCounterValue(name, histogram.getCount()), timestamp);
        sendMetric(metricClient, name, Metric.Type.MAX, snapshot.getMax(), timestamp);
        sendMetric(metricClient, name, Metric.Type.AVG, snapshot.getMean(), timestamp);
        sendMetric(metricClient, name, Metric.Type.MIN, snapshot.getMin(), timestamp);
        sendMetric(metricClient, name, Metric.Type.STANDARD_DEVIATION, snapshot.getStdDev(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_50, snapshot.getMedian(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_75, snapshot.get75thPercentile(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_95, snapshot.get95thPercentile(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_98, snapshot.get98thPercentile(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_99, snapshot.get99thPercentile(), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_999, snapshot.get999thPercentile(), timestamp);
    }

    private void reportCounter(MetricClient metricClient, String name, Counter counter, long timestamp) {
        sendMetric(metricClient, name, Metric.Type.COUNT, getNewCounterValue(name, counter.getCount()), timestamp);
    }

    private void reportTimer(MetricClient metricClient, String name, Timer timer, long timestamp) {
        final Snapshot snapshot = timer.getSnapshot();
        sendMetric(metricClient, name, Metric.Type.MAX, convertDuration(snapshot.getMax()), timestamp);
        sendMetric(metricClient, name, Metric.Type.AVG, convertDuration(snapshot.getMean()), timestamp);
        sendMetric(metricClient, name, Metric.Type.MIN, convertDuration(snapshot.getMin()), timestamp);
        sendMetric(metricClient, name, Metric.Type.STANDARD_DEVIATION, convertDuration(snapshot.getStdDev()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_50, convertDuration(snapshot.getMedian()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_75, convertDuration(snapshot.get75thPercentile()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_95, convertDuration(snapshot.get95thPercentile()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_98, convertDuration(snapshot.get98thPercentile()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_99, convertDuration(snapshot.get99thPercentile()), timestamp);
        sendMetric(metricClient, name, Metric.Type.PERCENTILE_999, convertDuration(snapshot.get999thPercentile()), timestamp);

        reportMetered(metricClient, name, timer, timestamp);
    }

    private void reportGauge(MetricClient metricClient, String name, Gauge gauge, long timestamp) {
        Optional<Double> value = getDouble(name, gauge.getValue().toString());
        if (value.isPresent() && !StringUtils.isBlank(name)) {
            Metric.Type type = Metric.Type.fromName(name);
            double metricValue = type == Metric.Type.COUNT ? getNewCounterValue(name, value.get()) : value.get();
            sendMetric(metricClient, name, type, metricValue, timestamp);
        }
    }

    private Optional<Double> getDouble(String name, String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        if (value.trim().equalsIgnoreCase("true")) {
            return Optional.of(1d);
        }

        if (value.trim().equalsIgnoreCase("false")) {
            return Optional.of(0d);
        }

        try {
            return Optional.of(Double.valueOf(value));
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse metric value for {}: {}", name, e.getMessage());
        }
        return Optional.empty();
    }

    private void reportMetered(MetricClient metricClient, String name, Metered meter, long timestamp) {
        sendMetric(metricClient, name, Metric.Type.COUNT, getNewCounterValue(name, meter.getCount()), timestamp);
        sendMetric(metricClient, name, Metric.Type.MEAN_RATE, meter.getMeanRate(), timestamp);
        sendMetric(metricClient, name, Metric.Type.RATE_1MIN, meter.getOneMinuteRate(), timestamp);
        sendMetric(metricClient, name, Metric.Type.RATE_5MIN, meter.getFiveMinuteRate(), timestamp);
        sendMetric(metricClient, name, Metric.Type.RATE_15MIN, meter.getFifteenMinuteRate(), timestamp);
    }

    private double convertDuration(double duration) {
        return duration * config.durationFactor();
    }

    private void sendMetric(MetricClient metricClient, String name, Metric.Type type, double value, long timestamp) {
        Metric metric = new SimpleMetric(formatMetricName(name), type, value);
        metricClient.send(metric, timestamp);
    }

    private String formatMetricName(String name) {
        return config.prefix().map(s -> s + "." + name).orElse(name);
    }

    // Workaround for https://github.com/dropwizard/metrics/issues/143
    // External reset operation is not supported on metrics.
    private Map<String, Double> counterValues = Maps.newHashMap();

    private double getNewCounterValue(String metric, double latestValue) {
        // Subtract the last known counter value (if any) from the newest one.
        if (!counterValues.containsKey(metric)) {
            counterValues.put(metric, latestValue);
            return latestValue;
        }

        double newValue = latestValue - counterValues.get(metric);
        counterValues.put(metric, latestValue);
        return newValue;
    }
}
