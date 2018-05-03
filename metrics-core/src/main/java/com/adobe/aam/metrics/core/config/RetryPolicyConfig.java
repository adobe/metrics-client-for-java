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
import static com.adobe.aam.metrics.core.config.ConfigUtils.getInt;

/**
 * Wrapper for the retry mechanism configuration used for publishing metrics to the backend.
 *
 * References:
 * - https://github.com/jhalterman/failsafe
 */

@Value.Immutable
public interface RetryPolicyConfig {

    @Value.Default
    default boolean enabled() {
        return true;
    }

    /**
     * Specify the number of time the client should retry to send the metrics to the backend data when an error
     * occurs.
     */
    @Value.Default
    default int retryAttempts() {
        return 3;
    }

    /**
     * Specify the time the client should wait before attempting to retry sending the metrics to the backend when
     * an error occurs.
     *
     */
    @Value.Default
    default int retryWaitTimeMs() {
        return 20000;
    }

    @Value.Check
    default void check() {

        Preconditions.checkState(retryAttempts() > 0, "'retryAttempts' should be a positive number");
        Preconditions.checkState(retryWaitTimeMs() > 0, "'retryWaitTimeMs' should be a positive number");
    }

    public static RetryPolicyConfig fromConfig(Config config) {
        boolean enabled = getBoolean(config, "retry_policy.enabled", true);
        int retryAttempts = getInt(config, "retry_policy.retry_attempts", 3);
        int retryWaitTimeMs = getInt(config, "retry_policy.retry_wait_time_ms", 20000);

        return ImmutableRetryPolicyConfig.builder()
                .enabled(enabled)
                .retryAttempts(retryAttempts)
                .retryWaitTimeMs(retryWaitTimeMs)
                .build();
    }

    public static RetryPolicyConfig defaultConfig() {
        return ImmutableRetryPolicyConfig.builder().build();
    }
}
