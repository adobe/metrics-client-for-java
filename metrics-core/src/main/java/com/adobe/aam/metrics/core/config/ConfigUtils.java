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

import com.typesafe.config.Config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ConfigUtils {

    static Optional<Integer> getInt(Config config, String path) {
        return config.hasPath(path) ? Optional.of(config.getInt(path)) : Optional.empty();
    }

    static int getInt(Config config, String path, int defaultValue) {
        return config.hasPath(path) ? config.getInt(path) : defaultValue;
    }

    static int getDurationMs(Config config, String path, int defaultValue) {
        return config.hasPath(path) ?
                (int) config.getDuration(path, TimeUnit.MILLISECONDS) : defaultValue;
    }

    public static boolean getBoolean(Config config, String path, boolean defaultValue) {
        return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
    }

    public static Optional<String> getString(Config config, String path) {
        return config.hasPath(path) ? Optional.ofNullable(config.getString(path)) : Optional.empty();
    }
}
