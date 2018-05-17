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
package com.adobe.aam.metrics.core.config;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import org.immutables.value.Value;

import static com.adobe.aam.metrics.core.config.ConfigUtils.getBoolean;
import static com.adobe.aam.metrics.core.config.ConfigUtils.getDurationMs;
import static com.adobe.aam.metrics.core.config.ConfigUtils.getInt;

/**
 * Wrapper for the circuit breaker mechanism configuration used for publishing metrics to Graphite.
 * <p>
 * References:
 * - https://martinfowler.com/bliki/CircuitBreaker.html
 * - https://github.com/jhalterman/failsafe
 */
@Value.Immutable
public interface CircuitBreakerConfig {

    String name();

    /**
     * Specify if a circuit breaker mechanism should be used when dispatching metrics to the
     * backend.
     * @return true if circuit breaker is enabled, false otherwise
     */
    @Value.Default
    default boolean enabled() {
        return true;
    }


    /**
     * Specify how many consecutive successful calls to the Backend should the client
     * register to establish that the backend is responsive again.
     * <p>
     * This is taken into consideration after the Backend has been marked non-responsive
     * and the `connectWaitTime` has passed.
     * <p>
     * When this threshold is exceeded, all further client calls are forwarded to the
     * Backend according to the normal flow.
     *
     */
    @Value.Default
    default int successThreshold() {
        return 3;
    }

    /**
     * Specify how many consecutive failing calls to the Backend should the client
     * register to establish that the backend is non-responsive.
     * <p>
     * When this threshold is exceeded, all further client calls are not anymore forwarded to
     * the Backend, i.e. the sent metrics are disregarded in favour of waiting for a
     * healthy backend.
     * <p>
     * To make the client try again to forward the calls to the Backend, set the
     * `connectWaitTime`.
     */
    @Value.Default
    default int failureThreshold() {
        return 3;
    }

    /**
     * Specify the time the client should wait before attempting to send the metrics
     * to the previously marked as non-responsive Backend.
     */
    @Value.Default
    default int connectWaitTimeMs() {
        return 1000;
    }

    /**
     * Specify the timeout after which a request should be considered as failed. This
     * will update the failures counter accordingly and, if this is the case, will consider the
     * Backend non-responsive when acting really slow. The advantage of using this is
     * letting the backend get back to its full power without overwhelming it when slow.
     */
    @Value.Default
    default int requestTimeoutMs() {
        return 1000;
    }

    @Value.Check
    default void check() {

        Preconditions.checkState(successThreshold() > 0, "'successThreshold' should be a positive number");
        Preconditions.checkState(failureThreshold() > 0, "'failureThreshold' should be a positive number");
        Preconditions.checkState(connectWaitTimeMs() > 0, "'connectWaitTimeMs' should be a positive number");
        Preconditions.checkState(requestTimeoutMs() > 0, "'requestTimeoutMs' should be a positive number");
    }

    public static CircuitBreakerConfig fromConfig(Config config, String name) {
        boolean enabled = getBoolean(config, "circuit_breaker.enabled", true);
        int successThreshold = getInt(config, "circuit_breaker.success_threshold", 3);
        int failureThreshold = getInt(config, "circuit_breaker.failure_threshold", 3);
        int requestTimeoutMs = getDurationMs(config, "circuit_breaker.request_timeout_ms", 1000);
        int connectWaitTimeMs = getDurationMs(config, "circuit_breaker.connect_wait_time_ms", 1000);

        return ImmutableCircuitBreakerConfig
                .builder()
                .name(name)
                .enabled(enabled)
                .successThreshold(successThreshold)
                .failureThreshold(failureThreshold)
                .requestTimeoutMs(requestTimeoutMs)
                .connectWaitTimeMs(connectWaitTimeMs)
                .build();
    }

    public static CircuitBreakerConfig defaultConfig() {
        return ImmutableCircuitBreakerConfig.builder().build();
    }
}
