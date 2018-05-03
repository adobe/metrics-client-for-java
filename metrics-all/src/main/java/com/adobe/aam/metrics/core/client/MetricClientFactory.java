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

package com.adobe.aam.metrics.core.client;

import com.adobe.aam.metrics.BufferedMetricClient;
import com.adobe.aam.metrics.metric.ImmutableTags;
import com.adobe.aam.metrics.metric.Tags;
import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.core.publish.PublisherFactory;
import com.google.common.collect.Queues;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.adobe.aam.metrics.core.config.ConfigUtils.getBoolean;
import static com.adobe.aam.metrics.core.config.ConfigUtils.getString;
import static java.net.InetAddress.getLocalHost;

public final class MetricClientFactory {

    private final static Logger logger = LoggerFactory.getLogger(MetricClientFactory.class);

    private final PublisherFactory publisherFactory;

    public MetricClientFactory() {
        this.publisherFactory = new PublisherFactory();
    }

    /**
     * @param config the client config, containing information such as the backend server, port, batch size etc.
     * @return a Metric Client which uses the config provided.
     */
    public BufferedMetricClient createMetricClient(PublisherConfig config, Tags tags) {
        return new DefaultMetricClient(
                Queues.newLinkedBlockingQueue(),
                publisherFactory.create(config),
                tags
        );
    }

    public BufferedMetricClient createMetricClient(Config config, Tags tags) {
        logger.info("Creating metric client using config: {}", config);
        return createMetricClient(
                PublisherConfig.fromConfig(config),
                tags);
    }

    public BufferedMetricClient create(List<? extends Config> configList, Tags tags) {
        return new DefaultMetricClient(
                Queues.newLinkedBlockingQueue(),
                getPublishers(configList),
                tags);
    }

    public BufferedMetricClient create(List<? extends Config> configList, Config tags) {
        return new DefaultMetricClient(
                Queues.newLinkedBlockingQueue(),
                getPublishers(configList),
                getTags(tags));
    }

    private Collection<Publisher> getPublishers(List<? extends Config> configList) {
        return configList
                .stream()
                .map(PublisherConfig::fromConfig)
                .map(publisherFactory::create)
                .collect(Collectors.toList());
    }

    public Tags getTags(Config config) {
        return ImmutableTags.builder()
                .environment(getString(config, "env"))
                .appName(getString(config, "app_name"))
                .regionName(getString(config, "region"))
                .clusterName(getString(config, "cluster"))
                .hostname(getHostname(getBoolean(config, "useHostname", true)))
                .build();
    }

    private Optional<String> getHostname(boolean useHostname) {

        try {
            return useHostname
                    ? Optional.ofNullable(getLocalHost().getHostName().split("\\.")[0])
                    : Optional.empty();
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
