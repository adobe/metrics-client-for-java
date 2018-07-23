package com.adobe.aam.metrics.core.config;

import org.immutables.value.Value;

import java.util.Map;
import java.util.regex.Pattern;

@Value.Immutable
public interface RelabelConfig {
    Pattern regex();
    Map<Integer, String> groupToLabelName();
}
