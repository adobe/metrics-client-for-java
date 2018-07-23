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

import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.MetricLabels;
import com.adobe.aam.metrics.metric.Tags;
import com.google.common.collect.Lists;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PrometheusSnapshotCollector extends Collector {

    private final static List<String> LABEL_NAMES = Lists.newArrayList("appName");
    private final Tags tags;
    private Metric metric;

    public PrometheusSnapshotCollector(Metric metric, Tags tags) {

        this.metric = metric;
        this.tags = tags;
    }

    public void updateMetric(Metric metric) {
        this.metric = metric;
    }

    @Override
    public List<MetricFamilySamples> collect() {

        String name = getSanitizedName();
        Type metricType = metric.getType() == Metric.Type.COUNT ? Type.COUNTER : Type.GAUGE;

        List<Sample> samples = Collections.singletonList(getSample(metric, name));
        return Collections.singletonList(new MetricFamilySamples(name, metricType, "", samples));
    }

    private Sample getSample(Metric metric, String name) {

        List<String> labelNames = Lists.newArrayList(LABEL_NAMES);
        List<String> labelValues = Lists.newArrayList(tags.appName().orElse(""));

        MetricLabels labels = metric.getLabels();
        if (labels.preLabelValue().isPresent() && labels.preLabelName().isPresent()) {
            labelNames.add(labels.preLabelName().get());
            labelValues.add(labels.preLabelValue().get());
        }

        List<String> postLabelNames = labels.postLabelNames();
        List<String> postLabelValues = labels.postLabelValues();
        for (int i = 0; i < Math.min(postLabelNames.size(), postLabelValues.size()); i++) {
            labelNames.add(postLabelNames.get(i));
            labelValues.add(postLabelValues.get(i));
        }

        return new Sample(name, labelNames, labelValues, metric.get());
    }

    private String getSanitizedName() {
        return getAppName() + metric.getName().replaceAll("[ -.]", "_") + getMetricType(metric);
    }

    private String getAppName() {
        return tags.appName().map(appName -> appName + "_").orElse("");
    }

    private String getMetricType(Metric metric) {
        String type = metric.getType().getName();
        return StringUtils.isBlank(type) ? "" : "_" + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrometheusSnapshotCollector that = (PrometheusSnapshotCollector) o;
        return metric.getName().equals(that.metric.getName()) && metric.getType() == that.metric.getType();
    }

    @Override
    public int hashCode() {

        return Objects.hash(metric.getName(), metric.getType());
    }
}
