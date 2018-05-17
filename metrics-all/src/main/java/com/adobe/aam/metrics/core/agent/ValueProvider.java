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

package com.adobe.aam.metrics.core.agent;


import java.util.Optional;

/**
 * A ValueProvider is useful for parts of the application that can't be instrumented with the metrics client.
 * For instance, if you use a Jetty/Tomcat web server, you might not be able to increment a counter for each rejected
 * request, which happens internally, in Jetty/Tomcat.
 * Instead, you can use a ValueProvider to fetch the number of rejected requests, periodically, from your app.
 *
 * <pre>
 * {@code
 * MetricAgent.builder()
 * 		.putMetricValueProvider(Metrics.REQUESTS_REJECTED, () -> Optional.of(webServer.getRejectedRequestsCount())
 * }
 * </pre>
 * The MetricAgent will call webServer.getRejectedRequestsCount() on each cycle, and populate the metric with the
 * provided value.
 */
@FunctionalInterface
public interface ValueProvider {

	 Optional<Double> getValue();
}
