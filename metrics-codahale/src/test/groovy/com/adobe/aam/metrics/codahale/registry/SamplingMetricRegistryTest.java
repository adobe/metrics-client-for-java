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

package com.adobe.aam.metrics.codahale.registry;

import com.adobe.aam.metrics.codahale.histogram.SamplingHistogram;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ThreadLocalRandom;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SamplingHistogram.class})
public class SamplingMetricRegistryTest {

    @Test
    public void testNoUpdate() {
        mockStatic(ThreadLocalRandom.class);
        SamplingMetricRegistry registry = new SamplingMetricRegistry(0);
        SamplingHistogram histogram = registry.samplingHistogram("histogram1");
        when(ThreadLocalRandom.current().nextDouble()).thenReturn(1.0);
        histogram.update(1);
        Assert.assertEquals(0, histogram.getCount());
    }

    @Test
    public void testSureUpdate() {
        mockStatic(ThreadLocalRandom.class);
        SamplingMetricRegistry registry = new SamplingMetricRegistry(1);
        SamplingHistogram histogram = registry.samplingHistogram("histogram1");
        when(ThreadLocalRandom.current().nextDouble()).thenReturn(0.0);
        histogram.update(1);
        Assert.assertEquals(1, histogram.getCount());
    }

    @Test
    public void testHistogramRegistered() {
        SamplingMetricRegistry registry = new SamplingMetricRegistry(1);
        registry.samplingHistogram("histogram1");
        Assert.assertNotNull(registry.getHistograms().get("histogram1"));
    }
}
