package com.adobe.aam.metrics.core;

import com.adobe.aam.metrics.MetricClient;

public interface MetricRegistryReporter {

    void reportTo(MetricClient metricClient);
}
