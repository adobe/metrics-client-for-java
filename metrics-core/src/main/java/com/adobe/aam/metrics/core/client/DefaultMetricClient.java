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

package com.adobe.aam.metrics.core.client;

import com.adobe.aam.metrics.BufferedMetricClient;
import com.adobe.aam.metrics.metric.Tags;
import com.adobe.aam.metrics.core.ImmutableMetricSnapshot;
import com.adobe.aam.metrics.core.MetricSnapshot;
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.metric.Metric;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultMetricClient implements BufferedMetricClient {

    private static Logger logger = LoggerFactory.getLogger(DefaultMetricClient.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Queue<MetricSnapshot> queue;
    private final Collection<Publisher> metricPublishers;
    private Tags tags;

    public DefaultMetricClient(Queue<MetricSnapshot> queue,
                               Collection<Publisher> metricPublishers,
                               Tags tags) {
        this.queue = queue;
        this.metricPublishers = metricPublishers;
        this.tags = tags;
    }

    public DefaultMetricClient(Queue<MetricSnapshot> queue,
                               Publisher metricPublisher,
                               Tags tags) {
        this(queue, ImmutableList.of(metricPublisher), tags);
    }

    public Collection<Publisher> getPublishers() {
        return metricPublishers;
    }

    @Override
    public Tags getTags() {
        return tags;
    }

    @Override
    public void sendAndReset(Metric metric, long timestamp) {
        queue.add(snapshotAndReset(metric, timestamp));
    }

    @Override
    public void send(Metric metric, long timestamp) {
        queue.add(snapshot(metric, timestamp));
    }

    private MetricSnapshot snapshotAndReset(Metric metric, long timestamp) {
        return snapshot(metric.getName(), metric.getType(), metric.getAndReset(), timestamp);
    }

    private MetricSnapshot snapshot(Metric metric, long timestamp) {
        return snapshot(metric.getName(), metric.getType(), metric.get(), timestamp);
    }

    private MetricSnapshot snapshot(String name, Metric.Type type, double value, long timestamp) {
        return ImmutableMetricSnapshot.builder()
                .name(name)
                .type(type)
                .timestamp(timestamp)
                .value(value)
                .tags(getTags())
                .build();
    }

    @Override
    public synchronized void flush() {
        while (!queue.isEmpty() && !Thread.currentThread().isInterrupted()) {
            List<MetricSnapshot> metrics = takeAll();
            if (!metrics.isEmpty()) {

                metricPublishers.forEach(publisher -> submitToExecutor(publisher, metrics));
            }
        }
    }

    @Override
    public void shutdown() {
        getPublishers().forEach(Publisher::shutdown);
        executor.shutdown();
    }

    private List<MetricSnapshot> takeAll() {
        List<MetricSnapshot> result = Lists.newArrayList();
        while (!queue.isEmpty()) {
            result.add(queue.poll());
        }

        return result;
    }

    private void submitToExecutor(Publisher publisher, Collection<MetricSnapshot> metrics) {

        List<MetricSnapshot> updatedMetrics = metrics
                .stream()
                .filter(publisher::isWhitelisted)
                .collect(Collectors.toList());

        Iterables.partition(updatedMetrics, nonEmptyBatchSize(publisher.getBatchSize()))
                .forEach(partitionedMetrics ->
                        executor.submit(() -> {
                            try {
                                publisher.publishMetrics(partitionedMetrics);
                            } catch (IOException e) {
                                logger.error("Failed to publish.", e);
                            }
                        }));
    }

    private int nonEmptyBatchSize(int batchSize) {
        return batchSize <= 0 ? 500 : batchSize;
    }
}
