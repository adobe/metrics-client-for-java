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

package com.adobe.aam.metrics.sample;

import com.adobe.aam.metrics.codahale.histogram.SamplingHistogram;
import com.adobe.aam.metrics.codahale.registry.SamplingMetricRegistry;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.bucket.MetricBucket;
import com.adobe.aam.metrics.metric.bucket.MetricBucketImpl;

import java.util.Collection;

public enum AppMetrics implements MetricBucket {

    CUSTOM_COUNTER("my.custom.metric", Metric.Type.COUNT),
    CUSTOM_AVG("my.other.custom.metric", Metric.Type.AVG),
    RELABEL_EXAMPLE_COUNTER("db.database1.table.users.inserts", Metric.Type.COUNT);

    public static final SamplingMetricRegistry registry = new SamplingMetricRegistry(0.1);
    public static final SamplingHistogram REQUEST_TIME_HISTOGRAM = registry.samplingHistogram("request.time");
    private final MetricBucketImpl bucket;

    AppMetrics(String name, Metric.Type bucketType) {

        bucket = new MetricBucketImpl(name, bucketType);
    }

    public void track(double value) {
        getParentMetric().track(value);
    }

    public void trackFor(String identifier, double value) {
        // Increment for parent.
        getParentMetric().track(value);

        if (identifier != null) {
            getMetricFor(identifier).track(value);
        }
    }

    public Metric getMetricFor(Object identifier) {
        return bucket.getMetric(identifier);
    }

    @Override
    public Collection<Metric> getMetrics() {
        return bucket.getMetrics();
    }

    @Override
    public Metric getMetric(Object child) {
        return bucket.getMetric(child);
    }

    @Override
    public Metric getParentMetric() {
        return bucket.getParentMetric();
    }

}
