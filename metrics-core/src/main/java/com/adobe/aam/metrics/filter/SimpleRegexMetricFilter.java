package com.adobe.aam.metrics.filter;

import com.adobe.aam.metrics.metric.Metric;
import com.yevdo.jwildcard.JWildcard;

public abstract class SimpleRegexMetricFilter implements MetricFilter {

    boolean filterMatches(String filter, Metric metric) {
        final String name = getMetricName(metric).toString().toLowerCase();
        final String fullName = getFullName(metric);
        return JWildcard.matches(filter, name) || JWildcard.matches(filter, fullName);
    }

    private String getFullName(Metric metric) {
        StringBuilder name = getMetricName(metric);
        name.append('.').append(metric.getType().getName().toLowerCase());
        return name.toString();
    }

    private StringBuilder getMetricName(Metric metric) {
        return metric.getLabels().format('.');
    }

}
