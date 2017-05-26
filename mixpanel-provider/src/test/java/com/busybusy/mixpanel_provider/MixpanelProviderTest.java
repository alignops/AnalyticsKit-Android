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
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
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
	MixpanelAPI      mockMixpanelAPI;
	MixpanelProvider provider;
	AnalyticsEvent   timedEvent;
	AnalyticsEvent   untimedEvent;

	String              testEventName;
	Map<String, Object> testEventPropertiesMap;
	boolean             trackMapCalled;
	boolean             timeEventCalled;

	@Before
	public void setup()
	{
		testEventName = null;
		testEventPropertiesMap = null;
		trackMapCalled = false;
		timeEventCalled = false;

		// Mock behavior for when MixpanelAPI.trackMap(String, Map<String, Object>) is called
		mockMixpanelAPI = mock(MixpanelAPI.class);
		doAnswer(new Answer<Void>()
		{
			public Void answer(InvocationOnMock invocation)
			{
				Object[] args = invocation.getArguments();
				testEventName = (String) args[0];
				//noinspection unchecked
				testEventPropertiesMap = (Map<String, Object>) args[1];
				trackMapCalled = true;
				return null;
			}
		}).when(mockMixpanelAPI).trackMap(anyString(), anyMapOf(String.class, Object.class));

		// Mock behavior for when MixpanelAPI.timeEvent(String) is called
		doAnswer(new Answer<Void>()
		{
			public Void answer(InvocationOnMock invocation)
			{
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
	public void testGetAndSetPriorityFilter()
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
		AnalyticsEvent event = new AnalyticsEvent("Let's test event priorities")
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

		AnalyticsEvent event = new AnalyticsEvent("Let's test event priorities again")
				.setPriority(10)
				.send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isFalse();

		event.setPriority(9)
		     .send();

		assertThat(provider.getPriorityFilter().shouldLog(event.getPriority())).isTrue();
	}

	@Test
	public void testSendEvent_notTimed()
	{
		provider.sendEvent(untimedEvent);

		assertThat(trackMapCalled).isTrue();
		assertThat(timeEventCalled).isFalse();

		assertThat(testEventName).isNotNull();
		assertThat(testEventName).isEqualTo("Untimed Event");

		assertThat(testEventPropertiesMap.keySet()).containsOnly("attribute1", "attribute2");
		assertThat(testEventPropertiesMap.values()).containsOnly("test1", "test2");
	}

	@Test
	public void testSendEvent_timed()
	{
		provider.sendEvent(timedEvent);

		assertThat(timeEventCalled).isTrue();
		assertThat(trackMapCalled).isFalse();

		assertThat(testEventName).isEqualTo("Timed Event");

		assertThat(testEventPropertiesMap).isNull();
	}

	@Test
	public void testEndTimedEvent()
	{
		provider.endTimedEvent(timedEvent);

		assertThat(timeEventCalled).isFalse();
		assertThat(trackMapCalled).isTrue();

		assertThat(testEventName).isEqualTo("Timed Event");
		assertThat(testEventPropertiesMap.keySet()).containsExactly("timed_attribute1");
		assertThat(testEventPropertiesMap.values()).containsExactly("timed_test1");
	}
}
