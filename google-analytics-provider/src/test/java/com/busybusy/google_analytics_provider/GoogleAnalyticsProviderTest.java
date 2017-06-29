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

package com.busybusy.google_analytics_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.busybusy.analyticskit_android.ContentViewEvent;
import com.busybusy.analyticskit_android.ErrorEvent;
import com.google.android.gms.analytics.Tracker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link GoogleAnalyticsProvider} class
 *
 * @author John Hunt on 3/21/16.
 */
public class GoogleAnalyticsProviderTest
{
	GoogleAnalyticsProvider provider;
	Tracker tracker;

	final String mockTrackingId = "mocked_tracking_id";
	Map<String, String> testEventPropertiesMap;
	boolean sendCalled;

	@Before
	public void setup()
	{
		// Mock behavior for when Tracker.send(Map<String, String>) is called
		tracker = mock(Tracker.class);
		doAnswer(new Answer<Void>()
		{
			public Void answer(InvocationOnMock invocation)
			{
				Object[] args = invocation.getArguments();
				//noinspection unchecked
				testEventPropertiesMap = (Map<String, String>) args[0];
				sendCalled = true;
				return null;
			}
		}).when(tracker).send(anyMapOf(String.class, String.class));

		provider = new GoogleAnalyticsProvider(tracker);

		sendCalled = false;
		testEventPropertiesMap = null;
	}

	@Test
	public void testSetAndGetPriorityFilter()
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
		AnalyticsEvent event = new AnalyticsEvent("Forecast: Event Flurries")
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

		AnalyticsEvent event = new AnalyticsEvent("Forecast: Event Flurries")
				.setPriority(10)
				.send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

		event.setPriority(9)
		     .send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void testStringifyAttributesMap_noParams()
	{
		Map<String, String> stringAttributes = provider.stringifyAttributesMap(null);
		assertNull(stringAttributes);
	}

	@Test
	public void testStringifyAttributesMap_validParams()
	{
		HashMap<String, Object> eventParams = new HashMap<>();
		eventParams.put("favorite_color", "Blue");
		eventParams.put("favorite_number", 42);
		Map<String, String> stringAttributes = provider.stringifyAttributesMap(eventParams);
		assertNotNull(stringAttributes);
		assertThat(stringAttributes.containsKey("favorite_color"));
		assertThat(stringAttributes.get("favorite_color")).isEqualTo("Blue");
		assertThat(stringAttributes.containsKey("favorite_number"));
		assertThat(stringAttributes.get("favorite_number")).isEqualTo("42");
	}

	@Test
	public void testSendEvent_unTimed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Test Run");
		provider.sendEvent(event);

		assertTrue(sendCalled);
		assertNotNull(testEventPropertiesMap);
		assertEquals(3, testEventPropertiesMap.size());

		assertEquals("Google Analytics Test Run", testEventPropertiesMap.get("&ea"));
		assertEquals("User Event", testEventPropertiesMap.get("&ec"));
		assertEquals("event", testEventPropertiesMap.get("&t"));
	}

	@Test
	public void testSendEvent_unTimed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Event With Params Run")
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);

		assertTrue(sendCalled);
		assertNotNull(testEventPropertiesMap);
		assertEquals(5, testEventPropertiesMap.size());

		assertEquals("Google Analytics Event With Params Run", testEventPropertiesMap.get("&ea"));
		assertEquals("User Event", testEventPropertiesMap.get("&ec"));
		assertEquals("event", testEventPropertiesMap.get("&t"));

		assertTrue(testEventPropertiesMap.containsKey("some_param"));
		assertEquals("yes", testEventPropertiesMap.get("some_param"));
		assertTrue(testEventPropertiesMap.containsKey("another_param"));
		assertEquals("yes again", testEventPropertiesMap.get("another_param"));
	}

	@Test
	public void testLogContentViewEvent()
	{
		ContentViewEvent event = new ContentViewEvent("Test page 7");
		provider.sendEvent(event);

		assertTrue(sendCalled);
		assertNotNull(testEventPropertiesMap);
		assertEquals(2, testEventPropertiesMap.size());

		//noinspection SpellCheckingInspection
		assertEquals("screenview", testEventPropertiesMap.get("&t"));
		assertEquals("Test page 7", testEventPropertiesMap.get("contentName"));
	}

	@Test
	public void testLogErrorEvent()
	{
		ErrorEvent event = new ErrorEvent()
				.setMessage("something bad happened")
				.setException(new EmptyStackException());
		provider.sendEvent(event);

		assertTrue(sendCalled);
		assertNotNull(testEventPropertiesMap);
		assertEquals(5, testEventPropertiesMap.size());

		assertEquals("something bad happened", testEventPropertiesMap.get("&exd"));
		assertEquals("exception", testEventPropertiesMap.get("&t"));
		assertEquals("0", testEventPropertiesMap.get("&exf"));

		assertTrue(testEventPropertiesMap.containsKey("error_message"));
		assertEquals("something bad happened", testEventPropertiesMap.get("error_message"));
		assertTrue(testEventPropertiesMap.containsKey("exception_object"));
		assertEquals("java.util.EmptyStackException", testEventPropertiesMap.get("exception_object"));
	}

	@Test
	public void testSendEvent_timed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Timed Event")
				.setTimed(true);

		provider.sendEvent(event);
		assertFalse(sendCalled);
	}

	@Test
	public void testSendEvent_timed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertFalse(sendCalled);
	}

	@Test
	public void testEndTimedEvent_Valid()
	{
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		provider.sendEvent(event);
		assertFalse(sendCalled);

		try
		{
			Thread.sleep(50);
		}
		catch (InterruptedException e)
		{
			// don't do anything, this is just a test that needs some delay
		}

		provider.endTimedEvent(event);

		// Verify Google Analytics framework is called
		assertTrue(sendCalled);
		assertNotNull(testEventPropertiesMap);
		assertEquals(6, testEventPropertiesMap.size());

		assertEquals("Google Analytics Timed Event With Parameters", testEventPropertiesMap.get("&utl"));
		assertEquals("Timed Events", testEventPropertiesMap.get("&utc"));
		assertEquals("timing", testEventPropertiesMap.get("&t"));

		String timeString  = testEventPropertiesMap.get("&utt");
		long   elapsedTime = Long.valueOf(timeString);
		assertThat(elapsedTime).isGreaterThanOrEqualTo(50);

		assertTrue(testEventPropertiesMap.containsKey("some_param"));
		assertEquals("yes", testEventPropertiesMap.get("some_param"));
		assertTrue(testEventPropertiesMap.containsKey("another_param"));
		assertEquals("yes again", testEventPropertiesMap.get("another_param"));
	}

	@Test
	public void test_endTimedEvent_WillThrow()
	{
		boolean didThrow = false;
		AnalyticsEvent event = new AnalyticsEvent("Google Analytics Timed Event With Parameters")
				.setTimed(true);

		try
		{
			provider.endTimedEvent(event); // attempting to end a timed event that was not started should throw an exception
		}
		catch (IllegalStateException e)
		{
			didThrow = true;
		}

		assertThat(didThrow).isTrue();
	}
}
