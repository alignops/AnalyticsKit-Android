/*
 * Copyright 2016 Busy, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.busybusy.answers_provider.loggers;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.answers_provider.Attributes;
import com.busybusy.answers_provider.PredefinedEvents;
import com.crashlytics.android.answers.PackageScopeWrappedCalls;
import com.crashlytics.android.answers.SearchEvent;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO: put some meaningful information about this class in the javadoc or delete it
 *
 * @author Trevor
 */
public class SearchLoggerTest extends LoggerBaseTest
{
	SearchLogger   logger;
	AnalyticsEvent searchEvent;

	@Override
	public void setup()
	{
		super.setup();
		logger = new SearchLogger(answers);
		searchEvent = new AnalyticsEvent(PredefinedEvents.SEARCH).putAttribute(Attributes.Search.QUERY, "SearchQuery")
		                                                         .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_buildAnswersSearchEvent()
	{
		SearchEvent result = logger.buildAnswersSearchEvent(searchEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(1);

		assertThat(predefinedAttributes).containsKey(Attributes.Search.QUERY);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);

		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(searchEvent);

		assertThat(answers.logSearchCalled).isTrue();
		assertThat(answers.searchEvent).isNotNull();
	}
}
