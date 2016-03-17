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

package com.busybusy.mixpanel_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.Providers;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link MixpanelProvider} class
 *
 * @author John Hunt on 3/16/16.
 */
public class MixpanelProviderTest
{
	MixpanelAPI  mockMixpanelAPI;
	MixpanelProvider provider;
	AnalyticsEvent   timedEvent;
	AnalyticsEvent   untimedEvent;

	String testEventName;
	Map<String, Object> testEventPropertiesMap;
	boolean trackMapCalled;
	boolean timeEventCalled;

	@Before
	public void setup()
	{
		testEventName = null;
		testEventPropertiesMap = null;
		trackMapCalled = false;
		timeEventCalled = false;

		// Mock behavior for when MixpanelAPI.trackMap(String, Map<String, Object>) is called
		mockMixpanelAPI = mock(MixpanelAPI.class);
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				testEventName = (String) args[0];
				//noinspection unchecked
				testEventPropertiesMap = (Map<String, Object>) args[1];
				trackMapCalled = true;
				return null;
			}
		}).when(mockMixpanelAPI).trackMap(anyString(), anyMapOf(String.class, Object.class));

		// Mock behavior for when MixpanelAPI.timeEvent(String) is called
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				testEventName = (String) args[0];
				timeEventCalled = true;
				return null;
			}
		}).when(mockMixpanelAPI).timeEvent(anyString());

		provider = new MixpanelProvider(mockMixpanelAPI);
		timedEvent = new AnalyticsEvent("Timed Event")
				.setTimed(true)
				.putAttribute("timed_attribute1", "timed_test1");
		untimedEvent = new AnalyticsEvent("Untimed Event")
				.putAttribute("attribute1", "test1")
				.putAttribute("attribute2", "test2");
	}

	@Test
	public void testGetType()
	{
		assertEquals(Providers.MIXPANEL, provider.getType());
	}

	@Test
	public void testSendEvent_notTimed()
	{
		provider.sendEvent(untimedEvent);

		assertTrue(trackMapCalled);
		assertFalse(timeEventCalled);

		assertNotNull(testEventName);
		assertEquals("Untimed Event", testEventName);

		assertNotNull(testEventPropertiesMap);
		assertThat(testEventPropertiesMap.size()).isEqualTo(2);
		assertThat(testEventPropertiesMap.containsKey("attribute1"));
		assertThat(testEventPropertiesMap.containsValue("test1"));
		assertThat(testEventPropertiesMap.containsKey("attribute2"));
		assertThat(testEventPropertiesMap.containsValue("test2"));
	}

	@Test
	public void testSendEvent_timed()
	{
		provider.sendEvent(timedEvent);

		assertTrue(timeEventCalled);
		assertFalse(trackMapCalled);

		assertNotNull(testEventName);
		assertEquals("Timed Event", testEventName);

		assertNull(testEventPropertiesMap);
	}

	@Test
	public void testEndTimedEvent()
	{
		provider.endTimedEvent(timedEvent);

		assertFalse(timeEventCalled);
		assertTrue(trackMapCalled);

		assertNotNull(testEventName);
		assertEquals("Timed Event", testEventName);

		assertNotNull(testEventPropertiesMap);
		assertThat(testEventPropertiesMap.size()).isEqualTo(1);
		assertThat(testEventPropertiesMap.containsKey("timed_attribute1"));
		assertThat(testEventPropertiesMap.containsValue("timed_test1"));
	}
}
