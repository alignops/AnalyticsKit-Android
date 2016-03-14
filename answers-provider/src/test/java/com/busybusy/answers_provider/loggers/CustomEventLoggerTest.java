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
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.PackageScopeWrappedCalls;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class CustomEventLoggerTest extends LoggerBaseTest
{
	CustomEventLogger logger;
	AnalyticsEvent    customEvent;

	@Before
	public void setup()
	{
		super.setup();

		logger = new CustomEventLogger(answers);
		customEvent = new AnalyticsEvent("This_can_be_anything!").putAttribute("attr_1", 1337D)
		                                                         .putAttribute("attr_2", "test_status_code");
	}

	@Test
	public void test_buildCustomAnswersEvent()
	{
		CustomEvent         result     = logger.buildCustomAnswersEvent(customEvent);
		Map<String, Object> attributes = PackageScopeWrappedCalls.getCustomAttributes(result);

		assertThat(attributes).hasSize(2);

		assertThat(attributes).containsKey("attr_1");
		assertThat(attributes.get("attr_1")).isEqualTo(Double.toString(1337D));

		assertThat(attributes).containsKey("attr_2");
		assertThat(attributes.get("attr_2")).isEqualTo("test_status_code");
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(customEvent);

		assertThat(answers.logCustomCalled).isTrue();
		assertThat(answers.customEvent).isNotNull();
	}
}
