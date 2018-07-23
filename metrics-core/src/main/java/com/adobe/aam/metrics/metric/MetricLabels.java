package com.adobe.aam.metrics.metric;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface MetricLabels {

    String metricName();

    Optional<String> preLabelName();
    Optional<String> preLabelValue();

    List<String> postLabelNames();
    List<String> postLabelValues();

    default MetricLabels normalize() {
        if (preLabelName().isPresent() && !preLabelValue().isPresent()) {

            return ImmutableMetricLabels.builder()
                    .metricName(metricName())
                    .addAllPostLabelNames(postLabelNames())
                    .addAllPostLabelValues(postLabelValues())
                    .build();
        }

        return this;
    }

    default StringBuilder format(char delim) {
        StringBuilder metric = new StringBuilder();

        preLabelName().ifPresent(metric::append);
        preLabelValue().ifPresent(prefixValue -> metric.append(metric.length() > 0 ? delim : "").append(prefixValue));

        metric.append(metric.length() > 0 ? delim : "").append(metricName());

        List<String> postLabelNames = postLabelNames();
        List<String> postLabelValues = postLabelValues();
        for (int i = 0; i < postLabelValues.size(); i++) {
            if (i < postLabelNames.size()) {
                metric.append(delim).append(postLabelNames.get(i));
            }
            metric.append(delim).append(postLabelValues.get(i));
        }

        return metric;
    }

    public static MetricLabels of(String metricName) {
        return ImmutableMetricLabels.builder().metricName(metricName).build();
    }
}
