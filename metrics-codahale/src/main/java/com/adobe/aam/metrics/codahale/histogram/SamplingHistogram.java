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

package com.adobe.aam.metrics.codahale.histogram;

import com.codahale.metrics.*;

import java.util.concurrent.ThreadLocalRandom;

public class SamplingHistogram implements Metric, Sampling, Counting {
    private final Histogram histogram;
    private final double samplingRatio;

    /**
     * A histogram which will update based on a samplingRatio. E.g. When calling the update method on a SamplingHistogram
     * with a samplingRatio of 0.1, there's 10% chance that it will update its contents.
     * @param histogram the decorated histogram
     * @param samplingRatio the ratio which will be used to decide when updates are triggered
     */
    public SamplingHistogram(Histogram histogram, double samplingRatio) {
        this.histogram = histogram;
        this.samplingRatio = samplingRatio;
    }

    public void update(long value) {
        double triggerChance = ThreadLocalRandom.current().nextDouble();

        if (triggerChance < samplingRatio) {
            histogram.update(value);
        }
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }
}
