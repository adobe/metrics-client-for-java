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
package com.adobe.aam.metrics

import com.adobe.aam.metrics.core.config.ImmutablePublisherConfig
import com.adobe.aam.metrics.core.config.ImmutableRelabelConfig
import com.adobe.aam.metrics.filter.MetricFilter
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.SimpleMetric
import com.adobe.aam.metrics.metric.bucket.MetricBucketImpl
import com.adobe.aam.metrics.prometheus.PrometheusPublisher
import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import spock.lang.Shared
import spock.lang.Specification

import java.util.regex.Pattern

import static com.adobe.aam.metrics.metric.Metric.Type.COUNT

class PrometheusPublisherTest extends Specification {

    @Shared
            tags = ImmutableTags.builder().appName("myapp").regionName("us-east-1").build()

    def "test prometheus publisher"() {

        setup:
        def config = ImmutablePublisherConfig.builder()
                .host("")
                .type("prometheus")
                .name("Prometheus Publisher")
                .addMetricFilters(MetricFilter.ALLOW_ALL)
                .tags(tags)
                .relabelConfigs(
                    [ImmutableRelabelConfig.builder()
                    .regex(Pattern.compile("http\\.request\\.([^.]+).*"))
                    .putGroupToLabelName(1, "cassandra_table").build()]
                )
                .build()

        def prometheusRegistry = new CollectorRegistry()
        def prometheusPublisher = new PrometheusPublisher(config, prometheusRegistry)

        when:
        prometheusPublisher.publishMetrics(metrics)

        then:
        prometheusRegistry.metricFamilySamples().hasMoreElements()
        prometheusRegistry.metricFamilySamples().nextElement().name == "myapp_http_request_count"

        when:
        def samples = prometheusRegistry.metricFamilySamples().nextElement().samples

        then:
        samples.size() == 1
        def sample = samples.iterator().next()
        sample.value == 100
        sample.labelNames == ["appName", "status_code", "cassandra_table"]
        sample.labelValues == ["myapp", "200", "mytable"]

        where:
        metrics = [metric()]
    }

    def metric(value = 100) {
        MetricBucketImpl bucket = new MetricBucketImpl("http.request.mytable", COUNT, "status_code");
        Metric metric = bucket.getMetric("200");
        metric.track(value);
        return metric
    }
}
