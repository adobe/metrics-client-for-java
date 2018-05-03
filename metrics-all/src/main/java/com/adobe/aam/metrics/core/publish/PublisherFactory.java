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
package com.adobe.aam.metrics.core.publish;

import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.core.failsafe.FailsafeDispatcher;
import com.adobe.aam.metrics.core.failsafe.FailsafeDispatcherFactory;
import com.adobe.aam.metrics.graphite.GraphitePublisher;
import com.adobe.aam.metrics.core.failsafe.FailsafePublisher;
import com.adobe.aam.metrics.opentsdb.OpenTsdbPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PublisherFactory {

    private final static Logger logger = LoggerFactory.getLogger(PublisherFactory.class);

    /**
     * Use this for creating a `Publisher`.
     * <p>
     * For now, the publisher uses Failsafe as a provider for retry and circuit breaker mechanisms.
     * The alternative would be Hystrix, however Failsafe is more lightweight and
     * seems to fit the current needs.
     * <p>
     * In case further use cases outline the need of implementing a publisher with Hystrix, this is
     * where the implementation should be given based on config.
     * <p>
     * Reference: https://github.com/jhalterman/failsafe/wiki/Comparisons#failsafe-vs-hystrix
     */
    public Publisher create(PublisherConfig config) {
        Publisher publisher;
        switch (config.type().toLowerCase()) {
            case "graphite":
                logger.info("Creating Graphite publisher with config: {}", config);
                publisher = new GraphitePublisher(config);
                break;
            case "opentsdb":
                logger.info("Creating OpenTSDB publisher with config: {}", config);
                publisher = new OpenTsdbPublisher(config);
                break;
            default:
                throw new IllegalArgumentException("Unable to create metric client of type " + config.type());
        }

        FailsafeDispatcher dispatcher = FailsafeDispatcherFactory.create(config.retryPolicyConfig(), config.circuitBreakerConfig());
        return new FailsafePublisher(publisher, dispatcher);
    }
}
