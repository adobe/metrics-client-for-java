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

import com.adobe.aam.metrics.core.config.CircuitBreakerConfig;
import com.adobe.aam.metrics.core.config.RetryPolicyConfig;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class FailsafeDispatcherFactory {

    private static final Logger logger = LoggerFactory.getLogger(FailsafeDispatcherFactory.class);

    /**
     * Create a `FailsafeDispatcher` instance, given the configuration for the retry and circuit
     * breaker mechanisms, used for safely dispatching a metrics call to the Graphite backend.
     *
     * Depending on the enabled mechanisms, a custom `FailsafeDispatcher` instance will be returned.
     */
    public static FailsafeDispatcher create(RetryPolicyConfig retryPolicyConfig,
                                            CircuitBreakerConfig circuitBreakerConfig) {
        boolean retryEnabled = retryPolicyConfig.enabled();
        boolean circuitBreakerEnabled = circuitBreakerConfig.enabled();

        if (retryEnabled && circuitBreakerEnabled) {
            return new FailsafeDispatcherWithRetryAndCircuitBreaker(
                    circuitBreakerConfig.name(),
                    circuitBreakerFromConfig(circuitBreakerConfig),
                    retryPolicyFromConfig(retryPolicyConfig));
        }

        if (retryEnabled) {
            return new FailsafeDispatcherWithRetry(retryPolicyFromConfig(retryPolicyConfig));
        }

        if (circuitBreakerEnabled) {
            return new FailsafeDispatcherWithCircuitBreaker(
                    circuitBreakerFromConfig(circuitBreakerConfig));
        }

        return new FailsafeDispatcherNoop();

    }

    /**
     * Create a Failsafe `RetryPolicy` instance from a given configuration. This is a wrapper for
     * the actual implementation with the aim of providing a way of setting a retry policy w/o a
     * delay between the retries, in a safe way.
     *
     * Failsafe does not accept a value <= 0 ms for this delay. However, by using this wrapper, one
     * can configure the delay with 0 and safely expect the retries being performed instantly.
     */
    private static RetryPolicy retryPolicyFromConfig(RetryPolicyConfig retryConfig) {
        final RetryPolicy retryPolicy = new RetryPolicy()
                .retryOn(Exception.class)
                .withMaxRetries(retryConfig.retryAttempts());

        final int retryWaitTimeMs = retryConfig.retryWaitTimeMs();
        if (retryWaitTimeMs > 0) {
            return retryPolicy.withDelay(retryWaitTimeMs, TimeUnit.MILLISECONDS);
        } else {
            return retryPolicy;
        }
    }

    /**
     * Create a Failsafe `CircuitBreaker` instance from a given configuration. This is a wrapper for
     * the actual implementation with the aim of providing a way of setting a circuit breaker mechanism
     * w/o a timeout for considering a long time taking request a failure and w/o a timeout for
     * retrying to close the circuit once it has been opened.
     *
     * Failsafe does not accept values <=0 for these timeouts. However, by using this wrapper, one
     * can configure them with 0 and safely expect the circuit breaker safely running without a request
     * timeout or/and a delay for the transition between open and half-open states.
     *
     * Reference:
     * - https://martinfowler.com/bliki/CircuitBreaker.html
     */
    private static CircuitBreaker circuitBreakerFromConfig(final CircuitBreakerConfig circuitBreakerConfig) {
        CircuitBreaker circuitBreaker = new CircuitBreaker()
                .withSuccessThreshold(circuitBreakerConfig.successThreshold())
                .withFailureThreshold(circuitBreakerConfig.failureThreshold())
                .withTimeout(circuitBreakerConfig.requestTimeoutMs(), TimeUnit.MILLISECONDS)
                .withDelay(circuitBreakerConfig.connectWaitTimeMs(), TimeUnit.MILLISECONDS);

        circuitBreaker.onOpen(
                () -> logger.warn("Metric client '{}' is not responding. Opened circuit. Dropping next metrics batches...",
                        circuitBreakerConfig.name())
        );

        circuitBreaker.onHalfOpen(() -> logger.warn("Checking on previously non-responsive metric client '{}' (half-open)." +
                        " Abandoning if configured success threshold {} is not met",
                circuitBreakerConfig.name(), circuitBreakerConfig.successThreshold()));

        return circuitBreaker;
    }
}
