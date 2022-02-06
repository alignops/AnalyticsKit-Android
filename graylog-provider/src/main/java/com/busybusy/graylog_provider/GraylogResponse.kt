/*
 * Copyright 2017 - 2022 busybusy, Inc.
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

/**
 * Data class containing the HTTP response code, HTTP response message, and the serialized
 * event JSON data.
 * @property code          the HTTP response code
 * @property message       the HTTP response message
 * @property eventName     the result of calling [AnalyticsEvent.name]
 * @property eventHashCode the result of calling [AnalyticsEvent.hashCode]
 * @property jsonPayload   the JSON payload that was sent via HTTP to your Graylog input
 *
 * @author John Hunt on 6/28/17.
 */
data class GraylogResponse(
    val code: Int,
    val message: String,
    val eventName: String,
    val eventHashCode: Int,
    val jsonPayload: String
)
