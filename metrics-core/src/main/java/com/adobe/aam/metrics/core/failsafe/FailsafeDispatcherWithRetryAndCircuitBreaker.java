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
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for publishing metrics to the Graphite Backend within a circuit breaker, with
 * a configured retry policy.
 * <p>
 * The retry mechanisms along the circuit breaker state and decisions are provided by Failsafe.
 * According to the Failsafe docs, the Graphite requests failures are firstly retried with respect
 * to the retry policy. Then, if the retry thresholds are exceeded, the circuit breaker starts
 * registering the failures count and will act according to its configured thresholds.
 * <p>
 * References:
 * - https://martinfowler.com/bliki/CircuitBreaker.html
 * - https://github.com/jhalterman/failsafe
 */
public class FailsafeDispatcherWithRetryAndCircuitBreaker implements FailsafeDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(FailsafeDispatcherWithRetryAndCircuitBreaker.class);

    private String name;
    private final AtomicInteger attemptsTried = new AtomicInteger();
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;

    public FailsafeDispatcherWithRetryAndCircuitBreaker(String name,
                                                        CircuitBreaker circuitBreaker,
                                                        RetryPolicy retryPolicy) {
        this.name = name;
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Execution failures are first retried according to the RetryPolicy, then if the policy is
     * exceeded the failure is recorded by the CircuitBreaker.
     */
    public void dispatch(final PublishCommand publishCommand) {
        try {
            Failsafe.with(retryPolicy)
                    .with(circuitBreaker)
                    .onFailedAttempt(throwable ->
                            logger.info("Failed attempt #{} to publish metrics to '{}': {}",
                                    attemptsTried.incrementAndGet(), name, throwable.getMessage())
                    )
                    .run(publishCommand::execute);
        } catch (Exception e) {
            logger.warn("Failed to publish batch metrics to '{}'. {}", name, e.getMessage());
        }
    }
}
