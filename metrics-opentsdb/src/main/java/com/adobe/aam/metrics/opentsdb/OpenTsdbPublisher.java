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
package com.adobe.aam.metrics.opentsdb;

import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.publish.AbstractPublisher;
import com.adobe.aam.metrics.metric.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class OpenTsdbPublisher extends AbstractPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OpenTsdbPublisher.class);

    private final String name;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String endpoint;

    public OpenTsdbPublisher(PublisherConfig config) {
        this(config, new OkHttpClient().newBuilder()
                .connectTimeout(config.socketTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.socketTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.socketTimeout(), TimeUnit.MILLISECONDS)
                .build()
        );
    }

    public OpenTsdbPublisher(PublisherConfig config, OkHttpClient client) {
        super(config);
        this.name = config.name();
        this.endpoint = getEndpoint(config);
        this.client = client;
    }

    private String getEndpoint(PublisherConfig config) {
        String host = config.host();

        if (config.port().isPresent()) {
            host = host.replaceAll("(http[s]?:\\/\\/[^\\/]+)(.*)", "$1:" + config.port().get() + "$2");
        }

        return host;
    }

    @Override
    public void shutdown() {
        try {
            Cache cache = client.cache();
            if (cache != null) {
                cache.flush();
                cache.close();
            }
        } catch (IOException e) {
            logger.error("Error while trying shutdown OpenTSDB publisher", e);
        }
    }

    @Override
    public void doPublishMetrics(final Collection<Metric> metrics) throws IOException {
        logger.info("Sending {} metrics to '{}': {}.", metrics.size(), name, endpoint);
        try {
            String json = objectMapper.writeValueAsString(toOpenTsdbMetrics(metrics));
            Response response = sendPost(endpoint, json);
            logger.info("Response from '{}' was {}", name, response);
        } catch (IOException e) {
            logger.error("Error sending metrics to '{}'. {}", name, e.getMessage());
            throw e;
        }
    }

    private Response sendPost(String endpoint, String json) throws IOException {

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response;
        }
    }

    private Set<OpenTsdbMetric> toOpenTsdbMetrics(Collection<Metric> metrics) {
        return metrics.stream()
                .map(this::toOpenTsdbMetric)
                .collect(Collectors.toSet());
    }

    private OpenTsdbMetric toOpenTsdbMetric(Metric metric) {
        return OpenTsdbMetric.from(metric, super.getMetricValue(metric), config().tags());
    }
}
