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

import com.adobe.aam.metrics.core.ImmutableMetricSnapshot
import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class GraphitePublisherTest extends Specification {

    def "test send metric using the Graphite publisher"() {

        setup:

        def actualHost = new BlockingVariable<String>()
        def actualPort = new BlockingVariable<Integer>()
        def actualTimeout = new BlockingVariable<Integer>()
        def actualBody = new BlockingVariable<String>()

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
                actualBody.set(line)
            }
        }

        def socketFactory = Mock(SocketWriterFactory) {
            create() >> {
                return socketWriter
            }
        }

        def config = Mock(PublisherConfig) {
            host() >> "https://myhost"
            port() >> Optional.empty()
            socketTimeout() >> 900
        }

        def graphitePublisher = new GraphitePublisher(config, socketFactory)

        def metric = ImmutableMetricSnapshot.builder()
                .name("request")
                .type(Metric.Type.COUNT)
                .timestamp(123)
                .value(100)
                .tags(ImmutableTags.builder().environment("prod").appName("myapp").build())
                .build()


        when:
        graphitePublisher.publishMetrics([metric])

        then:
        actualHost.get() == "https://myhost"
        actualPort.get() == 2003
        actualTimeout.get() == 900

        and:
        actualBody.get() == "prod.myapp.request.count 100.00 123"
    }
}
