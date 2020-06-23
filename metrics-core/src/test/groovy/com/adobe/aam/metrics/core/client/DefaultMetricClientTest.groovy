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

import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.core.publish.Publisher
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.SimpleMetric
import com.google.common.collect.Queues
import spock.lang.Specification
import spock.lang.Subject
import spock.util.concurrent.BlockingVariable

import static com.adobe.aam.metrics.metric.Metric.Type

class DefaultMetricClientTest extends Specification {

    @Subject queue = Queues.newLinkedBlockingQueue()
    def "test send metric with a single producer"() {

        setup:

        def actualMetricsSent = new BlockingVariable<Collection<Metric>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isAllowed(*_) >> true

            config() >> Mock(PublisherConfig)
        }

        def metricClient = new DefaultMetricClient(queue, publisher)

        when:
        metricClient.send(genMetric("max_request_time", Type.MIN, 100))
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 1
        def metricSent = metricsSent.iterator().next()
        metricSent.getName() == "max_request_time"
        metricSent.getType() == Type.MIN
        metricSent.get() == 100
    }

    def "test graphite queue flush on shutdown"() {

        setup:
        def actualMetricsSent = new BlockingVariable<Collection<Metric>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isAllowed(*_) >> true

            config() >> Mock(PublisherConfig)
        }

        def metricClient = new DefaultMetricClient(queue, publisher);

        when:
        metricClient.send([genMetric("latency", Type.AVG, 50), genMetric("mycounter", Type.COUNT, 100)])
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 2
        def iterator = metricsSent.iterator().sort(new Comparator<Metric>() {
            @Override
            int compare(Metric o1, Metric o2) {
                return o1.getName().compareTo(o2.getName())
            }
        })

        def metric1 = iterator.next()
        metric1.getName() == "latency"
        metric1.getType() == Type.AVG
        metric1.get() == 50

        def metric2 = iterator.next()
        metric2.getName() == "mycounter"
        metric2.getType() == Type.COUNT
        metric2.get() == 100
    }

    def "test send metric when using a filter setup on the client"() {

        setup:
        def actualMetricsSent = new BlockingVariable<Collection<Metric>>()
        def publisher = Mock(Publisher) {
            publishMetrics(*_) >> { metrics ->
                actualMetricsSent.set(metrics[0])
            }

            isAllowed(*_) >> {
                metric -> "only-this-metric".equals(metric[0].getName())
            }

            config() >> Mock(PublisherConfig)
        }

        def metricClient = new DefaultMetricClient(queue, publisher);

        when:
        metricClient.send([genMetric("max_request_time", Type.MIN, 100), genMetric("only-this-metric", Type.AVG, 100)])
        metricClient.flush()

        then:
        def metricsSent = actualMetricsSent.get()
        metricsSent.size() == 1
        def metric = metricsSent.iterator().next()
        metric.getName() == "only-this-metric"
        metric.getType() == Type.AVG
        metric.get() == 100
    }

    def Metric genMetric(name, type, value) {
        return new SimpleMetric(name, type, value)
    }
}
