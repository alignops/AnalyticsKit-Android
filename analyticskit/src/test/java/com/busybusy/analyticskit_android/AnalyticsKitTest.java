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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests the {@link AnalyticsKit} class
 * @author John Hunt on 3/7/16.
 */
public class AnalyticsKitTest
{
	@Test
	public void testGetInstance()
	{
		AnalyticsKit analyticsKit = AnalyticsKit.getInstance();
		assertNotNull(analyticsKit);
		assertEquals(analyticsKit, AnalyticsKit.getInstance());
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

		assertTrue(AnalyticsKit.getInstance().providers.contains(provider));
	}

	@Test
	public void testPriorityFiltering_multiple()
	{
		MockProvider flurryProvider = new MockProvider().setPriorityUpperBound(0);
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

		assertFalse(flurryProvider.sentEvents.containsKey(eventName1));
		assertFalse(flurryProvider.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(0, flurryProvider.sentEvents.size());
		assertTrue(customProviderOne.sentEvents.containsKey(eventName1));
		assertTrue(customProviderOne.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(1, customProviderOne.sentEvents.size());
		assertTrue(customProviderTwo.sentEvents.containsKey(eventName1));
		assertTrue(customProviderTwo.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(1, customProviderTwo.sentEvents.size());
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
		AnalyticsEvent flurryAndCustom1 = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.setPriority(1)
				.send();

		assertFalse(flurryProvider.sentEvents.containsKey(eventName1));
		assertFalse(flurryProvider.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(0, flurryProvider.sentEvents.size());
		assertFalse(customProviderOne.sentEvents.containsKey(eventName1));
		assertFalse(customProviderOne.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(0, customProviderOne.sentEvents.size());
		assertFalse(customProviderTwo.sentEvents.containsKey(eventName1));
		assertFalse(customProviderTwo.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(0, customProviderTwo.sentEvents.size());
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

		assertTrue(flurryProvider.sentEvents.containsKey(eventName1));
		assertTrue(flurryProvider.sentEvents.containsValue(flurryEvent));
		assertEquals(1, flurryProvider.sentEvents.size());

		try
		{
			Thread.sleep(150, 0);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		AnalyticsKit.getInstance().endTimedEvent(flurryEvent);

		assertNotNull(flurryEvent.getAttribute(MockProvider.EVENT_DURATION));
		Long duration = (Long) flurryEvent.getAttribute(MockProvider.EVENT_DURATION);
		assertNotNull(duration);
		assertTrue(duration >= 150);
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

		assertTrue(flurryProvider.sentEvents.containsKey(eventName1));
		assertTrue(flurryProvider.sentEvents.containsValue(flurryEvent));
		assertEquals(1, flurryProvider.sentEvents.size());
		assertTrue(customProviderOne.sentEvents.containsKey(eventName1));
		assertTrue(customProviderOne.sentEvents.containsValue(flurryEvent));
		assertEquals(1, customProviderOne.sentEvents.size());
		assertTrue(customProviderTwo.sentEvents.containsKey(eventName1));
		assertTrue(customProviderTwo.sentEvents.containsValue(flurryEvent));
		assertEquals(1, customProviderTwo.sentEvents.size());

		try
		{
			Thread.sleep(150, 0);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		AnalyticsKit.getInstance().endTimedEvent(flurryEvent);

		assertNotNull(flurryEvent.getAttribute(MockProvider.EVENT_DURATION));
		Long duration = (Long) flurryEvent.getAttribute(MockProvider.EVENT_DURATION);
		assertNotNull(duration);
		assertTrue(duration >= 150);
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

		assertTrue(didThrow);
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

		assertTrue(didThrow);
	}
}
