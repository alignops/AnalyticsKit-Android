/*
 * Copyright 2016 - 2022 busybusy, Inc.
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
package com.busybusy.analyticskit_android

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests the [AnalyticsEvent] class.
 *
 * @author John Hunt on 3/5/16.
 */
class AnalyticsEventTest {
    @Test
    fun testBuilder_withAttributes() {
        val name = "Yeah, this should work"
        val animalText = "The quick brown fox jumps over the lazy dog"
        val event = AnalyticsEvent(name)
                .putAttribute("the answer", 42)
                .putAttribute("animal text", animalText)
        assertThat(event.name()).isEqualTo(name)
        assertThat(event.attributes).isNotNull
        assertThat(event.attributes!!.keys).containsOnly("the answer", "animal text")
        assertThat(event.attributes!!.values).containsOnly(42, animalText)
    }

    @Test
    fun testBuilder_noAttributes() {
        val name = "Yeah, this should work"
        val event = AnalyticsEvent(name)
        assertThat(event.name()).isEqualTo(name)
        assertThat(event.attributes).isNull()
    }

    @Test
    fun testBuilder_specifyPriority() {
        val name = "Priority 7 event"
        val event = AnalyticsEvent(name)
                .setPriority(7)
        assertThat(event.name()).isEqualTo(name)
        assertThat(event.attributes).isNull()
        assertThat(event.priority).isEqualTo(7)
    }
}
