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
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.metric.Metric;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class DefaultMetricClient implements BufferedMetricClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMetricClient.class);
    private final ExecutorService executor;
    private final Queue<Metric> queue;
    private final Collection<Publisher> metricPublishers;

    public DefaultMetricClient(Queue<Metric> queue,
                               Collection<Publisher> metricPublishers,
                               ExecutorService executorService) {
        this.queue = queue;
        this.metricPublishers = metricPublishers;
        this.executor = executorService;
    }

    public DefaultMetricClient(Queue<Metric> queue,
                               Collection<Publisher> metricPublishers) {
        this(
                queue,
                metricPublishers,
                MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newCachedThreadPool())
        );
    }

    public DefaultMetricClient(Queue<Metric> queue,
                               Publisher metricPublisher) {
        this(queue, ImmutableList.of(metricPublisher));
    }

    public Collection<Publisher> getPublishers() {
        return metricPublishers;
    }

    @Override
    public void send(Metric metric) {
        queue.add(metric);
    }

    @Override
    public void send(Collection<Metric> metrics) {
        queue.addAll(metrics);
    }

    @Override
    public synchronized void flush() {
        while (!queue.isEmpty() && !Thread.currentThread().isInterrupted()) {
            List<Metric> metrics = takeAll();
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

    private List<Metric> takeAll() {
        List<Metric> result = Lists.newArrayList();
        while (!queue.isEmpty()) {
            result.add(queue.poll());
        }

        return result;
    }

    private void submitToExecutor(Publisher publisher, Collection<Metric> metrics) {

        executor.submit(() -> {
            try {
                publisher.publishMetrics(getFilteredMetrics(publisher, metrics));
            } catch (IOException e) {
                logger.error("Failed to publish.", e);
            }
        });
    }

    private Collection<Metric> getFilteredMetrics(Publisher publisher, Collection<Metric> metrics) {
        return metrics.stream()
                .filter(publisher::isAllowed)
                .collect(Collectors.toSet());
    }
}
