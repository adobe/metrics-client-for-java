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

package com.adobe.aam.metrics.metric;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.List;

public class SimpleMetric extends Metric {

	private final Type type;
	private final AtomicDouble value;

	public SimpleMetric(String metricName, Type type) {
		this(MetricLabels.of(metricName), type);
	}

	public SimpleMetric(MetricLabels labels, Type type) {
		this(labels, type, 0);
	}

	public SimpleMetric(String metricName, Type type, double value) {
		this(MetricLabels.of(metricName), type, value);
	}

	public SimpleMetric(MetricLabels labels, Type type, double value) {
		this(labels, type, value, System.currentTimeMillis());
	}



	public SimpleMetric(String metricName, Type type, double value, long lastTrack) {
		this(MetricLabels.of(metricName), type, value, lastTrack);
	}

	public SimpleMetric(MetricLabels labels, Type type, double value, long lastTrack) {
		super(labels);
		this.type = type;
		this.value = new AtomicDouble(value);
		super.lastTrack = lastTrack;
	}

	@Override
	public void doTrack(double value) {
		this.value.set(value);
	}

	@Override
	public double doGetAndReset() {
		return value.getAndSet(0);
	}

	@Override
	public double get() {
		return value.get();
	}

	public Type getType() {
		return type;
	}
}
