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

package com.busybusy.flurry_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;
import com.busybusy.analyticskit_android.AnalyticsKitProvider;
import com.flurry.android.FlurryAgent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;

/**
 * Tests the {@link FlurryProvider} class
 *
 * @author John Hunt on 3/21/16.
 */
@RunWith(PowerMockRunner.class)
public class FlurryProviderTest
{
	FlurryProvider provider;

	String              testEventName;
	Map<String, Object> testEventPropertiesMap;
	boolean             logEventNameOnlyCalled;
	boolean             logEventNameAndParamsCalled;
	boolean             logTimedEventNameOnlyCalled;
	boolean             logTimedEventNameAndParamsCalled;

	@Before
	public void setup()
	{
		provider = new FlurryProvider();

		testEventName = null;
		testEventPropertiesMap = null;
		logEventNameOnlyCalled = false;
		logEventNameAndParamsCalled = false;
		logTimedEventNameOnlyCalled = false;
		logTimedEventNameAndParamsCalled = false;
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
	public void testStringifyParameters_noParams()
	{
		HashMap<String, String> flurryParams = provider.stringifyParameters(null);
		assertThat(flurryParams).isNull();
	}

	@Test
	public void testStringifyParameters_validParams()
	{
		HashMap<String, Object> eventParams = new HashMap<>();
		eventParams.put("favorite_color", "Blue");
		eventParams.put("favorite_number", 42);
		HashMap<String, String> flurryParams = provider.stringifyParameters(eventParams);
		assertThat(flurryParams).isNotNull();
		assertThat(flurryParams.keySet()).contains("favorite_color", "favorite_number");
		assertThat(flurryParams.values()).contains("Blue", "42");
	}

	@Test
	public void testStringifyParameters_willThrow()
	{
		HashMap<String, Object> attributeMap = new HashMap<>();
		for (int count = 0; count <= provider.ATTRIBUTE_LIMIT + 1; count++)
		{
			attributeMap.put(String.valueOf(count), "placeholder");
		}

		String exceptionMessage = "";
		try
		{
			provider.stringifyParameters(attributeMap);
		}
		catch (IllegalStateException e)
		{
			exceptionMessage = e.getMessage();
		}

		assertThat(exceptionMessage).isEqualTo("Flurry events are limited to " + provider.ATTRIBUTE_LIMIT + " attributes");
	}

	@PrepareForTest({FlurryAgent.class})
	@Test
	public void testSendEvent_unTimed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Flurry Test Run");

		PowerMockito.mockStatic(FlurryAgent.class);
		PowerMockito.when(FlurryAgent.logEvent(anyString()))
		            .thenAnswer(new Answer<Object>()
		            {
			            @Override
			            public Object answer(InvocationOnMock invocation) throws Throwable
			            {
				            Object[] args = invocation.getArguments();
				            testEventName = (String) args[0];
				            logEventNameOnlyCalled = true;
				            return null;
			            }

		            });

		provider.sendEvent(event);

		assertThat(testEventName).isEqualTo("Flurry Test Run");
		assertThat(testEventPropertiesMap).isNull();
		assertThat(logEventNameOnlyCalled).isTrue();

		assertThat(logEventNameAndParamsCalled).isFalse();
		assertThat(logTimedEventNameOnlyCalled).isFalse();
		assertThat(logTimedEventNameAndParamsCalled).isFalse();
	}

	@PrepareForTest({FlurryAgent.class})
	@Test
	public void testSendEvent_unTimed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Flurry Event With Params Run")
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		PowerMockito.mockStatic(FlurryAgent.class);
		PowerMockito.when(FlurryAgent.logEvent(anyString(), anyMapOf(String.class, String.class)))
		            .thenAnswer(new Answer<Object>()
		            {
			            @Override
			            public Object answer(InvocationOnMock invocation) throws Throwable
			            {
				            logEventNameAndParamsCalled = true;
				            Object[] args = invocation.getArguments();
				            testEventName = (String) args[0];
				            //noinspection unchecked
				            testEventPropertiesMap = (Map<String, Object>) args[1];
				            return null;
			            }

		            });

		provider.sendEvent(event);

		assertThat(testEventName).isEqualTo("Flurry Event With Params Run");
		assertThat(testEventPropertiesMap.keySet()).containsExactlyInAnyOrder("some_param", "another_param");
		assertThat(testEventPropertiesMap.values()).containsExactlyInAnyOrder("yes", "yes again");

		assertThat(logEventNameAndParamsCalled).isTrue();

		assertThat(logEventNameOnlyCalled).isFalse();
		assertThat(logTimedEventNameOnlyCalled).isFalse();
		assertThat(logTimedEventNameAndParamsCalled).isFalse();
	}

	@PrepareForTest({FlurryAgent.class})
	@Test
	public void testSendEvent_timed_noParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Flurry Timed Event")
				.setTimed(true);

		PowerMockito.mockStatic(FlurryAgent.class);
		PowerMockito.when(FlurryAgent.logEvent(anyString(), anyBoolean()))
		            .thenAnswer(new Answer<Object>()
		            {
			            @Override
			            public Object answer(InvocationOnMock invocation) throws Throwable
			            {
				            Object[] args = invocation.getArguments();
				            testEventName = (String) args[0];
				            logTimedEventNameOnlyCalled = true;
				            return null;
			            }
		            });

		provider.sendEvent(event);

		assertThat(testEventName).isEqualTo("Flurry Timed Event");
		assertThat(testEventPropertiesMap).isNull();
		assertThat(logTimedEventNameOnlyCalled).isTrue();

		assertThat(logEventNameOnlyCalled).isFalse();
		assertThat(logEventNameAndParamsCalled).isFalse();
		assertThat(logTimedEventNameAndParamsCalled).isFalse();
	}

	@PrepareForTest({FlurryAgent.class})
	@Test
	public void testSendEvent_timed_withParams()
	{
		AnalyticsEvent event = new AnalyticsEvent("Flurry Timed Event With Parameters")
				.setTimed(true)
				.putAttribute("some_param", "yes")
				.putAttribute("another_param", "yes again");

		PowerMockito.mockStatic(FlurryAgent.class);
		PowerMockito.when(FlurryAgent.logEvent(anyString(),
		                                       anyMapOf(String.class, String.class),
		                                       anyBoolean()))
		            .thenAnswer(new Answer<Object>()
		            {
			            @Override
			            public Object answer(InvocationOnMock invocation) throws Throwable
			            {
				            Object[] args = invocation.getArguments();
				            testEventName = (String) args[0];
				            logTimedEventNameAndParamsCalled = true;
				            //noinspection unchecked
				            testEventPropertiesMap = (Map<String, Object>) args[1];
				            return null;
			            }
		            });

		provider.sendEvent(event);

		assertThat(testEventName).isEqualTo("Flurry Timed Event With Parameters");
		assertThat(testEventPropertiesMap.keySet()).containsExactlyInAnyOrder("some_param", "another_param");
		assertThat(testEventPropertiesMap.values()).containsExactlyInAnyOrder("yes", "yes again");

		assertThat(logTimedEventNameAndParamsCalled).isTrue();

		assertThat(logTimedEventNameOnlyCalled).isFalse();
		assertThat(logEventNameOnlyCalled).isFalse();
		assertThat(logEventNameAndParamsCalled).isFalse();
	}

	@PrepareForTest({FlurryAgent.class})
	@Test
	public void testEndTimedEvent()
	{
		AnalyticsEvent event = new AnalyticsEvent("End timed event");
		PowerMockito.mockStatic(FlurryAgent.class);

		provider.endTimedEvent(event);

		// Verify Flurry framework is called
		PowerMockito.verifyStatic(Mockito.times(1));
		FlurryAgent.endTimedEvent(event.name());
	}
}
