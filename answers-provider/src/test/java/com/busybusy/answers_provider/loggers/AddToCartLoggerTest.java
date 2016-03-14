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
import com.crashlytics.android.answers.AddToCartEvent;
import com.crashlytics.android.answers.PackageScopeWrappedCalls;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class AddToCartLoggerTest extends LoggerBaseTest
{
	AddToCartLogger logger;
	AnalyticsEvent  addToCartEvent;

	@Override
	public void setup()
	{
		super.setup();

		logger = new AddToCartLogger(answers);
		addToCartEvent = new AnalyticsEvent(PredefinedEvents.ADD_TO_CART).putAttribute(Attributes.AddToCart.ITEM_PRICE, BigDecimal.valueOf(374.99))
		                                                                 .putAttribute(Attributes.AddToCart.CURRENCY, Currency.getInstance("USD"))
		                                                                 .putAttribute(Attributes.AddToCart.ITEM_NAME, "That thing I really need")
		                                                                 .putAttribute(Attributes.AddToCart.ITEM_TYPE, "Fancy Electronics")
		                                                                 .putAttribute(Attributes.AddToCart.ITEM_ID, "ID10T")
		                                                                 .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_buildAnswersAddToCartEvent()
	{
		AddToCartEvent result = logger.buildAnswersAddToCartEvent(addToCartEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(5);

		assertThat(predefinedAttributes).containsKey(Attributes.AddToCart.ITEM_PRICE);
		assertThat(predefinedAttributes).containsKey(Attributes.AddToCart.CURRENCY);
		assertThat(predefinedAttributes).containsKey(Attributes.AddToCart.ITEM_NAME);
		assertThat(predefinedAttributes).containsKey(Attributes.AddToCart.ITEM_TYPE);
		assertThat(predefinedAttributes).containsKey(Attributes.AddToCart.ITEM_ID);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);

		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(addToCartEvent);

		assertThat(answers.logAddToCartCalled).isTrue();
		assertThat(answers.addToCartEvent).isNotNull();
	}
}
