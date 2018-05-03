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

package com.adobe.aam.metrics.filter;

import com.adobe.aam.metrics.core.ImmutableMetricSnapshot;
import com.adobe.aam.metrics.core.MetricSnapshot;
import com.adobe.aam.metrics.metric.ImmutableTags;
import com.adobe.aam.metrics.metric.Metric;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class WhitelistMetricFilterTest {

	@Test
	public void testWhitelistAll() {
		List<String> whitelist = Lists.newArrayList();
		whitelist.add("*");
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertTrue(filter.isAllowed(metric("metric.name")));
	}

	@Test
	public void testWhitelistWithValidItems() {
		List<String> whitelist = Lists.newArrayList();
		whitelist.add("good");
		whitelist.add("better");
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertTrue(filter.isAllowed(metric("metric.good.name")));
		Assert.assertTrue(filter.isAllowed(metric("metric.better.name")));
		Assert.assertFalse(filter.isAllowed(metric("metric.bad.name")));
	}

	@Test
	public void testCaseInsensitive() {
		List<String> whitelist = Lists.newArrayList();
		whitelist.add("GooD");
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertTrue(filter.isAllowed(metric("metric.good.name")));
		Assert.assertTrue(filter.isAllowed(metric("metric.gOOd.name")));
		Assert.assertTrue(filter.isAllowed(metric("metric.gOOD.name")));
		Assert.assertFalse(filter.isAllowed(metric("metric.bad.name")));
	}

	@Test
	public void testEmptyWhitelist() {
		List<String> whitelist = Lists.newArrayList();
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertFalse(filter.isAllowed(metric("metric.name")));
		Assert.assertFalse(filter.isAllowed(metric("other.metric.name")));
	}

	@Test
	public void testBlankItem() {
		List<String> whitelist = Lists.newArrayList();
		whitelist.add("good");
		whitelist.add("");
		whitelist.add("  ");
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertTrue(filter.isAllowed(metric("metric.good.name")));
		Assert.assertFalse(filter.isAllowed(metric("metric.name")));
	}

	@Test
	public void testItemWithWhitespaces() {
		List<String> whitelist = Lists.newArrayList();
		whitelist.add("good \t");
		WhitelistMetricFilter filter = new WhitelistMetricFilter(whitelist);
		Assert.assertFalse(filter.isAllowed(metric("metric.name")));
		Assert.assertTrue(filter.isAllowed(metric("metric.good.name")));
	}

	private MetricSnapshot metric(String name) {
		return ImmutableMetricSnapshot.builder()
				.name(name)
				.type(Metric.Type.COUNT)
				.timestamp(1000)
				.value(10)
				.tags(ImmutableTags.builder().build())
				.build();
	}
}
