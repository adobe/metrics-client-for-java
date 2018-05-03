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

package com.adobe.aam.metrics.core.client

import com.adobe.aam.metrics.core.MetricSnapshot
import com.adobe.aam.metrics.core.publish.Publisher
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.SimpleMetric
import com.google.common.collect.Queues
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.util.concurrent.BlockingVariable

class DefaultMetricClientTest extends Specification {
    @Shared timestamp = System.currentTimeMillis()
    @Shared tags = ImmutableTags.builder().appName("myapp").regionName("us-east-1").build()
    @Subject queue = Queues.newLinkedBlockingQueue()

    def "test send metric with a single producer"() {

        setup:

        def actualMetricsSent = new BlockingVariable<List<MetricSnapshot>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isWhitelisted(*_) >> true
        }

        def metricClient = new DefaultMetricClient(queue, publisher, tags)

        when:
        metricClient.sendAndReset(new SimpleMetric("max_request_time", Metric.Type.MIN, 100), timestamp)
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 1
        metricsSent.get(0).name() == "max_request_time"
        metricsSent.get(0).type() == Metric.Type.MIN
        metricsSent.get(0).value() == 100
        metricsSent.get(0).tags() == tags
    }

    def "test send metric with multiple producers"() {

        setup:

        def metricsSentCount = new BlockingVariable<Integer>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                metricsSentCount.set(metrics[0].size())
            }

            isWhitelisted(*_) >> true
        }

        def metricClient = new DefaultMetricClient(queue, publisher, tags)

        Thread thread1 = Thread.start {
            10.times {
                metricClient.sendAndReset(Metric.newInstance("latency", Metric.Type.AVG), timestamp);
            }
        }
        Thread thread2 = Thread.start {
            10.times {
                metricClient.sendAndReset(Metric.newInstance("latency", Metric.Type.AVG), timestamp);
            }
        }
        thread1.join()
        thread2.join()

        when:
        metricClient.flush()

        then:
        metricsSentCount.get() == 20
    }

    def "test graphite queue flush on shutdown"() {

        setup:
        def actualMetricsSent = new BlockingVariable<List<MetricSnapshot>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isWhitelisted(*_) >> true
        }

        def metricClient = new DefaultMetricClient(queue, publisher, tags);

        when:
        metricClient.sendAndReset(new SimpleMetric("latency", Metric.Type.AVG, 50), timestamp);
        metricClient.sendAndReset(new SimpleMetric("mycounter", Metric.Type.COUNT, 100), timestamp);
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 2
        metricsSent.get(0).name() == "latency"
        metricsSent.get(0).type() == Metric.Type.AVG
        metricsSent.get(0).value() == 50
        metricsSent.get(0).tags() == tags

        metricsSent.get(1).name() == "mycounter"
        metricsSent.get(1).type() == Metric.Type.COUNT
        metricsSent.get(1).value() == 100
        metricsSent.get(1).tags() == tags
    }

    def "test send metric when using a filter setup on the client"() {

        setup:
        def actualMetricsSent = new BlockingVariable<List<MetricSnapshot>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isWhitelisted(*_) >> {
                metric -> "only-this-metric".equals(metric[0].name())
            }
        }

        def metricClient = new DefaultMetricClient(queue, publisher, tags);

        when:
        metricClient.sendAndReset(new SimpleMetric("max_request_time", Metric.Type.MIN, 100), timestamp);
        metricClient.sendAndReset(new SimpleMetric("only-this-metric", Metric.Type.AVG, 100), timestamp);
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 1
        metricsSent.get(0).name() == "only-this-metric"
        metricsSent.get(0).type() == Metric.Type.AVG
        metricsSent.get(0).value() == 100
        metricsSent.get(0).tags() == tags
    }
}
