package com.adobe.aam.metrics.codahale;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CodahaleAllowListMetricFilter implements MetricFilter {

    private final List<String> allowList;

    public CodahaleAllowListMetricFilter(List<String> allowList) {
        this.allowList = allowList
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(item -> item.toLowerCase().trim())
                .collect(Collectors.toList());
    }

    @Override
    public boolean matches(String name, Metric metric) {

        final String nameLowerCase = name.trim().toLowerCase();

        return allowList.stream()
                .anyMatch(item -> nameLowerCase.contains(item) || "*".equals(item));
    }
}
