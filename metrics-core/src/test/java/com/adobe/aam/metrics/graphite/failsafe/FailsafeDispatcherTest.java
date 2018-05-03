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
package com.adobe.aam.metrics.graphite.failsafe;

import com.adobe.aam.metrics.core.failsafe.*;
import com.adobe.aam.metrics.core.publish.Publisher;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;


public class FailsafeDispatcherTest {

    private final int retryAttempts = 3;
    private final int failureThreshold = 5;
    private final int successThreshold = 2;
    private final int connectWaitTime = 1000;
    private final int requestTimeout = 1000;

    private RetryPolicy retryPolicy;
    private CircuitBreaker circuitBreaker;
    private CircuitBreaker circuitBreakerWithRequestTimeout;

    @Before
    public void setupMethod() {
        retryPolicy = new RetryPolicy().withMaxRetries(retryAttempts);

        circuitBreaker = new CircuitBreaker()
                .withSuccessThreshold(successThreshold)
                .withFailureThreshold(failureThreshold)
                .withDelay(connectWaitTime, TimeUnit.MILLISECONDS);

        circuitBreakerWithRequestTimeout = new CircuitBreaker()
                .withSuccessThreshold(successThreshold)
                .withFailureThreshold(1)
                .withDelay(connectWaitTime, TimeUnit.MILLISECONDS)
                .withTimeout(requestTimeout, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSafeCommandWithRetryAndCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreaker, retryPolicy);
        Publisher passingPublisher = everPassingPublisher();

        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));

        verify(passingPublisher, times(1)).publishMetrics(any());
    }

    @Test
    public void testRetriedCommandWithRetryAndCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreaker, retryPolicy);
        Publisher failingPublisher = everFailingPublisher();

        dispatcher.dispatch(() -> failingPublisher.publishMetrics(emptyList()));

        verify(failingPublisher, times(retryAttempts + 1)).publishMetrics(any());
    }


    @Test
    public void testRetriedAndDiscardedCommandWithRetryAndCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreaker, retryPolicy);

        Publisher failingPublisher1 = everFailingPublisher();
        Publisher failingPublisher2 = everFailingPublisher();
        Publisher passingPublisher = everPassingPublisher();

        // normal retry mechanism for a failing command
        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        verify(failingPublisher1, times(retryAttempts + 1)).publishMetrics(any());

        // the next failing command is only retried until the failure threshold is exceeded
        dispatcher.dispatch(() -> failingPublisher2.publishMetrics(emptyList()));
        verify(failingPublisher2, atMost(failureThreshold - retryAttempts)).publishMetrics(any());

        // the failure threshold has been exceeded - no further calls are dispatched
        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));
        verify(passingPublisher, never()).publishMetrics(any());
    }

    @Test
    public void testSafeCommandWhenHalfOpenWithRetryAndCircuitBreaker() throws IOException, InterruptedException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreaker, retryPolicy);
        Publisher failingPublisher = everFailingPublisher();
        Publisher passingPublisher = everPassingPublisher();

        // retry until failure threshold is exceeded
        dispatcher.dispatch(() -> failingPublisher.publishMetrics(emptyList()));
        dispatcher.dispatch(() -> failingPublisher.publishMetrics(emptyList()));
        verify(failingPublisher, atMost(failureThreshold + 1)).publishMetrics(any());

        // the circuit is open - further calls are disregarded
        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));
        verify(passingPublisher, never()).publishMetrics(any());

        // wait until the circuit is half-opened
        Thread.sleep(connectWaitTime);

        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));
        verify(passingPublisher, times(1)).publishMetrics(any());
    }


    @Test
    public void testDiscardedCommandWhenReopenedWithRetryAndCircuitBreaker() throws IOException, InterruptedException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreaker, retryPolicy);

        Publisher failingPublisher1 = everFailingPublisher();
        Publisher failingPublisher2 = everFailingPublisher();
        Publisher passingPublisher1 = everPassingPublisher();
        Publisher passingPublisher2 = everPassingPublisher();

        // perform unsafe calls until the circuit is open
        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        verify(failingPublisher1, atMost(failureThreshold + 1)).publishMetrics(any());

        // wait until the circuit is half-open
        Thread.sleep(connectWaitTime);

        // perform successful call; successfulCount == 1
        dispatcher.dispatch(() -> passingPublisher1.publishMetrics(emptyList()));
        verify(passingPublisher1, times(1)).publishMetrics(any());

        // perform unsafe call; successThreshold is not met => the circuit is opened again
        dispatcher.dispatch(() -> failingPublisher2.publishMetrics(emptyList()));
        verify(failingPublisher2, times(1)).publishMetrics(any());

        // all further calls are disregarded
        dispatcher.dispatch(() -> passingPublisher2.publishMetrics(emptyList()));
        verify(passingPublisher2, never()).publishMetrics(any());
    }

    @Test
    public void testDiscardedCommandWhenExceededTimeoutWithRetryAndCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetryAndCircuitBreaker(
                "name", circuitBreakerWithRequestTimeout, retryPolicy);
        Publisher longRunningPublisher = longRunningPublisher();

        dispatcher.dispatch(() -> longRunningPublisher.publishMetrics(emptyList()));
        dispatcher.dispatch(() -> longRunningPublisher.publishMetrics(emptyList()));

        // the second call should be disregarded - the failure threshold has already been exceeded
        verify(longRunningPublisher, times(1)).publishMetrics(any());
    }

    @Test
    public void testSafeCommandWithRetry() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetry(retryPolicy);
        Publisher passingPublisher = everPassingPublisher();

        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));

        verify(passingPublisher, times(1)).publishMetrics(any());
    }

    @Test
    public void testRetriedCommandWithRetry() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetry(retryPolicy);
        Publisher failingPublisher = everFailingPublisher();

        dispatcher.dispatch(() -> failingPublisher.publishMetrics(emptyList()));

        verify(failingPublisher, times(retryAttempts + 1)).publishMetrics(any());
    }

    @Test
    public void testRetriedMultipleCommandsWithRetry() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithRetry(retryPolicy);
        Publisher failingPublisher1 = everFailingPublisher();
        Publisher failingPublisher2 = everFailingPublisher();

        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        dispatcher.dispatch(() -> failingPublisher2.publishMetrics(emptyList()));

        verify(failingPublisher1, times(retryAttempts + 1)).publishMetrics(any());
        verify(failingPublisher2, times(retryAttempts + 1)).publishMetrics(any());
    }

    @Test
    public void testSafeCommandWithCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithCircuitBreaker(circuitBreaker);
        Publisher passingPublisher = everPassingPublisher();

        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));

        verify(passingPublisher, times(1)).publishMetrics(any());
    }

    @Test
    public void testDiscardedCommandWithCircuitBreaker() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherWithCircuitBreaker(
                circuitBreaker.withFailureThreshold(2));

        Publisher failingPublisher1 = everFailingPublisher();
        Publisher failingPublisher2 = everFailingPublisher();
        Publisher passingPublisher = everPassingPublisher();

        // increase failure count
        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        verify(failingPublisher1, times(1)).publishMetrics(any());

        // exceed failure threshold - circuit is opened
        dispatcher.dispatch(() -> failingPublisher2.publishMetrics(emptyList()));
        verify(failingPublisher2, times(1)).publishMetrics(any());

        // circuit is open - command is discarded
        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));
        verify(passingPublisher, never()).publishMetrics(any());
    }

    @Test
    public void testSafeCommandWithNoop() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherNoop();
        Publisher passingPublisher = everPassingPublisher();

        dispatcher.dispatch(() -> passingPublisher.publishMetrics(emptyList()));

        verify(passingPublisher, times(1)).publishMetrics(any());
    }

    @Test
    public void testUnsafeCommandWithNoop() throws IOException {
        FailsafeDispatcher dispatcher = new FailsafeDispatcherNoop();
        Publisher failingPublisher1 = everFailingPublisher();
        Publisher failingPublisher2 = everFailingPublisher();

        dispatcher.dispatch(() -> failingPublisher1.publishMetrics(emptyList()));
        dispatcher.dispatch(() -> failingPublisher2.publishMetrics(emptyList()));

        verify(failingPublisher1, times(1)).publishMetrics(any());
        verify(failingPublisher2, times(1)).publishMetrics(any());
    }

    private Publisher longRunningPublisher() throws IOException {
        Publisher command = mock(Publisher.class);
        doAnswer(invocation -> {
            Thread.sleep(requestTimeout + 1);
            return null;
        }).when(command).publishMetrics(any());

        return command;
    }

    private Publisher everPassingPublisher() throws IOException {
        Publisher command = mock(Publisher.class);
        doNothing().when(command).publishMetrics(any());

        return command;
    }

    private Publisher everFailingPublisher() throws IOException {
        Publisher publisher = mock(Publisher.class);
        doThrow(new IOException("Failed publishing metrics")).when(publisher).publishMetrics(any());

        return publisher;
    }
}
