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

import com.adobe.aam.metrics.metric.Metric;
import com.adobe.aam.metrics.metric.bucket.MetricBucket;
import com.adobe.aam.metrics.metric.bucket.MetricBucketImpl;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Represents a bucket of related metrics which can be accessed concurrently.
 *
 * These tests will overlap with the getName and getType Metric tests
 */
public class MetricBucketTest {

    @Test
    public void testGetMetricsCount() {
        Metric tempMetric;
		// check initial state
		MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.AVG);
        Assert.assertEquals("Initial bucket state is not valid.", 0, bucket.getMetrics().size());

		// check first metric added
		bucket.getMetric("succeeded");
        Assert.assertEquals("Bucket state is not valid.", 1, bucket.getMetrics().size());
		tempMetric = (Metric)(bucket.getMetrics().toArray()[0]);
		Assert.assertEquals("Metric type is not as expected.", Metric.Type.AVG, tempMetric.getType());
		Assert.assertEquals("Initial bucket state is not valid.", "requests.succeeded", tempMetric.getName());

		// check identical metric added
		bucket.getMetric("succeeded");
        Assert.assertEquals("Bucket state is not valid.", 1, bucket.getMetrics().size());
		Assert.assertEquals("Metric type is not as expected.", Metric.Type.AVG, tempMetric.getType());
		tempMetric = (Metric)(bucket.getMetrics().toArray()[0]);
		Assert.assertEquals("Metric type is not as expected.", Metric.Type.AVG, tempMetric.getType());
		Assert.assertEquals("Initial bucket state is not valid.", "requests.succeeded", tempMetric.getName());

		// check other metric added
		bucket.getMetric("failed");
        Assert.assertEquals("Bucket state state is not valid.", 2, bucket.getMetrics().size());
	}

    @Test
    public void testGetMetricsName() {
	    MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.AVG);
        bucket.getMetric("succeeded");
        Assert.assertEquals("Returned metric name is not valid.", "requests.succeeded", bucket.getMetrics().iterator().next().getName());
    }

	@Test
	public void testGetMetricsType() {
		MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.MAX);
		bucket.getMetric("succeeded");
		Assert.assertEquals("Returned metric type is not valid.", Metric.Type.MAX, bucket.getMetrics().iterator().next().getType());
	}

    @Test
    public void testGetMetricAndTrack() {
	    MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.MIN);
        bucket.getMetric("succeeded").track(-150);
	    Assert.assertEquals("Returned metric value is invalid.", -150, (long) bucket.getMetric("succeeded").get());
	    Assert.assertEquals("Returned metric value is invalid.", -150, (long) bucket.getMetrics().iterator().next().get());
    }

	@Test
	public void testGetFromMultipleThreads() throws InterruptedException {
		final MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.COUNT);
		final String identifier = "adobe.succeeded";
		int nrThreads = 100;

		// Test that different threads work with the same metric instance.
		List<WorkerThread> threads = Lists.newArrayList();
		for (int i = 0; i < nrThreads; i++) {
			WorkerThread thread = new WorkerThread(bucket, identifier);
			threads.add(thread);
			thread.start();
		}

		for (WorkerThread thread : threads) {
			thread.join();
		}

		for (int i = 1; i < nrThreads; i++) {
			Assert.assertEquals(threads.get(0).getMetricInstance(), threads.get(i).getMetricInstance());
		}
	}

	@Test
	public void testMetricNameWithPrefix() {
		MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.MIN);
		Metric metric = bucket.getMetricWithPrefix("prefix");
		Assert.assertEquals("prefix.requests", metric.getName());
	}

	@Test
	public void testMetricNameWithSuffix() {
		MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.MIN);
		Metric metric = bucket.getMetric("suffix1");
		Assert.assertEquals("requests.suffix1", metric.getName());
	}

	@Test
	public void testMetricNameWithPrefixAndSuffixes() {
		MetricBucketImpl bucket = new MetricBucketImpl("requests", Metric.Type.MIN);
		Metric metric = bucket.getMetric("prefix", "suffix1", "suffix2");
		Assert.assertEquals("prefix.requests.suffix1.suffix2", metric.getName());
	}

	class WorkerThread extends Thread {

		private final MetricBucket bucket;
		private final String identifier;
		private Metric instance;

		public WorkerThread(MetricBucket bucket, String identifier) {
			this.bucket = bucket;
			this.identifier = identifier;
		}

		@Override
		public void run() {
			instance = bucket.getMetric(identifier);
		}

		public Metric getMetricInstance() {
			return instance;
		}
	}
}
