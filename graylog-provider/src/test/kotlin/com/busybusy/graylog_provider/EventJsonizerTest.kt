/*
 * Copyright 2017 - 2018, 2020 busybusy, Inc.
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
package com.busybusy.graylog_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * Tests the [EventJsonizer] class.
 *
 * @author John Hunt on 6/29/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class EventJsonizerTest {
    val VERSION = "1.1"
    val HOST = "unit-test-android"
    var jsonizer = EventJsonizer(VERSION, HOST)

    @Test
    fun jsonBody_defaultFields() {
        val emails = mutableListOf<String>("john.jacob@unittest.me", "john.jacob@unittest.us")
        val mapField = mutableMapOf<String, Any>("first_name" to "John", "last_name" to "Jacob", "emails" to emails)
        val event = AnalyticsEvent("test_event")
                .putAttribute("test_attribute_1", 100)
                .putAttribute("test_attribute_2", "200")
                .putAttribute("member_info", mapField)
        val now = System.currentTimeMillis() / 1000.0 // possibly flaky, shame on me
        val json = jsonizer.getJsonBody(event)
        assertThat(json).isEqualTo("{\"version\": \"" + VERSION + "\", \"host\": \"" + HOST + "\", \"short_message\": \"" +
                event.name() + "\", \"timestamp\": " + now + ", \"level\": 6, \"_test_attribute_1\": 100, " +
                "\"_test_attribute_2\": \"200\", \"_member_info\": {\"first_name\": \"John\", \"last_name\": \"Jacob\", " +
                "\"emails\": [\"john.jacob@unittest.me\", \"john.jacob@unittest.us\"]}}")
    }

    @Test
    fun jsonBody_customFields() {
        val emails = mutableMapOf<String, Any>("personal" to "john.jacob@unittest.me", "work" to "john.jacob@unittest.us")
        val mapField = mutableMapOf<String, Any>("first_name" to "John", "last_name" to "Jacob", "emails" to emails)
        val timestamp = (System.currentTimeMillis() - 5000) / 1000.0
        val event = AnalyticsEvent("test_event")
                .putAttribute("level", 5)
                .putAttribute("full_message", "This is a test of the JSON conversion")
                .putAttribute("timestamp", timestamp)
                .putAttribute("test_attribute_1", 100)
                .putAttribute("test_attribute_2", "200")
                .putAttribute("member_info", mapField)
        val json = jsonizer.getJsonBody(event)
        assertThat(json).isEqualTo("{\"version\": \"" + VERSION + "\", \"host\": \"" + HOST + "\", \"short_message\": \"" +
                event.name() + "\", \"full_message\": \"This is a test of the JSON conversion\", " +
                "\"timestamp\": " + timestamp + ", \"level\": 5, \"_test_attribute_1\": 100, " +
                "\"_test_attribute_2\": \"200\", \"_member_info\": {\"first_name\": \"John\", \"last_name\": \"Jacob\", " +
                "\"emails\": {\"personal\": \"john.jacob@unittest.me\", \"work\": \"john.jacob@unittest.us\"}}}")
    }

    @Test
    fun jsonBody_WithBooleans() {
        val timestamp = (System.currentTimeMillis() - 5000) / 1000.0
        val event = AnalyticsEvent("test_event")
                .putAttribute("level", 5)
                .putAttribute("full_message", "This is a test of the JSON conversion")
                .putAttribute("timestamp", timestamp)
                .putAttribute("test_attribute_1", 100)
                .putAttribute("test_attribute_2", "200")
                .putAttribute("can_pass_booleans", true)
        val json = jsonizer.getJsonBody(event)
        assertThat(json).isEqualTo("{\"version\": \"" + VERSION + "\", \"host\": \"" + HOST + "\", \"short_message\": \"" +
                event.name() + "\", \"full_message\": \"This is a test of the JSON conversion\", " +
                "\"timestamp\": " + timestamp + ", \"level\": 5, \"_test_attribute_1\": 100, " +
                "\"_test_attribute_2\": \"200\", \"_can_pass_booleans\": true}")
    }

    @Test
    fun jsonFromMapRecursive_emptyMap() {
        val map: Map<String, Any> = HashMap()
        val json = jsonizer.getJsonFromMapRecursive(map)
        assertThat(json).isEqualTo("{}")
    }

    @Test
    fun jsonFromMapRecursive_flatMapStructure() {
        val map = mutableMapOf<String, Any>("one" to "abc", "two" to 123, "three" to 321.123, "four" to true)
        val json = jsonizer.getJsonFromMapRecursive(map)
        assertThat(json).isEqualTo("{\"one\": \"abc\", \"two\": 123, \"three\": 321.123, \"four\": true}")
    }

    @Test
    fun jsonFromMapRecursive_recursiveMapStructure() {
        val levelTwoMap = mutableMapOf<String, Any>("a value" to 123, "l3" to mutableMapOf<Any, Any>())
        val map = mutableMapOf<String, Any>("l2" to levelTwoMap)
        val json = jsonizer.getJsonFromMapRecursive(map)
        assertThat(json).isEqualTo("{\"l2\": {\"a value\": 123, \"l3\": {}}}")
    }

    @Test
    fun jsonFromMapRecursive_mapOfLists() {
        val levelTwoMap = mutableMapOf<String, Any>("a value" to 123, "l3" to mutableMapOf<Any, Any>())
        val map = mutableMapOf<String, Any>("l2" to levelTwoMap)
        val json = jsonizer.getJsonFromMapRecursive(map)
        assertThat(json).isEqualTo("{\"l2\": {\"a value\": 123, \"l3\": {}}}")
    }

    @Test
    fun jsonFromListRecursive() {
        val levelThreeListOne = mutableListOf<Any>(31)
        val levelThreeListTwo = mutableListOf<Any>(32)
        val innerListOne = mutableListOf<Any>(levelThreeListOne)
        val innerListTwo = mutableListOf<Any>(levelThreeListTwo)
        val innerListThree = mutableListOf<Any>(true)
        val topLevelList = mutableListOf<Any>(innerListOne, innerListTwo, innerListThree)
        val json = jsonizer.getJsonFromListRecursive(topLevelList)
        assertThat(json).isEqualTo("[[[31]], [[32]], [true]]")
    }

    @Test
    fun jsonFromListRecursive_listOfMaps() {
        val levelTwoMap = mutableMapOf<String, Any>("a value" to 123, "l3" to mutableMapOf<Any, Any>())
        val map = mutableMapOf<String, Any>("l2" to levelTwoMap)
        val listMapList = mutableListOf<Any>(42, 24)
        val anotherMap = mutableMapOf<String, Any>("second" to "value", "second_num" to 31, "list_map_list" to listMapList)
        val list = mutableListOf<Any>(map, anotherMap)
        val json = jsonizer.getJsonFromListRecursive(list)
        assertThat(json).isEqualTo("[{\"l2\": {\"a value\": 123, \"l3\": {}}}, {\"second\": \"value\", \"second_num\": 31, \"list_map_list\": " +
                "[42, 24]}]")
    }
}
