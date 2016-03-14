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
import com.crashlytics.android.answers.PurchaseEvent;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trevor
 */
public class PurchaseLoggerTest extends LoggerBaseTest
{
	PurchaseLogger logger;
	AnalyticsEvent purchaseEvent;

	@Override
	public void setup()
	{
		super.setup();
		logger = new PurchaseLogger(answers);
		purchaseEvent = new AnalyticsEvent(PredefinedEvents.PURCHASE).putAttribute(Attributes.Purchase.ITEM_PRICE, BigDecimal.valueOf(374.99))
		                                                             .putAttribute(Attributes.Purchase.CURRENCY, Currency.getInstance("USD"))
		                                                             .putAttribute(Attributes.Purchase.ITEM_NAME, "That thing I really need")
		                                                             .putAttribute(Attributes.Purchase.ITEM_TYPE, "Fancy Electronics")
		                                                             .putAttribute(Attributes.Purchase.ITEM_ID, "ID10T")
		                                                             .putAttribute(Attributes.Purchase.SUCCESS, true)
		                                                             .putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_buildAnswersPurchaseEvent()
	{
		PurchaseEvent result = logger.buildAnswersPurchaseEvent(purchaseEvent);

		Map<String, Object> predefinedAttributes = PackageScopeWrappedCalls.getPredefinedAttributes(result);
		assertThat(predefinedAttributes.size()).isEqualTo(6);

		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.ITEM_PRICE);
		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.CURRENCY);
		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.ITEM_NAME);
		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.ITEM_TYPE);
		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.ITEM_ID);
		assertThat(predefinedAttributes).containsKey(Attributes.Purchase.SUCCESS);

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(result);
		assertThat(customAttributes.size()).isEqualTo(1);

		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}

	@Test
	public void test_logSpecificEvent()
	{
		logger.logSpecificEvent(purchaseEvent);

		assertThat(answers.logPurchaseCalled).isTrue();
		assertThat(answers.purchaseEvent).isNotNull();
	}
}
