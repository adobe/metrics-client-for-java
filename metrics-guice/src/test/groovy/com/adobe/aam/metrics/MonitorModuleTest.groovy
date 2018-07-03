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

package com.adobe.aam.metrics

import com.adobe.aam.metrics.agent.MetricAgent
import com.adobe.aam.metrics.agent.MetricAgentConfig
import com.adobe.aam.metrics.agent.ValueProvider
import com.adobe.aam.metrics.core.MetricRegistryReporter
import com.adobe.aam.metrics.core.client.DefaultMetricClient
import com.adobe.aam.metrics.core.client.MetricClientFactory
import com.adobe.aam.metrics.core.di.MonitorModule
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.bucket.MetricBucket
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spock.lang.Specification

import java.time.Duration

class MonitorModuleTest extends Specification {
    def collectFrequency = 1000
    def sendOnlyRecentlyUpdatedMetrics = true
    def graphiteHost = "127.0.0.1"
    def graphitePort = 32770
    def appName = "httpSender"
    def flushInterval = Duration.ofMillis(60000)
    def batchSize = 500
    def appendLocalHostname = false

    def "MonitorModule test"() {
        setup:
        Injector injector = Guice.createInjector(new MetricAgentConfigModule(), new MonitorModule())

        when:
        MetricAgent metricAgent = injector.getInstance(MetricAgent.class)

        then:
        metricAgent != null
        metricAgent.metricBuckets as Set == MetricAgentConfigModule.MockMetrics.values() as Set
        metricAgent.collectFrequency == Duration.ofMillis(collectFrequency)
        metricAgent.metricClient instanceof DefaultMetricClient
    }

    class MetricAgentConfigModule extends AbstractModule {

        def configString = """
            monitor {
                collectFrequency : ${collectFrequency}
                sendOnlyRecentlyUpdatedMetrics: ${sendOnlyRecentlyUpdatedMetrics}

                publishers: [ 
                    {
                        name: Graphite Primary
                        type: graphite
                        host: ${graphiteHost}
                        port: ${graphitePort}
                        tags.app_name: ${appName}
                        flush_interval: ${"" + flushInterval.toMillis() + "ms"}
                        batch_size: ${batchSize}
                        append_local_hostname: ${appendLocalHostname}
                    }
                ]
            }"""

        Config config;

        @Override
        protected void configure() {

        }

        MetricAgentConfigModule() {
            config = ConfigFactory.parseString(configString).resolve()
        }

        @Provides
        BufferedMetricClient provideMetricClient() {
            Config monitorConfig = config.getObject("monitor").toConfig()
            ImmutableTags tags = ImmutableTags.builder().build();
            return new MetricClientFactory().create(monitorConfig.getConfigList("publishers"), tags)
        }

        @Provides
        MetricAgentConfig provideMetricAgentConfig() {

            return new MetricAgentConfig() {

                @Override
                Set<MetricBucket> getMetricBuckets() {
                    return MockMetrics.values()
                }

                @Override
                Set<Metric> getMetrics() {
                    return Collections.emptySet()
                }

                @Override
                Set<MetricRegistryReporter> getMetricRegistries() {
                    return [new MetricRegistryReporter() {
                        void reportTo(MetricClient metricClient) {

                        }
                    }] as Set
                }

                @Override
                Map<Metric, ValueProvider> getMetricValueProviders() {
                    return Collections.emptyMap()
                }

                @Override
                public boolean sendOnlyRecentlyUpdatedMetrics() {
                    return config.getBoolean("monitor.sendOnlyRecentlyUpdatedMetrics");
                }

                @Override
                public Duration getCollectFrequency() {
                    return config.getDuration("monitor.collectFrequency");
                }
            }

        }

        enum MockMetrics implements MetricBucket {
            METRIC1,
            METRIC2,
            METRIC3

            @Override
            Collection<Metric> getMetrics() {
                return null
            }

            @Override
            Metric getMetric(Object child) {
                return null
            }

            @Override
            Metric getParentMetric() {
                return null
            }
        }
    }
}
