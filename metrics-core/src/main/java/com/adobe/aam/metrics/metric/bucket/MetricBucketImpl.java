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

package com.adobe.aam.metrics.metric.bucket;

import com.adobe.aam.metrics.metric.ImmutableMetricLabels;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.MetricLabels;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class MetricBucketImpl implements MetricBucket {

    private final ConcurrentMap<MetricLabels, Metric> bucket = Maps.newConcurrentMap();
    private final Metric.Type type;
    private final MetricLabels parentLabels;

    public MetricBucketImpl(String name, Metric.Type type) {
        this.parentLabels = ImmutableMetricLabels
                .builder()
                .metricName(name)
                .build();
        this.type = type;
    }

    public MetricBucketImpl(String name, Metric.Type type, String preLabelName, List<String> postLabelNames) {
        this.parentLabels = ImmutableMetricLabels
                .builder()
                .metricName(name)
                .preLabelName(preLabelName)
                .addAllPostLabelNames(postLabelNames)
                .build();
        this.type = type;
    }

    public MetricBucketImpl(String name, Metric.Type type, List<String> postLabelNames) {
        this.parentLabels = ImmutableMetricLabels
                .builder()
                .metricName(name)
                .addAllPostLabelNames(postLabelNames)
                .build();
        this.type = type;
    }

    public MetricBucketImpl(String name, Metric.Type type, String postLabelName) {
        this.parentLabels = ImmutableMetricLabels
                .builder()
                .metricName(name)
                .addPostLabelNames(postLabelName)
                .build();
        this.type = type;
    }

    private Metric safeGetMetric(MetricLabels labels) {
        MetricLabels normalizedLabels = labels.normalize();
        Metric metric = bucket.get(normalizedLabels);
        if (metric == null) {
            // Metric was not yet created for this identifier.
            Metric newMetric = Metric.newInstance(normalizedLabels, type);
            metric = bucket.putIfAbsent(normalizedLabels, newMetric);
            if (metric == null) {
                // If this is the lucky thread that managed to put the metric, then return it as is.
                metric = newMetric;
            }
        }

        return metric;
    }

    @Override
    public Collection<Metric> getMetrics() {
        return bucket.values();
    }

    @Override
    public Metric getMetric(Object suffix) {
        MetricLabels childLabels = ImmutableMetricLabels.copyOf(parentLabels)
                .withPostLabelValues(suffix.toString());

        return safeGetMetric(childLabels);
    }

    public Metric getMetricWithPrefix(Object prefix) {
        MetricLabels childLabels = ImmutableMetricLabels.copyOf(parentLabels)
                .withPreLabelValue(prefix.toString());

        return safeGetMetric(childLabels);
    }

    public Metric getMetric(Object prefix, Object... suffixes) {
        List<String> postLabelValues = Arrays.stream(suffixes).map(Object::toString)
                .collect(Collectors.toList());
        MetricLabels childLabels = ImmutableMetricLabels.copyOf(parentLabels)
                .withPreLabelValue(prefix.toString())
                .withPostLabelValues(postLabelValues);
        return safeGetMetric(childLabels);
    }

    @Override
    public Metric getParentMetric() {
        return safeGetMetric(parentLabels);
    }

    public MetricLabels labels() {
        return parentLabels;
    }
}
