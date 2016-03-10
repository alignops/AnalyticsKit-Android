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

	public static final int CUSTOM_PROVIDER_1 = 0x1;
	public static final int CUSTOM_PROVIDER_2 = 0x2;

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
			@Override
			public int getType()
			{
				return Providers.ANSWERS;
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
	public void testProviderFiltering()
	{
		MockProvider flurryProvider = new MockProvider(Providers.FLURRY);
		MockProvider customProviderOne = new MockProvider(CUSTOM_PROVIDER_1);
		MockProvider customProviderTwo = new MockProvider(CUSTOM_PROVIDER_2);

		AnalyticsKit.getInstance()
		            .registerProvider(flurryProvider)
		            .registerProvider(customProviderOne)
		            .registerProvider(customProviderTwo);

		String eventName1 = "Flurry and Custom 1 only";
		AnalyticsEvent flurryAndCustom1 = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.specifyProviders(Providers.FLURRY | CUSTOM_PROVIDER_1)
				.send();

		assertTrue(flurryProvider.sentEvents.containsKey(eventName1));
		assertTrue(flurryProvider.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(1, flurryProvider.sentEvents.size());
		assertTrue(customProviderOne.sentEvents.containsKey(eventName1));
		assertTrue(customProviderOne.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(1, customProviderOne.sentEvents.size());
		assertFalse(customProviderTwo.sentEvents.containsKey(eventName1));
		assertFalse(customProviderTwo.sentEvents.containsValue(flurryAndCustom1));
		assertEquals(0, customProviderTwo.sentEvents.size());
	}

	@Test
	public void testTimedEvent()
	{
		MockProvider flurryProvider = new MockProvider(Providers.FLURRY);
		AnalyticsKit.getInstance().registerProvider(flurryProvider);

		String eventName1 = "Flurry only event";
		AnalyticsEvent flurryEvent = new AnalyticsEvent(eventName1)
				.putAttribute("hello", "world")
				.addProvider(Providers.FLURRY)
				.setTimed(true)
				.send();

		assertTrue(flurryProvider.sentEvents.containsKey(eventName1));
		assertTrue(flurryProvider.sentEvents.containsValue(flurryEvent));
		assertEquals(1, flurryProvider.sentEvents.size());

		try
		{
			Thread.sleep(350, 0);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		AnalyticsKit.getInstance().endTimedEvent(flurryEvent);

		assertNotNull(flurryEvent.getAttribute(MockProvider.EVENT_DURATION));
		Long duration = (Long) flurryEvent.getAttribute(MockProvider.EVENT_DURATION);
		assertNotNull(duration);
		assertTrue(duration >= 350);
	}
}
