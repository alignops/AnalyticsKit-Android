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
 * Tests the [ErrorEvent] class.
 *
 * @author John Hunt on 4/6/16.
 */
class ErrorEventTest {
    @Test
    fun testConstructor() {
        var event: AnalyticsEvent = ErrorEvent()
        assertThat(event.name()).isEqualTo(CommonEvents.ERROR)
        event = ErrorEvent("Custom Error Name")
        assertThat(event.name()).isEqualTo("Custom Error Name")
        assertThat(event.error()).isNull()
    }

    @Test
    fun testSetAndGetMessage() {
        val message = "Something went wrong"
        val event = ErrorEvent()
        assertThat(event.message()).isNull()
        event.setMessage(message)
        assertThat(event.message()).isEqualTo(message)
    }

    @Test
    fun testSetAndGetException() {
        val exception: Exception = StringIndexOutOfBoundsException()
        val event = ErrorEvent()
        assertThat(event.exception()).isNull()
        event.setException(exception)
        assertThat(event.exception()).isEqualTo(exception)
    }

    @Test
    fun testSetAndGetError() {
        val error: Error = UnknownError()
        val event = ErrorEvent()
        assertThat(event.error()).isNull()
        event.setError(error)
        assertThat(event.error()).isEqualTo(error)
    }
}
