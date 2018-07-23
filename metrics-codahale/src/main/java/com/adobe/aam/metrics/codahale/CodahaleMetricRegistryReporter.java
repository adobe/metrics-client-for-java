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

import com.adobe.aam.metrics.core.MetricRegistryReporter;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.SimpleMetric;
import com.codahale.metrics.*;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Collection<Metric> getMetrics() {

        return Stream.of(
                getCounters(),
                getHistograms(),
                getTimers(),
                getMeters(),
                getGauges()
        )
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
    }

    private Stream<Metric> getCounters() {
        return metricRegistry.getCounters(config.metricFilter())
                .entrySet()
                .stream()
                .map(entry -> reportCounter(entry.getKey(), entry.getValue()));
    }

    private Stream<Metric> getHistograms() {
        return metricRegistry.getHistograms(config.metricFilter())
                .entrySet()
                .stream()
                .flatMap(entry -> reportHistogram(entry.getKey(), entry.getValue()));
    }

    private Stream<Metric> getTimers() {
        return metricRegistry.getTimers(config.metricFilter())
                .entrySet()
                .stream()
                .flatMap(entry -> reportTimer(entry.getKey(), entry.getValue()));
    }

    private Stream<Metric> getMeters() {
        return metricRegistry.getMeters(config.metricFilter())
                .entrySet()
                .stream()
                .flatMap(entry -> reportMetered(entry.getKey(), entry.getValue()));
    }

    private Stream<Metric> getGauges() {
        return metricRegistry.getGauges(config.metricFilter())
                .entrySet()
                .stream()
                .flatMap(entry -> reportGauge(entry.getKey(), entry.getValue()));
    }

    private Stream<Metric> reportHistogram(String name, Histogram histogram) {
        final Snapshot snapshot = histogram.getSnapshot();

        return Stream.of(
                generateMetric(name, Metric.Type.COUNT, histogram.getCount()),
                generateMetric(name, Metric.Type.MAX, snapshot.getMax()),
                generateMetric(name, Metric.Type.AVG, snapshot.getMean()),
                generateMetric(name, Metric.Type.MIN, snapshot.getMin()),
                generateMetric(name, Metric.Type.STANDARD_DEVIATION, snapshot.getStdDev()),
                generateMetric(name, Metric.Type.PERCENTILE_50, snapshot.getMedian()),
                generateMetric(name, Metric.Type.PERCENTILE_75, snapshot.get75thPercentile()),
                generateMetric(name, Metric.Type.PERCENTILE_95, snapshot.get95thPercentile()),
                generateMetric(name, Metric.Type.PERCENTILE_98, snapshot.get98thPercentile()),
                generateMetric(name, Metric.Type.PERCENTILE_99, snapshot.get99thPercentile()),
                generateMetric(name, Metric.Type.PERCENTILE_999, snapshot.get999thPercentile())
        );
    }

    private Metric reportCounter(String name, Counting counter) {
        return new CodahaleCounter(formatMetricName(name), counter);
    }

    private Stream<Metric> reportTimer(String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();

        return Stream.concat(
                reportMetered(name, timer),
                Stream.of(
                        generateMetric(name, Metric.Type.MAX, convertDuration(snapshot.getMax())),
                        generateMetric(name, Metric.Type.AVG, convertDuration(snapshot.getMean())),
                        generateMetric(name, Metric.Type.MIN, convertDuration(snapshot.getMin())),
                        generateMetric(name, Metric.Type.STANDARD_DEVIATION, convertDuration(snapshot.getStdDev())),
                        generateMetric(name, Metric.Type.PERCENTILE_50, convertDuration(snapshot.getMedian())),
                        generateMetric(name, Metric.Type.PERCENTILE_75, convertDuration(snapshot.get75thPercentile())),
                        generateMetric(name, Metric.Type.PERCENTILE_95, convertDuration(snapshot.get95thPercentile())),
                        generateMetric(name, Metric.Type.PERCENTILE_98, convertDuration(snapshot.get98thPercentile())),
                        generateMetric(name, Metric.Type.PERCENTILE_99, convertDuration(snapshot.get99thPercentile())),
                        generateMetric(name, Metric.Type.PERCENTILE_999, convertDuration(snapshot.get999thPercentile()))
                ));
    }

    private Stream<Metric> reportGauge(String name, Gauge gauge) {
        Optional<Double> value = getDouble(name, gauge.getValue().toString());
        if (value.isPresent() && !StringUtils.isBlank(name)) {
            Metric.Type type = Metric.Type.fromName(name);
            return Stream.of(generateMetric(name, type, value.get()));
        }
        return Stream.empty();
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

    private Stream<Metric> reportMetered(String name, Metered meter) {
        return Stream.of(
                reportCounter(name, meter),
                generateMetric(name, Metric.Type.MEAN_RATE, meter.getMeanRate()),
                generateMetric(name, Metric.Type.RATE_1MIN, meter.getOneMinuteRate()),
                generateMetric(name, Metric.Type.RATE_5MIN, meter.getFiveMinuteRate()),
                generateMetric(name, Metric.Type.RATE_15MIN, meter.getFifteenMinuteRate())
        );
    }

    private double convertDuration(double duration) {
        return duration * config.durationFactor();
    }

    private Metric generateMetric(String name, Metric.Type type, double value) {
        return new SimpleMetric(formatMetricName(name), type, value);
    }

    private String formatMetricName(String name) {
        return config.prefix().map(s -> s + "." + name).orElse(name);
    }
}
