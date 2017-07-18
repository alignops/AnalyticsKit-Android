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

package com.busybusy.analyticskit_android;

import android.support.annotation.NonNull;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests the {@link AnalyticsKit} class.
 *
 * @author John Hunt on 3/7/16.
 */
public class AnalyticsKitTest
{
	@Test
	public void testGetInstance()
	{
		AnalyticsKit analyticsKit = AnalyticsKit.getInstance();
		assertThat(analyticsKit).isNotNull();
		assertThat(analyticsKit).isEqualTo(AnalyticsKit.getInstance());
	}

	@Test
	public void testRegisterProvider()
	{
		AnalyticsKitProvider provider = new AnalyticsKitProvider()
		{
			@NonNull
			@Override
			public PriorityFilter getPriorityFilter()
			{
				return new PriorityFilter()
				{
					@Override
					public boolean shouldLog(int priorityLevel)
					{
						return true;
					}
				};
			}

			@Override
			public void sendEvent(@NonNull AnalyticsEvent event)
			{
				// do nothing
			}

			@Override
			public void endTimedEvent(@NonNull AnalyticsEvent timedEvent)
			{
				// do nothing
			}
		};

		AnalyticsKit.getInstance().registerProvider(provider);

		assertThat(AnalyticsKit.getInstance().providers).contains(provider);
	}

	@Test
	public void testPriorityFiltering_multiple()
	{
		MockProvider flurryProvider    = new MockProvider().setPriorityUpperBound(0);
		MockProvider customProviderOne = new MockProvider().setPriorityUpperBound(3);
		MockProvider customProviderTwo = new MockProvider().setPriorityUpperBound(5);

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider)
		            .registerProvider(customProviderOne)
		            .registerProvider(customProviderTwo);

		String eventName1 = "Custom Providers only";
		AnalyticsEvent flurryAndCustom1 = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setPriority(2)
				.send();

		assertThat(flurryProvider.sentEvents).isEmpty();
		assertThat(customProviderOne.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(customProviderOne.sentEvents.values()).containsExactly(flurryAndCustom1);
		assertThat(customProviderTwo.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(customProviderTwo.sentEvents.values()).containsExactly(flurryAndCustom1);
	}

	@Test
	public void testPriorityFiltering_none()
	{
		MockProvider flurryProvider    = new MockProvider();
		MockProvider customProviderOne = new MockProvider();
		MockProvider customProviderTwo = new MockProvider();

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider)
		            .registerProvider(customProviderOne)
		            .registerProvider(customProviderTwo);

		String eventName1 = "Flurry and Custom 1 only";
		new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setPriority(1)
				.send();

		assertThat(flurryProvider.sentEvents).isEmpty();
		assertThat(customProviderOne.sentEvents).isEmpty();
		assertThat(customProviderTwo.sentEvents).isEmpty();
	}

	@Test
	public void testTimedEvent()
	{
		MockProvider flurryProvider = new MockProvider();
		AnalyticsKit.getInstance().registerProvider(flurryProvider);

		String eventName1 = "Hello event";
		AnalyticsEvent flurryEvent = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setTimed(true)
				.send();

		assertThat(flurryProvider.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(flurryProvider.sentEvents.values()).containsExactly(flurryEvent);

		try
		{
			Thread.sleep(150, 0);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		AnalyticsKit.getInstance().endTimedEvent(flurryEvent);

		assertThat(flurryEvent.getAttribute(MockProvider.EVENT_DURATION)).isNotNull();
		Long duration = (Long) flurryEvent.getAttribute(MockProvider.EVENT_DURATION);
		assertThat(duration).isNotNull();
		assertThat(duration).isGreaterThanOrEqualTo(150L);
	}

	@Test
	public void testTimedEvent_manyProviders()
	{
		MockProvider flurryProvider    = new MockProvider();
		MockProvider customProviderOne = new MockProvider();
		MockProvider customProviderTwo = new MockProvider();

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider)
		            .registerProvider(customProviderOne)
		            .registerProvider(customProviderTwo);

		String eventName1 = "Flurry only event";
		AnalyticsEvent flurryEvent = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setTimed(true)
				.send();

		assertThat(flurryProvider.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(flurryProvider.sentEvents.values()).containsExactly(flurryEvent);
		assertThat(customProviderOne.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(customProviderOne.sentEvents.values()).containsExactly(flurryEvent);
		assertThat(customProviderTwo.sentEvents.keySet()).containsExactly(eventName1);
		assertThat(customProviderTwo.sentEvents.values()).containsExactly(flurryEvent);

		try
		{
			Thread.sleep(150, 0);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		AnalyticsKit.getInstance().endTimedEvent(flurryEvent);

		assertThat(flurryEvent.getAttribute(MockProvider.EVENT_DURATION)).isNotNull();
		Long duration = (Long) flurryEvent.getAttribute(MockProvider.EVENT_DURATION);
		assertThat(duration).isNotNull();
		assertThat(duration).isGreaterThanOrEqualTo(150L);
	}

	@Test
	public void test_endTimeEvent_willThrow() throws Exception
	{
		MockProvider flurryProvider = new MockProvider();

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider);

		String eventName1 = "throwEvent";
		AnalyticsEvent flurryEvent = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.send();

		boolean didThrow = false;
		try
		{
			AnalyticsKit.getInstance().endTimedEvent(flurryEvent);
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}

	@Test
	public void test_endTimeEvent_willThrow_innerCase() throws Exception
	{
		MockProvider flurryProvider = new MockProvider();

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider);

		String eventName1 = "normalEvent";
		//noinspection unused
		AnalyticsEvent normalEvent = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setTimed(true)
				.send();

		String eventName2 = "throwEvent";
		AnalyticsEvent throwEvent = new AnalyticsEvent(eventName2)
				.putAttribute("hello", "world")
				.send();

		boolean didThrow = false;
		try
		{
			AnalyticsKit.getInstance().endTimedEvent(throwEvent);
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}
}
