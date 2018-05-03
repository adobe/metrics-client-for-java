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

import java.util.concurrent.atomic.DoubleAdder;

public class CounterMetric extends Metric {

	private final DoubleAdder sum = new DoubleAdder();

	public CounterMetric(String name) {
		super(name);
	}

	@Override
	public void doTrack(double value) {
		sum.add(value);
	}

	@Override
	public double getAndReset() {
		return sum.sumThenReset();
	}

	@Override
	public double get() {
		return sum.sum();
	}

	public Type getType() {
		return Type.COUNT;
	}

	/**
	 * Adds the specified delta to the COUNTER metric.
	 *
	 * @param delta the desired value.
	 */
	public void add(long delta) {
		track(delta);
	}

	/**
	 * Increments the COUNTER metric.
	 */
	public void increment() {
		add(1);
	}
}
