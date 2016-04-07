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

package com.busybusy.answers_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.crashlytics.android.answers.MockAnswers;
import com.crashlytics.android.answers.PackageScopeWrappedCalls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link AnswersProvider} class
 * @author Trevor
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 21, constants = com.busybusy.answers_provider.BuildConfig.class)
public class AnswersProviderTest
{
	static String CUSTOM_KEY  = "CustomFieldKey";
	static String CUSTOM_DATA = "CustomData";

	MockAnswers     answers;
	AnswersProvider provider;
	AnalyticsEvent  event;
	AnalyticsEvent  customEvent;

	@Before
	public void setup()
	{
		answers = new MockAnswers();
		provider = new AnswersProvider(answers);
		event = new AnalyticsEvent(PredefinedEvents.ADD_TO_CART).putAttribute(Attributes.AddToCart.ITEM_PRICE, BigDecimal.valueOf(374.99))
		                                                        .putAttribute(Attributes.AddToCart.CURRENCY, Currency.getInstance("USD"))
		                                                        .putAttribute(Attributes.AddToCart.ITEM_NAME, "That thing I really need")
		                                                        .putAttribute(Attributes.AddToCart.ITEM_TYPE, "Fancy Electronics")
		                                                        .putAttribute(Attributes.AddToCart.ITEM_ID, "ID10T")
		                                                        .putAttribute(CUSTOM_KEY, CUSTOM_DATA);

		customEvent = new AnalyticsEvent("CustomEventName").putAttribute(CUSTOM_KEY, CUSTOM_DATA);
	}

	@Test
	public void test_setPriorityFilter()
	{
		AnalyticsKitProvider.PriorityFilter filter = new AnalyticsKitProvider.PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return false;
			}
		};
		provider.setPriorityFilter(filter);

		assertThat(provider.getPriorityFilter()).isEqualTo(filter);
	}

	@Test
	public void test_priorityFiltering_default()
	{
		AnalyticsEvent event = new AnalyticsEvent(PredefinedEvents.LEVEL_END)
				.setPriority(10)
				.send();
		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();

		event.setPriority(-9)
		     .send();
		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void test_priorityFiltering_custom()
	{
		provider.setPriorityFilter(new AnalyticsKitProvider.PriorityFilter()
		{
			@Override
			public boolean shouldLog(int priorityLevel)
			{
				return priorityLevel < 10;
			}
		});

		AnalyticsEvent event = new AnalyticsEvent(PredefinedEvents.LEVEL_END)
				.setPriority(10)
				.send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

		assertThat(answers.logLevelEndCalled).isFalse();
		assertThat(answers.addToCartEvent).isNull();

		event.setPriority(9)
				.send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void test_getPriorityFilter()
	{
		assertThat(provider.getPriorityFilter()).isNotNull();
	}

	@Test
	public void test_sendEvent_Predefined()
	{
		provider.sendEvent(event);

		assertThat(answers.logAddToCartCalled).isTrue();
		assertThat(answers.addToCartEvent).isNotNull();
	}

	@Test
	public void test_sendEvent_PredefinedTimed()
	{
		event.setTimed(true);

		provider.sendEvent(event);

		assertThat(answers.logAddToCartCalled).isFalse();
		assertThat(answers.addToCartEvent).isNull();

		provider.endTimedEvent(event);

		assertThat(answers.logAddToCartCalled).isTrue();
		assertThat(answers.addToCartEvent).isNotNull();
		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(answers.addToCartEvent);

		assertThat(customAttributes).containsKey(Attributes.EVENT_DURATION);
	}

	@Test
	public void test_endTimedEvent_WillThrow()
	{
		boolean didThrow = false;

		try
		{
			provider.endTimedEvent(event);
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}

	@Test
	public void test_sendEvent_Custom()
	{
		provider.sendEvent(customEvent);

		assertThat(answers.logCustomCalled).isTrue();
		assertThat(answers.customEvent).isNotNull();

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(answers.customEvent);
		assertThat(customAttributes).containsKey(CUSTOM_KEY);
	}


	@Test
	public void test_sendEvent_CustomTimed()
	{
		customEvent.setTimed(true);
		provider.sendEvent(customEvent);

		assertThat(answers.logCustomCalled).isFalse();
		assertThat(answers.customEvent).isNull();

		provider.endTimedEvent(customEvent);

		assertThat(answers.logCustomCalled).isTrue();
		assertThat(answers.customEvent).isNotNull();

		Map<String, Object> customAttributes = PackageScopeWrappedCalls.getCustomAttributes(answers.customEvent);
		assertThat(customAttributes).containsKey(CUSTOM_KEY);
		assertThat(customAttributes).containsKey(Attributes.EVENT_DURATION);
	}
}
