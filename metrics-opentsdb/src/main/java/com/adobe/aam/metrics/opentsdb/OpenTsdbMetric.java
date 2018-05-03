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

package com.adobe.aam.metrics.opentsdb;

import com.adobe.aam.metrics.metric.Tags;
import com.adobe.aam.metrics.core.MetricSnapshot;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;

@Value.Style(stagedBuilder = true)
@Value.Immutable
@JsonSerialize(as = ImmutableOpenTsdbMetric.class)
@JsonDeserialize(as = ImmutableOpenTsdbMetric.class)
public interface OpenTsdbMetric {
    String metric();
    double value();
    long timestamp();
    Map<String, String> tags();

    static OpenTsdbMetric from(MetricSnapshot metricSnapshot) {

        Map<String, String> tagsMap = new HashMap<>();
        Tags tags = metricSnapshot.tags();

        tags.environment().map(value -> tagsMap.put("env", value));
        tags.appName().map(value -> tagsMap.put("appName", value));
        tags.regionName().map(value -> tagsMap.put("region", value));
        tags.clusterName().map(value -> tagsMap.put("cluster", value));
        tags.hostname().map(value -> tagsMap.put("hostname", value));


        return ImmutableOpenTsdbMetric.builder()
                .metric(metricSnapshot.name() + "." + metricSnapshot.type().getName())
                .value(metricSnapshot.value())
                .timestamp(metricSnapshot.timestamp())
                .putAllTags(tagsMap)
                .build();
    }
}
