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
import com.crashlytics.android.answers.StartCheckoutEvent;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class StartCheckoutLoggerTest extends LoggerBaseTest
{
	StartCheckoutLogger logger;
	AnalyticsEvent      startCheckoutEvent;

	@Override
	public void setup()
	{
		super.setup();
		logger = new StartCheckoutLogger(answers);
		startCheckoutEvent = new AnalyticsEvent(PredefinedEvents.START_CHECKOUT).putAttribute(Attributes.StartCheckout.TOTAL_PRICE, BigDecimal.valueOf(374.99))
		                                                                        .putAttribute(Attributes.StartCheckout.CURRENCY, Currency.getInstance("USD"))
		                                                                        .putAttribute(Attributes.StartCheckout.ITEM_COUNT, 1)
		                                                                        .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}


	@Test
	public void test_buildAnswersStartCheckoutEvent()
	{
		StartCheckoutEvent result = logger.buildAnswersStartCheckoutEvent(startCheckoutEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(3);

		assertThat(predefinedAttributes).containsKey(Attributes.StartCheckout.TOTAL_PRICE);
		assertThat(predefinedAttributes).containsKey(Attributes.StartCheckout.CURRENCY);
		assertThat(predefinedAttributes).containsKey(Attributes.StartCheckout.ITEM_COUNT);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);

		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(startCheckoutEvent);

		assertThat(answers.logStartCheckoutCalled).isTrue();
		assertThat(answers.startCheckoutEvent).isNotNull();
	}
}
