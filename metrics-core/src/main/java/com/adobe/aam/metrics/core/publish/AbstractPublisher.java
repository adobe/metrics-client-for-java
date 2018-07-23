package com.adobe.aam.metrics.core.publish;

import com.adobe.aam.metrics.core.config.PublisherConfig;
import com.adobe.aam.metrics.metric.Metric;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPublisher implements Publisher {

    private final PublisherConfig config;
    private final ResetCounterHelper resetCounterHelper;

    public AbstractPublisher(PublisherConfig config) {
        this.config = config;
        this.resetCounterHelper = config.resetCounters() ? new ResetCounterHelperImpl() : new ResetCounterHelperNoop();
    }

    @Override
    public void publishMetrics(Collection<Metric> metrics) throws IOException {
        Iterator<Metric> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            doPublishMetrics(getNextBatch(iterator));
        }
    }

    private Collection<Metric> getNextBatch(Iterator<Metric> iterator) {
        List<Metric> batch = Lists.newArrayList();
        while (iterator.hasNext() && batch.size() < nonEmptyBatchSize()) {
            Metric metric = iterator.next();
            if (shouldKeep(metric)) {
                batch.add(metric);
            }
        }
        return batch;
    }

    public abstract void doPublishMetrics(Collection<Metric> metrics) throws IOException;

    private int nonEmptyBatchSize() {
        return config.batchSize() <= 0 ? 500 : config.batchSize();
    }

    private boolean shouldKeep(Metric metric) {
        return !config().sendOnlyRecentlyUpdatedMetrics()
                || System.currentTimeMillis() - metric.getLastTrackTime() <= config().publishFrequencyMs();
    }

    public double getMetricValue(Metric metric) {
        return config().resetCounters() ? resetCounterHelper.resetIfCounter(metric) : metric.get();
    }

    public PublisherConfig config() {
        return config;
    }
}
