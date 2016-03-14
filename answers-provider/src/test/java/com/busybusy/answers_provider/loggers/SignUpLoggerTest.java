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
import com.crashlytics.android.answers.SignUpEvent;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class SignUpLoggerTest extends LoggerBaseTest
{
	SignUpLogger   logger;
	AnalyticsEvent signupEvent;

	@Override
	public void setup()
	{
		super.setup();
		logger = new SignUpLogger(answers);
		signupEvent = new AnalyticsEvent(PredefinedEvents.SIGN_UP).putAttribute(Attributes.SignUp.METHOD, "MethodName")
		                                                          .putAttribute(Attributes.SignUp.SUCCESS, false)
		                                                          .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_buildAnswersSignUpEvent()
	{
		SignUpEvent result = logger.buildAnswersSignUpEvent(signupEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(2);

		assertThat(predefinedAttributes).containsKey(Attributes.SignUp.METHOD);
		assertThat(predefinedAttributes).containsKey(Attributes.SignUp.SUCCESS);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);

		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(signupEvent);

		assertThat(answers.logSignUpCalled).isTrue();
		assertThat(answers.signUpEvent).isNotNull();
	}
}
