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

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.stream.Stream;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public abstract class Tags {
    public abstract Optional<String> environment();

    public abstract Optional<String> appName();

    public abstract Optional<String> regionName();

    public abstract Optional<String> clusterName();

    public abstract Optional<String> hostname();

    @Value.Lazy
    public String asMetricName() {

        return Stream.of(environment(), appName(), regionName(), clusterName(), hostname())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(StringUtils::isNotBlank)
                .reduce((first, second) -> first + '.' + second)
                .orElse("");

    }
}
