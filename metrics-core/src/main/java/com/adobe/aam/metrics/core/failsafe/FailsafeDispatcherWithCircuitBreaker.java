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

import com.adobe.aam.metrics.core.publish.PublishCommand;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.function.CheckedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for publishing metrics to the Graphite Backend using a circuit breaker mechanism
 * provided by Failsafe.
 *
 * References:
 * - https://martinfowler.com/bliki/CircuitBreaker.html
 * - https://github.com/jhalterman/failsafe
 */
public class FailsafeDispatcherWithCircuitBreaker implements FailsafeDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(FailsafeDispatcherWithCircuitBreaker.class);

    private final CircuitBreaker circuitBreaker;

    public FailsafeDispatcherWithCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void dispatch(final PublishCommand publishCommand) {
        try {
            Failsafe.with(circuitBreaker)
                    .run(new CheckedRunnable() {
                        @Override
                        public void run() throws Exception {
                            publishCommand.execute();
                        }
                    });
        } catch (Exception e) {
            logger.warn("Failed to publish metrics to Graphite.", e.getMessage());
        }
    }
}
