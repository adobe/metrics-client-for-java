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

package com.adobe.aam.metrics.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BlacklistPattern {

    private final static Logger logger = LoggerFactory.getLogger(BlacklistPattern.class);
    private final String patternString;
    private final Pattern pattern;

    public BlacklistPattern(String patternString, Pattern pattern) {

        this.patternString = patternString;
        this.pattern = pattern;
    }

    public static BlacklistPattern of(String patternString) {
        Pattern pattern = null;

        String regex = patternString.replaceAll(".", "[$0]").replace("[*]", ".*");

        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            logger.warn("Unable to parse blacklist pattern: {}", patternString);
        }

        return new BlacklistPattern(patternString, pattern);
    }

    public Optional<Pattern> getPattern() {
        return Optional.ofNullable(pattern);
    }

    public boolean matches(String input) {
        boolean isBlacklisted = getPattern().isPresent() && getPattern().get().matcher(input).find();
        return isBlacklisted || input.contains(patternString);
    }
}
