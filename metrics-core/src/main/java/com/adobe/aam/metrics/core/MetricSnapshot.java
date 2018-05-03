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

package com.adobe.aam.metrics.core;

import com.adobe.aam.metrics.metric.Tags;
import com.adobe.aam.metrics.metric.Metric;
import org.immutables.value.Value;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public interface MetricSnapshot {

    String name();
    Metric.Type type();
    long timestamp();
    double value();
    Tags tags();

}
