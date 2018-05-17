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

import com.adobe.aam.metrics.metric.Metric;

import java.util.Collection;
/**
 * Represents a bucket of related metrics which can be accessed concurrently.
 * It is similar to a tree:
 * requests
 * requests.succeeded
 * requests.failed
 */
public interface MetricBucket {

	/**
	 * @return all the metrics in the bucket
	 */
	Collection<Metric> getMetrics();

	/**
	 * Finds the metric corresponding to the child and returns it.
	 * If none exists, one will be created and returned.
	 *
	 * For example: requests.succeeded, where "succeeded" is the child.
	 *
	 * @param child the sub-metric identifier
	 */
	Metric getMetric(Object child);

	/**
	 * @return the parent metric (eg. requests)
	 */
	Metric getParentMetric();
}
