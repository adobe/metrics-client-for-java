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

import com.adobe.aam.metrics.metric.Metric;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Case insensitive filter that blocks the desired patterns.
 * This filter uses REGEX and contains when checking each pattern.
 */
public class BlockListMetricFilter extends SimpleRegexMetricFilter {

	private final List<String> blockList;

	public BlockListMetricFilter(List<String> blockList) {
		this.blockList = blockList
				.stream()
				.filter(StringUtils::isNotBlank)
				.map(item -> item.toLowerCase().trim())
				.collect(Collectors.toList());
	}

	@Override
	public boolean isAllowed(Metric metric) {
		return blockList.stream()
				.noneMatch(filter -> super.filterMatches(filter, metric));
	}

	@Override
	public String toString() {
		return "BlockListMetricFilter{" +
				"blocklist=" + blockList +
				'}';
	}
}
