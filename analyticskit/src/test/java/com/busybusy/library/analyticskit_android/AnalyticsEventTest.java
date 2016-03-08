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

package com.busybusy.library.analyticskit_android;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Tests the {@link AnalyticsEvent} class
 * @author John Hunt on 3/5/16.
 */
public class AnalyticsEventTest
{
	@Test
	public void testBuilder_withAttributes()
	{
		String name = "Yeah, this should work";
		String animalText = "The quick brown fox jumps over the lazy dog";
		AnalyticsEvent event = new AnalyticsEvent(name)
				.putAttribute("the answer", 42)
				.putAttribute("animal text", animalText);

		assertEquals(event.name(), name);
		HashMap attributes = event.getAttributes();
		assertNotNull(attributes);
		assertEquals(attributes.get("the answer"), 42);
		assertEquals(attributes.get("animal text"), animalText);
		assertNull(attributes.get("nonexistent key"));
	}

	@Test
	public void testBuilder_noAttributes()
	{
		String name = "Yeah, this should work";
		AnalyticsEvent event = new AnalyticsEvent(name);

		assertEquals(event.name(), name);
		assertNull(event.getAttributes());
	}

	@Test
	public void testBuilder_specifyProviders()
	{
		String name = "Answers only event";
		AnalyticsEvent event = new AnalyticsEvent(name)
				.specifyProviders(Providers.ANSWERS);

		assertEquals(event.name(), name);
		assertNull(event.getAttributes());
		assertTrue((event.providersMask & Providers.ANSWERS) != 0);
		assertFalse((event.providersMask & Providers.GOOGLE_ANALYTICS) != 0);

		name = "Answers and Mixpanel only event";
		event = new AnalyticsEvent(name)
				.specifyProviders(Providers.ANSWERS | Providers.MIXPANEL);

		assertEquals(event.name(), name);
		assertNull(event.getAttributes());
		assertTrue((event.providersMask & Providers.ANSWERS) != 0);
		assertTrue((event.providersMask & Providers.MIXPANEL) != 0);
		assertFalse((event.providersMask & Providers.GOOGLE_ANALYTICS) != 0);
	}

	@Test
	public void testBuilder_addProviders()
	{
		String name = "Answers only event";
		AnalyticsEvent event = new AnalyticsEvent(name)
				.addProvider(Providers.ANSWERS);

		assertEquals(event.name(), name);
		assertNull(event.getAttributes());
		assertTrue((event.providersMask & Providers.ANSWERS) != 0);
		assertFalse((event.providersMask & Providers.GOOGLE_ANALYTICS) != 0);

		name = "Answers and Mixpanel only event";
		event = new AnalyticsEvent(name)
				.addProvider(Providers.ANSWERS)
				.addProvider(Providers.MIXPANEL);

		assertEquals(event.name(), name);
		assertNull(event.getAttributes());
		assertTrue((event.providersMask & Providers.ANSWERS) != 0);
		assertTrue((event.providersMask & Providers.MIXPANEL) != 0);
		assertFalse((event.providersMask & Providers.GOOGLE_ANALYTICS) != 0);
	}
}