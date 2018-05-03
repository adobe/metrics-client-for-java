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

import com.adobe.aam.metrics.metric.CounterMetric;
import com.adobe.aam.metrics.metric.Metric;
import org.junit.Assert;
import org.junit.Test;

public class MetricTest {
	private static final double DELTA = 0;

    @Test
    public void testAvg() {
        Metric metric = Metric.newInstance("latency", Metric.Type.AVG);
        Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
        metric.track(100);
        metric.track(200);
        metric.track(300);
        Assert.assertEquals("Metric average is not valid.", 200, metric.getAndReset(), DELTA);
    }

	@Test
	public void testAvgRounded() {
		Metric metric = Metric.newInstance("latency", Metric.Type.AVG);
		Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
		metric.track(100);
		metric.track(101);
		Assert.assertEquals("Metric average is not valid.", 100.5, metric.getAndReset(), DELTA);
	}

    @Test
    public void testAvgZero() {
        Metric metric = Metric.newInstance("latency", Metric.Type.AVG);
        metric.track(0);
        Assert.assertEquals("Metric average is not valid.", 0, metric.getAndReset(), DELTA);
    }

    @Test
    public void testCountIncrement() {
        CounterMetric metric = (CounterMetric)Metric.newInstance("requests", Metric.Type.COUNT);
        metric.increment();
        metric.increment();
        metric.increment();
        Assert.assertEquals("Metric count is not valid.", 3, metric.getAndReset(), DELTA);
    }

	@Test
	public void testCountTrack() {
		CounterMetric metric = (CounterMetric)Metric.newInstance("requests", Metric.Type.COUNT);
		metric.increment();
		metric.track(10);
		metric.increment();
		Assert.assertEquals("Metric count is not valid.", 12, metric.getAndReset(), DELTA);
	}

    @Test
    public void testAddition() {
	    CounterMetric metric = (CounterMetric)Metric.newInstance("requests", Metric.Type.COUNT);
        Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
        metric.add(100);
        metric.add(200);
        metric.add(300);
        Assert.assertEquals("Metric count is not valid.", 600, metric.getAndReset(), DELTA);
    }

    @Test
    public void testMin() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MIN);
        Assert.assertEquals("Initial value is not the one expected.", Metric.POSITIVE_INFINITY, metric.get(), DELTA);
        metric.track(100);
        metric.track(-200);
        metric.track(0);
        Assert.assertEquals("Metric min is not valid.", -200, metric.getAndReset(), DELTA);
    }

    @Test
    public void testMinZero() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MIN);
        metric.track(300);
		metric.track(0);
		metric.track(100);
        metric.track(1000);
        Assert.assertEquals("Metric min 0 valid.", 0, metric.getAndReset(), DELTA);
    }

    @Test
    public void testMinValue() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MIN);
        metric.track(100);
		metric.track(Metric.NEGATIVE_INFINITY);
		metric.track(-200);
        Assert.assertEquals("Metric min is not valid.", Metric.NEGATIVE_INFINITY, metric.getAndReset(), DELTA);

    }

    @Test
    public void testMax() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        Assert.assertEquals("Initial value is not the one expected.", Metric.NEGATIVE_INFINITY, metric.get(), DELTA);
        metric.track(-200);
        metric.track(100);
        metric.track(0);
        Assert.assertEquals("Metric max is not valid.", 100, metric.getAndReset(), DELTA);
    }

    @Test
    public void testMaxValue() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        metric.track(100);
		metric.track(Metric.POSITIVE_INFINITY);
		metric.track(-200);
        Assert.assertEquals("Metric max is not valid.", Metric.POSITIVE_INFINITY, metric.getAndReset(), DELTA);
    }

    @Test
    public void testGetAndResetMax() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        Assert.assertEquals("Initial value is not the one expected.", Metric.NEGATIVE_INFINITY, metric.get(), DELTA);
        metric.track(100);
        Assert.assertEquals("Max value is not the one expected.", 100, metric.getAndReset(), DELTA);
        Assert.assertEquals("Reset did not work properly.", Metric.NEGATIVE_INFINITY, metric.get(), DELTA);
    }

	@Test
	public void testGetAndResetMin() {
		Metric metric;
		metric = Metric.newInstance("requests", Metric.Type.MIN);
		Assert.assertEquals("Initial value is not the one expected.", Metric.POSITIVE_INFINITY, metric.get(), DELTA);
		metric.track(-100.2);
		Assert.assertEquals("Min value is not the one expected.", -100.2, metric.getAndReset(), DELTA);
		Assert.assertEquals("Reset did not work properly.", Metric.POSITIVE_INFINITY, metric.get(), DELTA);
	}

	@Test
	public void testGetAndResetCount() {
		Metric metric;
		metric = Metric.newInstance("requests", Metric.Type.COUNT);
		Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
		metric.track(100);
		Assert.assertEquals("Max value is not the one expected.", 100, metric.getAndReset(), DELTA);
		Assert.assertEquals("Reset did not work properly.", 0, metric.get(), DELTA);
	}

	@Test
	public void testGetAndResetAverage() {
		Metric metric;
		metric = Metric.newInstance("requests", Metric.Type.AVG);
		Assert.assertEquals("Initial value is not the one expected.", 0, metric.get(), DELTA);
		metric.track(100);
		metric.track(1);
		Assert.assertEquals("Average value is not the one expected.", 50.5, metric.getAndReset(), DELTA);
		Assert.assertEquals("Reset did not work properly.", 0, metric.get(), DELTA);
	}

    @Test
    public void testGetValue() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        Assert.assertEquals("Initial value is not the one expected.", Metric.NEGATIVE_INFINITY, metric.get(), DELTA);
        metric.track(100);
        Assert.assertEquals("Get did not work properly.", 100, metric.get(), DELTA);
        metric.track(200);
        Assert.assertEquals("Get did not work properly.", 200, metric.get(), DELTA);
        metric.track(-200);
        Assert.assertEquals("Get did not work properly.", 200, metric.get(), DELTA);
    }

    @Test
    public void testGetName() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        Assert.assertEquals("The returned metric name is not valid.", "requests", metric.getName());
    }

    @Test
    public void testGetType() {
        Metric metric = Metric.newInstance("requests", Metric.Type.MAX);
        Assert.assertEquals("The returned name is not valid.", Metric.Type.MAX, metric.getType());
    }
}
