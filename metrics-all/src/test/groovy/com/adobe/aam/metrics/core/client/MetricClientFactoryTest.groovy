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

import com.adobe.aam.metrics.core.failsafe.FailsafePublisher
import com.adobe.aam.metrics.core.publish.Publisher
import com.adobe.aam.metrics.graphite.GraphitePublisher
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Tags
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spock.lang.Shared
import spock.lang.Specification

import static java.util.Optional.of

class MetricClientFactoryTest extends Specification {

    @Shared
            tags = ImmutableTags.builder().appName("myapp").regionName("us-east-1").build();
    
    def "test metric client factory for ConfigList with no defined clients"() {

        setup:
        def clientFactory = new MetricClientFactory()
        def configString = """
                publishers: [ 
                ]
                """

        Config config = ConfigFactory.parseString(configString).resolve()

        when:
        def metricClient = clientFactory.create(config.getConfigList("publishers"), tags)

        then:
        metricClient instanceof DefaultMetricClient

        when:
        DefaultMetricClient multipleClient = (DefaultMetricClient) metricClient

        then:
        multipleClient.getPublishers().size() == 0
    }

    def "test metric client factory for ConfigList with 1 client"() {

        setup:
        def clientFactory = new MetricClientFactory()
        def configString = """
                publishers: [ 
                    {
                        name: Graphite Primary
                        type: graphite
                        host: "myhost"
                        port: 2003
                        batch_size: 500
                        
                    }
                    
                     
                ]
                
                relabel: {
                        "pcs\\\\.([^.]+).*": {
                          "cassandra_table": "\$1"
                        }
                  }
                
                """

        Config config = ConfigFactory.parseString(configString).resolve()

        when:
        def metricClient = clientFactory.create(config.getConfigList("publishers"), tags)

        then:
        metricClient instanceof DefaultMetricClient

        when:
        DefaultMetricClient multipleClient = (DefaultMetricClient) metricClient

        then:
        multipleClient.getPublishers().size() == 1
        multipleClient.getPublishers().iterator().next() instanceof FailsafePublisher

        when:
        def decorator = (FailsafePublisher) multipleClient.getPublishers().iterator().next()

        def publisher = decorator.getPublisher()
        then:
        publisher instanceof GraphitePublisher
        publisher.config().name() == "Graphite Primary"
        publisher.config().host() == "myhost"
        publisher.config().port() == of(2003)
        publisher.config().batchSize() == 500
    }


    def "test metric client factory for ConfigList with 2 clients"() {

        setup:
        def clientFactory = new MetricClientFactory()
        def configString = """
                publishers: [ 
                    {
                        name: Graphite Primary
                        type: graphite
                        host: "myhost"
                        port: 2003
                        batch_size: 500
                    },
                    {
                        name: Graphite Secondary
                        type: graphite
                        host: "otherhost"
                        port: 2004
                        batch_size: 300
                    }
                ]"""

        Config config = ConfigFactory.parseString(configString).resolve()

        when:
        def metricClient = clientFactory.create(config.getConfigList("publishers"), tags)

        then:
        metricClient instanceof DefaultMetricClient

        when:
        DefaultMetricClient multipleClient = (DefaultMetricClient) metricClient

        then:
        multipleClient.getPublishers().size() == 2

        when:
        def it = multipleClient.getPublishers().iterator()
        def firstPublisher = it.next().getPublisher()
        def secondPublisher = it.next().getPublisher()

        then:
        firstPublisher.config().name() == "Graphite Primary"
        firstPublisher.config().host() == "myhost"
        firstPublisher.config().port() == of(2003)
        firstPublisher.config().batchSize() == 500

        then:
        secondPublisher.config().name() == "Graphite Secondary"
        secondPublisher.config().host() == "otherhost"
        secondPublisher.config().port() == of(2004)
        secondPublisher.config().batchSize() == 300
    }
}
