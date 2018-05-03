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

package com.adobe.aam.metrics.opentsdb

import com.adobe.aam.metrics.core.ImmutableMetricSnapshot
import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class OpenTsdbPublisherTest extends Specification {

    def "test send metric using OpenTSDB publisher"() {

        setup:

        def httpBody = new BlockingVariable<String>()
        def httpEndpoint = new BlockingVariable<String>()
        def httpClient = Mock(OkHttpClient) {
            newCall(_) >> {
                Request request ->
                    httpBody.set(getBody(request))
                    httpEndpoint.set(request.url().toString())
                    return Mock(Call)
            }
        }

        def config = Mock(PublisherConfig) {
            host() >> "https://myhost"
            port() >> Optional.empty()
        }

        def openTsdbPublisher = new OpenTsdbPublisher(config, httpClient)

        def metric = ImmutableMetricSnapshot.builder()
                .name("request")
                .type(Metric.Type.COUNT)
                .timestamp(123)
                .value(100)
                .tags(ImmutableTags.builder().environment("prod").appName("myapp").build())
                .build()


        when:
        openTsdbPublisher.publishMetrics([metric])

        then:
        httpEndpoint.get() == "https://myhost/"
        httpBody.get() == '[{"metric":"request.count","value":100.0,"timestamp":123,"tags":{"appName":"myapp","env":"prod"}}]'
    }

    String getBody(Request request) {
        def buffer = new Buffer()
        request.body().writeTo(buffer)

        def outputStream = new ByteArrayOutputStream()
        buffer.writeTo(outputStream)
        return outputStream.toString()
    }
}
