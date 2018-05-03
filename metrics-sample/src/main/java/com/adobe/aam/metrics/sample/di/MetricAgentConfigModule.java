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

package com.adobe.aam.metrics.sample.di;

import com.adobe.aam.metrics.BufferedMetricClient;
import com.adobe.aam.metrics.core.agent.ImmutableMetricAgentConfig;
import com.adobe.aam.metrics.core.agent.MetricAgentConfig;
import com.adobe.aam.metrics.codahale.MetricRegistryReporter;
import com.adobe.aam.metrics.core.client.MetricClientFactory;
import com.adobe.aam.metrics.sample.SampleWebServiceMetrics;
import com.adobe.aam.metrics.sample.http.HttpDispatcher;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;

import java.util.Optional;

class MetricAgentConfigModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    BufferedMetricClient provideMetricClient(MetricClientFactory metricClientFactory, Config config) {
        return metricClientFactory
                .create(config.getConfigList("monitor.publishers"), config.getConfig("monitor.tags"));
    }

    @Provides
    MetricAgentConfig provideMetricAgentConfig(Config config, HttpDispatcher httpDispatcher) {

        return ImmutableMetricAgentConfig.builder()
                .sendOnlyRecentlyUpdatedMetrics(config.getBoolean("monitor.sendOnlyRecentlyUpdatedMetrics"))
                .collectFrequency(config.getDuration("monitor.collectFrequency"))
                .addMetricBuckets(SampleWebServiceMetrics.values())
                .addMetricRegistries(new MetricRegistryReporter(SampleWebServiceMetrics.registry))
                .putMetricValueProviders(SampleWebServiceMetrics.REJECTED_REQUESTS.getParentMetric(),
                        () -> Optional.of((double) httpDispatcher.getRejectedRequestsCount())
                )
                .build();
    }
}
