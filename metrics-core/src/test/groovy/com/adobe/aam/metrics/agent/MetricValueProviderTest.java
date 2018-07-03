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

package com.adobe.aam.metrics.agent;

import com.adobe.aam.metrics.BufferedMetricClient;
import com.adobe.aam.metrics.agent.MetricAgent;
import com.adobe.aam.metrics.agent.MetricAgentConfig;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.Tags;
import com.adobe.aam.metrics.metric.bucket.MetricBucketImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricValueProviderTest {

    private static final double DELTA = 0;
    private MockedMetricClient mockedMetricClient;

    @Before
    public void setUp() {
        this.mockedMetricClient = new MockedMetricClient();
    }

    @Test
    public void testMetricValueProvider() throws Exception {
        // Given a metric and a metric provider
        // When the provider object returns a value
        // Then the metric agent should report this metric value to the backend

        Metric metric = Metric.newInstance("latency", Metric.Type.AVG);
        Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
        MetricAgentConfig config = ImmutableMetricAgentConfig.builder()
                .putMetricValueProviders(metric, () -> Optional.of(123.00))
                .sendOnlyRecentlyUpdatedMetrics(false)
                .build();
        MetricAgent metricAgent = new MetricAgent(mockedMetricClient, config);

        metricAgent.runOneIteration();

        Assert.assertEquals("Metric filter did not overwrite the metric value.",
                123.00, mockedMetricClient.metricValue, DELTA);
    }

    @Test
    public void testMetricValueProviderNoUpdates() throws Exception {
        // Given a metric and a value provider
        // When the provider does not provide any value
        // Then the metric agent should not send the metric to the backend

        Metric metric = Metric.newInstance("latency", Metric.Type.AVG);
        Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);

        MetricAgentConfig config = ImmutableMetricAgentConfig.builder()
                .putMetricValueProviders(metric, Optional::empty)
                .sendOnlyRecentlyUpdatedMetrics(false)
                .build();
        MetricAgent metricAgent = new MetricAgent(mockedMetricClient, config);

        metricAgent.runOneIteration();

        Assert.assertEquals("Metric agent sent the metric to the backend despite it having no value provided.",
                null, mockedMetricClient.metricName);
    }

    @Test
    public void testMonitoringDuplicatedMetric() throws Exception {
        // Given a metric that reaches a MonitorAgent from several sources
        // When the agent sends the metrics to the backend
        // Then it should not duplicate the same metric

        MetricBucketImpl bucket = new MetricBucketImpl("partner", Metric.Type.COUNT);
        Metric metric = bucket.getMetricWithPrefix("adobe");

        int frequency = 100;

        // Pass the same metric twice to the agent.

        MetricAgentConfig config = ImmutableMetricAgentConfig.builder()
                .addMetrics(metric) // #1
                .addMetricBuckets(bucket) // #2
                .sendOnlyRecentlyUpdatedMetrics(false)
                .collectFrequency(Duration.ofMillis(frequency))
                .sendOnlyRecentlyUpdatedMetrics(false)
                .build();
        MetricAgent metricAgent = new MetricAgent(mockedMetricClient, config);

        metricAgent.runOneIteration();
        Assert.assertEquals("Expected one metric to be sent.", 1, mockedMetricClient.metricsSent.getAndSet(0));

        Thread.sleep(frequency + 1);

        metricAgent.runOneIteration();
        Assert.assertEquals("Expected one metric to be sent.", 1, mockedMetricClient.metricsSent.get());

    }

    private class MockedMetricClient implements BufferedMetricClient {

        public String metricName;
        public String metricType;
        public double metricValue;
        public long timestamp;

        public final AtomicInteger metricsSent = new AtomicInteger();

        @Override
        public Tags getTags() {
            return null;
        }

        public void sendAndReset(Metric metric) {
            this.sendAndReset(metric, System.currentTimeMillis());
        }

        @Override
        public void sendAndReset(Metric metric, long timestamp) {
            this.send(
                    metric.getName(),
                    metric.getType().getName(),
                    metric.getAndReset(),
                    timestamp);
        }

        @Override
        public void send(Metric metric, long timestamp) {
            this.send(
                    metric.getName(),
                    metric.getType().getName(),
                    metric.get(),
                    timestamp);
        }

        public void send(String metricName, String metricType, double metricValue, long timestamp) {
            this.metricName = metricName;
            this.metricType = metricType;
            this.metricValue = metricValue;
            this.timestamp = timestamp;

            metricsSent.incrementAndGet();
        }

        @Override
        public void flush() {

        }

        @Override
        public void shutdown() {

        }
    }
}
