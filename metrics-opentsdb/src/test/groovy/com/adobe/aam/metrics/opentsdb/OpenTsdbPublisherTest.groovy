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

import com.adobe.aam.metrics.core.config.ImmutablePublisherConfig
import com.adobe.aam.metrics.core.config.PublisherConfig
import com.adobe.aam.metrics.metric.ImmutableTags
import com.adobe.aam.metrics.metric.Metric
import com.adobe.aam.metrics.metric.SimpleMetric
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class OpenTsdbPublisherTest extends Specification {

    @Shared
            tags = ImmutableTags.builder().environment("prod").appName("myapp").build()

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

        def publishFrequencyMs = 60000
        PublisherConfig config = ImmutablePublisherConfig.builder()
                .type("OpenTSDB")
                .name("OpenTSDB publisher")
                .host("https://myhost")
                .publishFrequencyMs(publishFrequencyMs)
                .sendOnlyRecentlyUpdatedMetrics(true)
                .tags(tags)
                .build();

        def openTsdbPublisher = new OpenTsdbPublisher(config, httpClient)

        def metric = new SimpleMetric("request", Metric.Type.COUNT, 100, System.currentTimeMillis() - publishFrequencyMs + 1000)
        def staleMetric = new SimpleMetric("request.other", Metric.Type.COUNT, 120, System.currentTimeMillis() - 2 * publishFrequencyMs)

        when:
        openTsdbPublisher.publishMetrics([metric, staleMetric])

        then:
        httpEndpoint.get() == "https://myhost/"
        def body = httpBody.get()
        body.startsWith('[{"metric":"request.count","value":100.0,"timestamp":')
        body.endsWith('"tags":{"appName":"myapp","env":"prod"}}]')
    }

    String getBody(Request request) {
        def buffer = new Buffer()
        request.body().writeTo(buffer)

        def outputStream = new ByteArrayOutputStream()
        buffer.writeTo(outputStream)
        return outputStream.toString()
    }
}
