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

package com.adobe.aam.metrics.graphite

import com.adobe.aam.metrics.core.config.ImmutablePublisherConfig
import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.SimpleMetric
import com.adobe.aam.metrics.metric.bucket.MetricBucketImpl
import org.testng.collections.Lists
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class GraphitePublisherTest extends Specification {

    @Shared
            tags = ImmutableTags.builder().environment("prod").appName("myapp").build()

    def "test send metric using the Graphite publisher"() {

        setup:

        def actualHost = new BlockingVariable<String>()
        def actualPort = new BlockingVariable<Integer>()
        def actualTimeout = new BlockingVariable<Integer>()
        def line1 = new BlockingVariable<String>()
        def line2 = new BlockingVariable<String>()
        int linesOutputted = 0

        def socketWriter = Mock(SocketWriter) {

            open(*_) >> { arguments ->
                final String host = arguments[0]
                final int port = arguments[1]
                final int timeout = arguments[2]

                actualHost.set(host)
                actualPort.set(port)
                actualTimeout.set(timeout)
            }

            write(_) >> { arguments ->
                final String line = arguments[0]
                if (linesOutputted++ == 0) {
                    line1.set(line)
                } else {
                    line2.set(line)
                }
            }
        }

        def socketFactory = Mock(SocketWriterFactory) {
            create() >> {
                return socketWriter
            }
        }

        def publishFrequencyMs = 60000
        PublisherConfig config = ImmutablePublisherConfig.builder()
                .type("Graphite")
                .name("Graphite publisher")
                .host("https://myhost")
                .resetCounters(true)
                .sendOnlyRecentlyUpdatedMetrics(true)
                .socketTimeout(900)
                .publishFrequencyMs(publishFrequencyMs)
                .tags(tags)
                .build();

        def graphitePublisher = new GraphitePublisher(config, socketFactory)

        def metricT1 = new SimpleMetric("request", Metric.Type.COUNT, 100, System.currentTimeMillis() - publishFrequencyMs + 1000)
        def staleMetricT2 = new SimpleMetric("request.other", Metric.Type.COUNT, 120, System.currentTimeMillis() - 2 * publishFrequencyMs)

        when:
        graphitePublisher.publishMetrics([metricT1, staleMetricT2])

        then:
        actualHost.get() == "https://myhost"
        actualPort.get() == 2003
        actualTimeout.get() == 900

        and:
        line1.get().startsWith("prod.myapp.request.count 100.00")

        when:
        metricT1.track(120)
        graphitePublisher.publishMetrics([metricT1])

        then:
        line2.get().startsWith("prod.myapp.request.count 20.00")
    }

    def "test send metric using the Graphite publisher with prefix and suffix"() {

        setup:

        def line = new BlockingVariable<String>()

        def socketWriter = Mock(SocketWriter) {

            open(*_) >> {
            }

            write(_) >> { arguments ->
                line.set(arguments[0])
            }
        }

        def socketFactory = Mock(SocketWriterFactory) {
            create() >> {
                return socketWriter
            }
        }

        def publishFrequencyMs = 60000
        PublisherConfig config = ImmutablePublisherConfig.builder()
                .type("Graphite")
                .name("Graphite publisher")
                .host("https://myhost")
                .resetCounters(true)
                .sendOnlyRecentlyUpdatedMetrics(true)
                .socketTimeout(900)
                .publishFrequencyMs(publishFrequencyMs)
                .tags(tags)
                .build();

        def graphitePublisher = new GraphitePublisher(config, socketFactory)

        def bucket = new MetricBucketImpl("request", Metric.Type.COUNT, "prefix", Lists.newArrayList("suffix"))
        def metric = bucket.getMetric("prefix_value", "suffix_value")
        metric.track(10)

        when:
        graphitePublisher.publishMetrics([metric])

        then:
        line.get().startsWith("prod.myapp.prefix.prefix_value.request.suffix.suffix_value.count 10.00")
    }
}
