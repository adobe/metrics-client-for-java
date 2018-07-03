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

package com.adobe.aam.metrics.sample;

import com.adobe.aam.metrics.agent.MetricAgent;
import com.adobe.aam.metrics.sample.di.InjectorBuilder;
import com.adobe.aam.metrics.sample.http.HttpDispatcher;
import com.adobe.aam.metrics.sample.http.HttpRequest;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleWebService {

    private final static Logger LOG = LoggerFactory.getLogger(SampleWebService.class);

    public static void main(String[] args) throws TimeoutException, InterruptedException {

        Injector injector = InjectorBuilder.build();

        // The MetricAgent takes care of periodically collecting the metrics and publishing to the backend(s).
        MetricAgent metricAgent = injector.getInstance(MetricAgent.class);
        metricAgent.startAsync();

        HttpDispatcher dispatcher = injector.getInstance(HttpDispatcher.class);

        dispatcher.route(new HttpRequest());
        dispatcher.route(new HttpRequest());
        dispatcher.route(new HttpRequest());

        LOG.info("Processed {} requests", SampleWebServiceMetrics.REQUEST_COUNT.getCounter().get());

        metricAgent.stopAsync().awaitTerminated(1, TimeUnit.MINUTES);
    }
}
