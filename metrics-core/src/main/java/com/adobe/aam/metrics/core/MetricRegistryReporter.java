package com.adobe.aam.metrics.core;

import com.adobe.aam.metrics.metric.Metric;

import java.util.Collection;

public interface MetricRegistryReporter {

    Collection<Metric> getMetrics();
}
