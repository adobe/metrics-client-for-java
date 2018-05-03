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

import com.google.common.base.Optional;

/**
 * A metric is a time series collection of name/value data, used to generate graphs in a dashboard.
 */
public abstract class Metric {

	public static final double POSITIVE_INFINITY = Long.MAX_VALUE;
	public static final double NEGATIVE_INFINITY = Long.MIN_VALUE;
	private final String name;
	private long lastUpdate;

	public Metric(String name) {
		this.name = name;
	}

	public enum Type {
		AVG("avg"),
		MIN("min"),
		MAX("max"),
		COUNT("count"),
		STANDARD_DEVIATION("stddev"),
		PERCENTILE_50("p", 50),
		PERCENTILE_75("p", 75),
		PERCENTILE_95("p", 95),
		PERCENTILE_98("p", 98),
		PERCENTILE_99("p", 99),
		PERCENTILE_999("p", 999),
		RATE_1MIN("rate_", 1),
		RATE_5MIN("rate_", 5),
		RATE_15MIN("rate_", 15),
		MEAN_RATE("mean_rate"),
		GAUGE("");

		private final String name;
		private final Optional<Integer> value;

		private Type(String name, int value) {
			this.name = name;
			this.value = Optional.of(value);
		}

		private Type(String name) {
			this.name = name;
			this.value = Optional.absent();
		}

		public String getName() {
			return value.isPresent() ? name + value.get() : name;
		}

		public static Type fromName(String name) {
			if (name.toLowerCase().contains("mean")) {
				return Metric.Type.AVG;
			}

			if (name.toLowerCase().contains("max")) {
				return Metric.Type.MAX;
			}

			if (name.toLowerCase().contains("count")) {
				return Metric.Type.COUNT;
			}

			return Metric.Type.GAUGE;
		}
	}


	/**
	 * @param name         the metric name (eg. requests)
	 * @param type         the metric type (eg. count)
	 * @param initialValue the initial metric value
	 * @return a new metric instance
	 */
	public static Metric newInstance(String name, Type type, double initialValue) {
		Metric metric = newInstance(name, type);
		metric.track(initialValue);
		return metric;
	}

	/**
	 * @param name the metric name (eg. requests)
	 * @param type the metric type (eg. count)
	 * @return a new metric instance
	 */
	public static Metric newInstance(String name, Type type) {
		switch (type) {
			case COUNT:
				return new CounterMetric(name);
			case MIN:
				return new MinMetric(name);
			case MAX:
				return new MaxMetric(name);
			case AVG:
				return new AverageMetric(name);
			default:
				return new SimpleMetric(name, type);
		}
	}

	/**
	 * @return the metric type, such as counter, average, min, max
	 */
	public abstract Type getType();

	/**
	 * Track specified value. This depends on the metric type. - For MIN/MAX metrics, the specified value will replace
	 * the metric value only if it's lower/higher than the current min/max. - For COUNT, the value will be added to the
	 * current value. - For AVG, the value will be used when calculating the average. The get() method will return the
	 * average.
	 *
	 * @param value the desired value to be tracked in this metric
	 */
	public void track(double value) {
		lastUpdate = System.currentTimeMillis();
		doTrack(value);
	}

	protected abstract void doTrack(double value);

	/**
	 * @return the time when this metric was last updated
	 */
	public long getLastUpdateTime() {
		return lastUpdate;
	}

	/**
	 * @return the metric value and resets it atomically
	 */
	public abstract double getAndReset();

	/**
	 * @return the metric value
	 */
	public abstract double get();

	public String getName() {
		return name;
	}

	public String toString() {
		return "Metric { name: " + getName() + ", type: " + getType() + ", value: " + get() + "}";
	}
}
