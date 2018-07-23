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
package com.adobe.aam.metrics.core.failsafe;

import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.publish.Publisher;
import com.adobe.aam.metrics.metric.Metric;

import java.util.Collection;

/**
 * Responsible for publishing a given set of metrics to the Backend.
 *
 * The communication with the backend is tracked through the medium of a configured circuit breaker
 * i.e. when the backend becomes unresponsive, the metrics are silently disregarded to protect the
 * clients from a potential OOM.
 */
public final class FailsafePublisher implements Publisher {

    private final Publisher publisher;
    private final FailsafeDispatcher dispatcher;

    public FailsafePublisher(Publisher publisher, FailsafeDispatcher dispatcher) {
        this.publisher = publisher;
        this.dispatcher = dispatcher;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    @Override
    public PublisherConfig config() {
        return publisher.config();
    }

    @Override
    public void shutdown() {
        publisher.shutdown();
    }

    @Override
    public void publishMetrics(Collection<Metric> metrics) {
        if (metrics.isEmpty()) {
            return;
        }

        dispatcher.dispatch(() -> publisher.publishMetrics(metrics));
    }
}
