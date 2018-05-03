package com.adobe.aam.metrics.codahale;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CodahaleWhitelistMetricFilter implements MetricFilter {

    private final List<String> whitelist;

    public CodahaleWhitelistMetricFilter(List<String> whitelist) {
        this.whitelist = whitelist
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(item -> item.toLowerCase().trim())
                .collect(Collectors.toList());
    }

    @Override
    public boolean matches(String name, Metric metric) {

        final String nameLowerCase = name.trim().toLowerCase();

        return whitelist.stream()
                .anyMatch(item -> nameLowerCase.contains(item) || "*".equals(item));
    }
}
