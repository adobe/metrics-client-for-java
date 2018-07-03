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
import com.adobe.aam.metrics.core.MetricRegistryReporter;
import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.SimpleMetric;
import com.adobe.aam.metrics.metric.bucket.MetricBucket;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The MetricAgent periodically reports the metric values to the provided metric client.
 */
public class MetricAgent extends AbstractScheduledService {

	private final static Logger logger = LoggerFactory.getLogger(MetricAgent.class);

	private final BufferedMetricClient metricClient;
	private final Collection<Metric> metrics;
	private final Collection<MetricBucket> metricBuckets;
	private final Collection<MetricRegistryReporter> codahaleMetricRegistryReporters;
	private final Duration collectFrequency;
	private final boolean sendOnlyRecentlyUpdated;
	private final Map<Metric, ValueProvider> metricValueProviders;

	public MetricAgent(BufferedMetricClient metricClient, MetricAgentConfig config) {
		this.metricClient = metricClient;
		this.collectFrequency = config.getCollectFrequency();
		this.sendOnlyRecentlyUpdated = config.sendOnlyRecentlyUpdatedMetrics();
		this.metrics = config.getMetrics();
		this.metricBuckets = config.getMetricBuckets();
		this.codahaleMetricRegistryReporters = config.getMetricRegistries();
		this.metricValueProviders = config.getMetricValueProviders();
	}

	@Override
	protected void startUp() throws Exception {
		super.startUp();
		logger.info("Starting metric agent.");
	}

	@Override
	protected void shutDown() throws Exception {
		logger.info("Stopping metric agent.");
		metricClient.shutdown();
		super.shutDown();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(
				0,
				collectFrequency.toMillis(),
				TimeUnit.MILLISECONDS
		);
	}

	@Override
	protected void runOneIteration() {

		ImmutableSet.Builder<Metric> allMetrics = ImmutableSet.<Metric>builder()
				.addAll(metrics);
		metricBuckets
				.forEach(bucket -> allMetrics.addAll(bucket.getMetrics()));

		reportMetrics(Sets.union(allMetrics.build(), metricValueProviders.keySet()));
		reportMetricRegistries();
		metricClient.flush();
	}

	private void reportMetrics(Collection<Metric> metrics) {
		long timestamp = System.currentTimeMillis() / 1000;

		for (Metric metric : metrics) {
			boolean hasValueProvider = metricValueProviders.containsKey(metric);
			if (hasValueProvider) {
				reportMetricWithValueProvider(metric, metricValueProviders.get(metric), timestamp);
			} else {
				reportMetric(metric, timestamp);
			}
		}
	}

	private void reportMetricWithValueProvider(Metric metric, ValueProvider valueProvider, long timestamp) {
		Optional<Double> value = valueProvider.getValue();

		if (!value.isPresent()) {
			logger.trace("Metric does not have a value. Not sending over the network. {}", metric);
		}

		value.map(val -> new SimpleMetric(metric.getName(), metric.getType(), val))
				.ifPresent(updatedMetric -> metricClient.send(updatedMetric, timestamp));
	}

	private void reportMetric(Metric metric, long timestamp) {
		if (sendOnlyRecentlyUpdated) {
			reportRecentlyUpdatedMetrics(metric, timestamp);
		} else {
			metricClient.sendAndReset(metric, timestamp);
		}
	}

	private void reportRecentlyUpdatedMetrics(Metric metric, long timestamp) {
		boolean wasUpdatedRecently = System.currentTimeMillis() - metric.getLastUpdateTime() <= collectFrequency.toMillis();

		if (wasUpdatedRecently) {
			metricClient.sendAndReset(metric, timestamp);;
		} else {
			logger.trace("Metric was not updated recently. Not sending over the network. {}", metric);
		}
	}

	private void reportMetricRegistries() {

		codahaleMetricRegistryReporters.forEach(metricRegistryReporter -> metricRegistryReporter.reportTo(metricClient));
	}
}
