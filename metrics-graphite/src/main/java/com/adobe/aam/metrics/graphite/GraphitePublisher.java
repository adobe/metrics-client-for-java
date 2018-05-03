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

package com.adobe.aam.metrics.graphite;

import com.adobe.aam.metrics.core.MetricSnapshot;
import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.filter.MetricFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class GraphitePublisher implements Publisher {

    private static final Logger logger = LoggerFactory.getLogger(GraphitePublisher.class);
    private final PublisherConfig config;
    private final SocketWriterFactory socketFactory;

    public GraphitePublisher(PublisherConfig config, SocketWriterFactory socketFactory) {
        this.config = config;
        this.socketFactory = socketFactory;
    }

    public GraphitePublisher(PublisherConfig config) {
        this.config = config;
        this.socketFactory = new SocketWriterFactory();
    }

    @Override
    public int getBatchSize() {
        return config.batchSize();
    }

    @Override
    public List<MetricFilter> getMetricFilters() {
        return config.metricFilters();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void publishMetrics(final List<MetricSnapshot> metrics) throws IOException {

        try {
            send(metrics);
        } catch (IOException e) {
            logger.error("Error sending metrics to '{}'. {}", config.name(), e.getMessage());
            throw e;
        }
    }

    private void send(List<MetricSnapshot> metrics) throws IOException {
        logger.info("Sending {} metrics to '{}': {}", metrics.size(), config.name(), config.host());

        SocketWriter writer = socketFactory.create();
        writer.open(config.host(), config.port().orElse(2003), config.socketTimeout());

        for (MetricSnapshot metric : metrics) {
            writer.write(format(metric));
            logger.info("Metric sent from '{}': {}", config.name(), metric);
        }

        writer.close();
    }

    private String format(MetricSnapshot metric) {
        // Generate full metric name (eg. prod.myapp.cluster1.us-east-1.requests.attempted.count)
        StringBuilder fullMetric = new StringBuilder();
        fullMetric
                .append(metric.tags().asMetricName())
                .append(".")
                .append(metric.name());

        String metricName = metric.type().getName();
        double metricValue = metric.value();
        if (StringUtils.isNotBlank(metricName)) {
            fullMetric
                    .append(".")
                    .append(metricName);
        }

        return String.format("%s %2.2f %d", fullMetric, metricValue, metric.timestamp());
    }
}
