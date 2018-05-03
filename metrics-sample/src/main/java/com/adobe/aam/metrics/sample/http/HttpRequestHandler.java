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

package com.adobe.aam.metrics.sample.http;

import com.adobe.aam.metrics.sample.SampleWebServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestHandler {

    private final static Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);

    public void handle(HttpRequest request) {

        // This will increment request.count and request.www.count
        SampleWebServiceMetrics.REQUEST_COUNT.incrementFor(request.getSubdomain());

        // This will update request.size.avg
        SampleWebServiceMetrics.AVG_REQUEST_BODY_SIZE.track(request.getBodySize());

        long processTime = processRequest(request);

        // This will update request.time.avg / request.time.p99 / request.time.p95 etc.
        SampleWebServiceMetrics.REQUEST_TIME_HISTOGRAM.update(processTime);
    }

    /**
     * @return the total time (in ms) spent while processing the HTTP request
     */
    private long processRequest(HttpRequest request) {
        long startTime = System.currentTimeMillis();

        // DO WORK
        LOG.info("Processing: {}", request);

        return System.currentTimeMillis() - startTime;
    }
}
