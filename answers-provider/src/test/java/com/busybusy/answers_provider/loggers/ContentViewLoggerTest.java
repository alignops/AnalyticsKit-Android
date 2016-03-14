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
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.PackageScopeWrappedCalls;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class ContentViewLoggerTest extends LoggerBaseTest
{
	ContentViewLogger logger;
	AnalyticsEvent    contentViewEvent;

	@Override
	public void setup()
	{
		super.setup();

		logger = new ContentViewLogger(answers);

		contentViewEvent = new AnalyticsEvent(PredefinedEvents.CONTENT_VIEW).putAttribute(Attributes.ContentView.CONTENT_NAME, "goodStuff")
		                                                                    .putAttribute(Attributes.ContentView.CONTENT_TYPE, "ofHighQuality")
		                                                                    .putAttribute(Attributes.CONTENT_ID, "lvl-10")
		                                                                    .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_buildAnswersContentViewEvent()
	{
		ContentViewEvent result = logger.buildAnswersContentViewEvent(contentViewEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(3);
		assertThat(predefinedAttributes).containsKey(Attributes.ContentView.CONTENT_NAME);
		assertThat(predefinedAttributes).containsKey(Attributes.ContentView.CONTENT_TYPE);
		assertThat(predefinedAttributes).containsKey(Attributes.ContentView.CONTENT_ID);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);
		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(contentViewEvent);

		assertThat(answers.logContentViewCalled).isTrue();
		assertThat(answers.contentViewEvent).isNotNull();
	}
}
