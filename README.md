# Metrics Library for Java

[![Build Status](https://travis-ci.org/adobe/metrics-client-for-java.svg?branch=master)](https://travis-ci.org/adobe/metrics-client-for-java)

An application metrics client integrated with Graphite/OpenTSDB/more to come.
Offers the following features:
- sends the metrics in batch to the backend
- fail safe (retry + circuit breaker to not overwhelm the backend)
- send to multiple backends in parallel 
    - useful for migrating to one backend to another
    - useful for sending a set of metrics to one backend (e.g. a sub-set of importants metrics used for alerts in Prometheus) and another set to another backend (e.g. all metrics to OpenTSDB).

# Usage

## Maven
```xml
<!-- https://mvnrepository.com/artifact/com.adobe.aam/metrics-all -->
<dependency>
    <groupId>com.adobe.aam</groupId>
    <artifactId>metrics-all</artifactId>
    <version>1.0.2</version>
</dependency>
```

## Gradle
```
// https://mvnrepository.com/artifact/com.adobe.aam/metrics-all
compile group: 'com.adobe.aam', name: 'metrics-all', version: '1.0.2'
```

# Sample application

You can find a demo app in this project: [Sample Application](https://github.com/adobe/metrics-client-for-java/tree/master/metrics-sample)

# Java docs

You can find here the [Java docs](http://javadoc.io/doc/com.adobe.aam/metrics-core)

# Architecture

![EC2 Shredder diagram](https://user-images.githubusercontent.com/952836/36027791-5fb02010-0da5-11e8-88d7-61fc9bce60f2.png)

# Sample App: How to use the metrics library

See [metrics-sample](https://github.com/adobe/metrics-client-for-java/tree/master/metrics-sample) app, for a fully fledged demo.

# Create a metric

There are several metric types available: CounterMetric, AverageMetric, MaxMetric, MinMetric etc.

### Average Metric
```
// Create a metric that generates the average of the values.
Metric metric = Metric.newInstance("request.time", Metric.Type.AVG);

metric.track(10);
metric.track(20);

metric.get(); // returns (10 + 20) / 2 = 15
```

### Counter
```
CounterMetric metric = new CounterMetric("event.pixel");

metric.increment(); // New value is 1
metric.increment(); // New value is 2
metric.add(10); // New value is 12

metric.get(); // returns 12
```

# Send a metric to a backend (e.g. Graphite / OpenTSDB)

### How the metric client manages to publish metrics to the backend
By default, the client is able to send metrics to Backend using a retry mechanism guarded by a circuit
breaker mechanism. The former ensures metrics are being resent should the initial request fail,
providing a way of not losing important metrics. On the other hand, the circuit breaker mechanism
makes the client silently aware of a non-responsive Backend backend by discarding all incoming metrics
until the service is up and running again. 

Nevertheless, one can always switch to a client w/o retry and/or w/o circuit breaker mechanisms depending
on the environment needs. For instance, for a testing environment that does not highly rely on metrics,
the metric client can be used without retry and circuit breaker mechanisms.

However, there is a great benefit brought by the enablement of both mechanism (via config) - safely
and effectively (using retry) sending metrics while not polluting with metrics the memory of the
metric client when the Backend is non-responsive. The downside of enabling the circuit
breaker is the potential impact on aggregated metrics -  each client node might enter into an
open circuit at a different time, hence the metrics are prone to inconsistency

References:
- [Circuit Breaker]
- [Failsafe Library]
 
[Circuit Breaker]: https://martinfowler.com/bliki/CircuitBreaker.html
[Failsafe Library]: https://github.com/jhalterman/failsafe

### Create a metric client

The MetricClientFactory contains a series of methods to create a metric client. It can use either configuration file(s), a Properties object, a typesafe config etc.

```hocon
monitor {
    collectFrequency : 60000ms

    tags {
        env : prod
        app_name: mywebapp
        region: us-east-1
        cluster: edge1
        useHostname: true
    }

    publishers: [ 
        {
            name: Graphite Primary
            type: graphite
            host: graphiterelay.com
            port: 2003
            batch_size: 500
            sendOnlyRecentlyUpdatedMetrics: true
            resetCounters: true
            filter.whitelist : [
              // Only these metrics will be sent through this client.
              "*"
            ]
        },
        {
          name: Prometheus
          type: prometheus
          sendOnlyRecentlyUpdatedMetrics: false
          resetCounters: false
          filter.whitelist : [
            // Only these metrics will be exposed through this client.
            "*"
          ]
          relabel: {
            // Relabel example. Useful for 3rd party metrics such as those coming from Codahale.
            // myapp.db.table1.requests -> myapp_db_requests{db_table="table1"}
            "db\\.([^.]+).*": [
              {
                "db_table": "$1"
              }
            ]
          }
        }
    ]
}
```



```java
// Create metric client.
MetricClient metricClient = new MetricClientFactory()
      .create(config.get("monitor.publishers"));
```

### Send metrics

```java
long timeNow = System.currentTimeMillis() / 1000;
metricClient.sendAndReset(metric, timeNow);
metricClient.flush();
```


# Using a metric agent to monitor your metrics
The metric agent monitors a list of metrics. Every 60 seconds (or other configurable frequency), it sends the current metric values to the specified metric client and resets them to zero.

```java
List<Metric> metrics = new ArrayList<>();
// Add metrics to the list
...

MetricAgentConfig config = ImmutableMetricAgentConfig.builder()
                .collectFrequency(config.getDuration("monitor.collectFrequency"))
                .addMetrics(metrics)
                .tags(MetricAgentConfig.tagsFromConfig(config.getConfig("monitor.tags")))
                .build();

MetricAgent metricAgent = new MetricAgent(metricClient, config);

metricAgent.startAsync();

...

metricAgent.stopAsync();
```

# Codahale integration

This metrics library is integrated with codahale. You can add one or more codahale `MetricRegistry` to the metric agent and they will be reported to the backend.
Advantages for doing this:
- sends the metrics in batch to the backend
- fail safe (retry + circuit breaker)
- resets the metrics once sent to the backend (useful for Codahale Counters due to https://github.com/dropwizard/metrics/issues/143)
- send to multiple backends in parallel


# Build

To build this project:

```
$ git clone git@github.com:adobe/metrics-client-for-java.git
$ cd metrics-client-for-java/
$ ./gradlew build
```

# Bugs and Feedback

For bugs, questions and discussions please use the [GitHub Issues](https://github.com/adobe/metrics-client-for-java/issues).

# LICENSE

Copyright 2018 Adobe Systems Incorporated

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
