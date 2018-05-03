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
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

public class MetricBucketImpl implements MetricBucket {

	private final ConcurrentMap<String, Metric> bucket = Maps.newConcurrentMap();
	private final Metric.Type type;
	private final String name;

	public MetricBucketImpl(String name, Metric.Type type) {
		this.name = name;
		this.type = type;
	}

	private Metric safeGetMetric(String identifier) {
		Metric metric = bucket.get(identifier);
		if (metric == null) {
			// Metric was not yet created for this identifier.
			Metric newMetric = Metric.newInstance(identifier, type);
			metric = bucket.putIfAbsent(identifier, newMetric);
			if (metric == null) {
				// If this is the lucky thread that managed to put the metric, then return it as is.
				metric = newMetric;
			}
		}

		return metric;
	}

	@Override
	public Collection<Metric> getMetrics() {
		return bucket.values();
	}

	@Override
	public Metric getMetric(Object suffix) {
		return safeGetMetric(prettyName(name, suffix));
	}

	public Metric getMetricWithPrefix(Object prefix) {
		return safeGetMetric(prettyName(prefix, name));
	}

	public Metric getMetric(Object prefix, Object... suffixes) {
		return safeGetMetric(prettyName(prefix, name, suffixes));
	}

	@Override
	public Metric getParentMetric() {
		return safeGetMetric(name);
	}

	private String prettyName(Object prefix, Object middle, Object... suffixes) {
		StringBuilder builder = new StringBuilder();
		builder
				.append(prefix)
				.append('.')
				.append(middle);

		for (Object suffix : suffixes) {
			builder
					.append('.')
					.append(suffix);
		}

		return builder.toString();
	}
}
