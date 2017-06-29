/*
 * Copyright 2017 busybusy, Inc.
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

package com.busybusy.graylog_provider;

import com.busybusy.analyticskit_android.AnalyticsEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests the {@link EventJsonizer} class.
 *
 * @author John Hunt on 6/29/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public class EventJsonizerTest
{
	final String VERSION = "1.1";
	final String HOST    = "unit-test-android";

	EventJsonizer jsonizer = new EventJsonizer(VERSION, HOST);

	@Test
	public void getJsonBody_defaultFields()
	{
		List<String> emails = new LinkedList<>();
		emails.add("john.jacob@unittest.me");
		emails.add("john.jacob@unittest.us");
		Map<String, Object> mapField = new LinkedHashMap<>();
		mapField.put("first_name", "John");
		mapField.put("last_name", "Jacob");
		mapField.put("emails", emails);

		AnalyticsEvent event = new AnalyticsEvent("test_event")
				.putAttribute("test_attribute_1", 100)
				.putAttribute("test_attribute_2", "200")
				.putAttribute("member_info", mapField);

		double now = System.currentTimeMillis() / 1000d; // possibly flaky, shame on me
		String json = jsonizer.getJsonBody(event);

		assertThat(json).isEqualTo("{\"version\": \"" + VERSION + "\", \"host\": \"" + HOST + "\", \"short_message\": \"" +
				                           event.name() + "\", \"timestamp\": " + now + ", \"level\": 6, \"_test_attribute_1\": 100, " +
				                           "\"_test_attribute_2\": \"200\", \"_member_info\": {\"first_name\": \"John\", \"last_name\": \"Jacob\", " +
				                           "\"emails\": [\"john.jacob@unittest.me\", \"john.jacob@unittest.us\"]}}");
	}

	@Test
	public void getJsonBody_customFields()
	{
		Map<String, Object> emails = new LinkedHashMap<>();
		emails.put("personal", "john.jacob@unittest.me");
		emails.put("work", "john.jacob@unittest.us");

		Map<String, Object> mapField = new LinkedHashMap<>();
		mapField.put("first_name", "John");
		mapField.put("last_name", "Jacob");
		mapField.put("emails", emails);

		double timestamp = (System.currentTimeMillis() - 5000) / 1000d;

		AnalyticsEvent event = new AnalyticsEvent("test_event")
				.putAttribute("level", 5)
				.putAttribute("full_message", "This is a test of the JSON conversion")
				.putAttribute("timestamp", timestamp)
				.putAttribute("test_attribute_1", 100)
				.putAttribute("test_attribute_2", "200")
				.putAttribute("member_info", mapField);

		String json = jsonizer.getJsonBody(event);
		assertThat(json).isEqualTo("{\"version\": \"" + VERSION + "\", \"host\": \"" + HOST + "\", \"short_message\": \"" +
				                           event.name() + "\", \"full_message\": \"This is a test of the JSON conversion\", " +
				                           "\"timestamp\": " + timestamp + ", \"level\": 5, \"_test_attribute_1\": 100, " +
				                           "\"_test_attribute_2\": \"200\", \"_member_info\": {\"first_name\": \"John\", \"last_name\": \"Jacob\", " +
				                           "\"emails\": {\"personal\": \"john.jacob@unittest.me\", \"work\": \"john.jacob@unittest.us\"}}}");

	}

	@Test
	public void getJsonFromMapRecursive_emptyMap()
	{
		Map<String, Object> map  = new HashMap<>();
		String              json = jsonizer.getJsonFromMapRecursive(map);

		assertThat(json).isEqualTo("{}");
	}

	@Test
	public void getJsonFromMapRecursive_flatMapStructure()
	{
		Map<String, Object> map = new HashMap<>();
		map.put("one", "abc");
		map.put("two", 123);
		map.put("three", 321.123d);

		String json = jsonizer.getJsonFromMapRecursive(map);
		assertThat(json).isEqualTo("{\"one\": \"abc\", \"two\": 123, \"three\": 321.123}");
	}

	@Test
	public void getJsonFromMapRecursive_recursiveMapStructure()
	{
		Map<String, Object> map         = new LinkedHashMap<>();
		Map<String, Object> levelTwoMap = new LinkedHashMap<>();

		levelTwoMap.put("a value", 123);
		levelTwoMap.put("l3", new LinkedHashMap<>());
		map.put("l2", levelTwoMap);

		String json = jsonizer.getJsonFromMapRecursive(map);
		assertThat(json).isEqualTo("{\"l2\": {\"a value\": 123, \"l3\": {}}}");
	}

	@Test
	public void getJsonFromMapRecursive_mapOfLists()
	{
		Map<String, Object> map         = new LinkedHashMap<>();
		Map<String, Object> levelTwoMap = new LinkedHashMap<>();

		levelTwoMap.put("a value", 123);
		levelTwoMap.put("l3", new LinkedHashMap<>());
		map.put("l2", levelTwoMap);

		String json = jsonizer.getJsonFromMapRecursive(map);
		assertThat(json).isEqualTo("{\"l2\": {\"a value\": 123, \"l3\": {}}}");
	}

	@Test
	public void getJsonFromListRecursive()
	{
		List<Object> list              = new LinkedList<>();
		List<Object> innerListOne      = new LinkedList<>();
		List<Object> innerListTwo      = new LinkedList<>();
		List<Object> levelThreeListOne = new LinkedList<>();
		List<Object> levelThreeListTwo = new LinkedList<>();

		levelThreeListOne.add(31);
		levelThreeListTwo.add(32);
		innerListOne.add(levelThreeListOne);
		innerListTwo.add(levelThreeListTwo);
		list.add(innerListOne);
		list.add(innerListTwo);

		String json = jsonizer.getJsonFromListRecursive(list);
		assertThat(json).isEqualTo("[[[31]], [[32]]]");

	}

	@Test
	public void getJsonFromListRecursive_listOfMaps()
	{
		Map<String, Object> map         = new LinkedHashMap<>();
		Map<String, Object> levelTwoMap = new LinkedHashMap<>();

		levelTwoMap.put("a value", 123);
		levelTwoMap.put("l3", new LinkedHashMap<>());
		map.put("l2", levelTwoMap);

		List<Object> listMapList = new LinkedList<>();
		listMapList.add(42);
		listMapList.add(24);
		Map<String, Object> anotherMap = new LinkedHashMap<>();
		anotherMap.put("second", "value");
		anotherMap.put("second_num", 31);
		anotherMap.put("list_map_list", listMapList);

		List<Object> list = new LinkedList<>();
		list.add(map);
		list.add(anotherMap);

		String json = jsonizer.getJsonFromListRecursive(list);
		assertThat(json).isEqualTo("[{\"l2\": {\"a value\": 123, \"l3\": {}}}, {\"second\": \"value\", \"second_num\": 31, \"list_map_list\": " +
				                           "[42, 24]}]");
	}

}