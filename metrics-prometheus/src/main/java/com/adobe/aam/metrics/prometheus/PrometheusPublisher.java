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
package com.adobe.aam.metrics.prometheus;

import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.config.RelabelConfig;
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.metric.ImmutableMetricLabels;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.MetricLabels;
import com.google.common.collect.Maps;
import io.prometheus.client.CollectorRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public final class PrometheusPublisher implements Publisher {

    private final Map<String, PrometheusSnapshotCollector> trackedMetrics = Maps.newConcurrentMap();
    private final CollectorRegistry prometheusRegistry;
    private final PublisherConfig config;

    public PrometheusPublisher(PublisherConfig config) {
        this(config, CollectorRegistry.defaultRegistry);
    }

    public PrometheusPublisher(PublisherConfig config, CollectorRegistry prometheusRegistry) {
        this.config = config;
        this.prometheusRegistry = prometheusRegistry;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void publishMetrics(Collection<Metric> metrics) {
        metrics
                .stream()
                .map(this::relabelMetric)
                .forEach(metric -> {
                    String key = getKey(metric);
                    if (trackedMetrics.containsKey(key)) {
                        trackedMetrics.get(key).updateMetric(metric);
                    } else {
                        PrometheusSnapshotCollector collector = new PrometheusSnapshotCollector(metric, config().tags());
                        prometheusRegistry.register(collector);
                        trackedMetrics.put(key, collector);
                    }
                });
    }

    private Metric relabelMetric(Metric metric) {
        List<RelabelConfig> relabelConfigs = config().relabelConfigs();
        if (relabelConfigs.isEmpty()) {
            return metric;
        }

        String fullMetricName = metric.getLabels().metricName();
        boolean[] found = new boolean[1];
        found[0] = false;
        StringBuilder newName = new StringBuilder(fullMetricName);
        ImmutableMetricLabels.Builder labels = ImmutableMetricLabels.builder()
                .preLabelName(metric.getLabels().preLabelName())
                .preLabelValue(metric.getLabels().preLabelValue())
                .addAllPostLabelNames(metric.getLabels().postLabelNames())
                .addAllPostLabelValues(metric.getLabels().postLabelValues());

        int[] totalDeleted = new int[1];
        totalDeleted[0] = 0;
        relabelConfigs.forEach(relabelConfig -> {
            Matcher matcher = relabelConfig.regex().matcher(fullMetricName);
            if (matcher.matches()) {
                relabelConfig.groupToLabelName()
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .forEach(entry -> {
                            int groupId = entry.getKey();
                            String labelName = entry.getValue();
                            if (groupId <= matcher.groupCount()) {
                                String labelValue = matcher.group(groupId);
                                labels.addPostLabelNames(labelName);
                                labels.addPostLabelValues(labelValue);

                                int start = matcher.start(groupId) - totalDeleted[0];
                                int end = matcher.end(groupId) - totalDeleted[0];
                                newName.replace(start, end, "");
                                totalDeleted[0] += end - start;
                                found[0] = true;
                            }
                        });
            }
        });

        return found[0] ? new PrometheusMetric(metric, labels.metricName(sanitize(newName.toString())).build()) : metric;
    }

    private String sanitize(String metricName) {
        return metricName
                .replace("..", ".")
                .replaceAll("^\\.", "")
                .replaceAll("\\.$", "");
    }

    static class PrometheusMetric extends Metric {

        private final Metric metric;

        public PrometheusMetric(Metric metric, MetricLabels labels) {
            super(labels);
            this.metric = metric;
        }

        @Override
        public Type getType() {
            return metric.getType();
        }

        @Override
        protected void doTrack(double value) {
            metric.track(value);
        }

        @Override
        public double doGetAndReset() {
            return metric.doGetAndReset();
        }

        @Override
        public double get() {
            return metric.get();
        }
    }

    private String getKey(Metric metric) {
        return metric.getName() + "_" + metric.getType().getName();
    }

    @Override
    public PublisherConfig config() {
        return config;
    }
}
