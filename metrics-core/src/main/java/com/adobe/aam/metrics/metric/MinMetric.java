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

public class MinMetric extends Metric {
	private AtomicDouble min = new AtomicDouble(Metric.POSITIVE_INFINITY);

	public MinMetric(MetricLabels labels) {
		super(labels);
	}

	@Override
	public Type getType() {
		return Type.MIN;
	}

	@Override
	public synchronized void doTrack(double value) {
		if (value < min.get()) {
			min.set(value);
		}
	}

	@Override
	public double doGetAndReset() {
		double value = min.getAndSet(Metric.POSITIVE_INFINITY);
		return value == POSITIVE_INFINITY ? 0 : value;
	}

	@Override
	public double get() {
		return min.get();
	}
}
