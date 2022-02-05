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

import com.busybusy.analyticskit_android.AnalyticsEvent
import java.lang.Error
import java.lang.Exception
import java.lang.StringBuilder
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import java.util.HashSet

/**
 * Turns an [AnalyticsEvent] into a JSON String.
 *
 * @author John Hunt on 6/29/17.
 */
class EventJsonizer internal constructor(gelfSpecVersion: String, host: String) {
    val HOST: String
    val GELF_SPEC_VERSION: String
    val MAX_FIELD_LENGTH = 32000

    init {
        GELF_SPEC_VERSION = getSafeSizeString(gelfSpecVersion)
        HOST = getSafeSizeString(host)
    }

    /**
     * Builds a JSON string from an [AnalyticsEvent].
     *
     * @param event the event to serialize to JSON
     * @return the JSON string representation of the given event
     */
    fun getJsonBody(event: AnalyticsEvent): String {
        val eventAttributes = if (event.attributes != null) event.attributes else HashMap()
        val attributes: Set<String> =
            if (event.attributes != null) event.attributes!!.keys else HashSet()

        // guard clause: Libraries SHOULD not allow to send id as additional field (_id).
        if (attributes.contains("id")) {
            throw UnsupportedOperationException("id is NOT allowed as an additional field according to the GELF spec!")
        }
        val sb = StringBuilder("{")
        sb.append("\"version\": \"").append(GELF_SPEC_VERSION).append("\", ")
        sb.append("\"host\": \"").append(HOST).append("\", ")
        sb.append("\"short_message\": \"").append(getSafeSizeString(event.name())).append("\", ")
        if (attributes.contains("full_message")) {
            val fullMessage = eventAttributes!!["full_message"].toString()
            sb.append("\"full_message\": \"").append(getSafeSizeString(fullMessage)).append("\", ")
        }
        // No else needed: this is an optional long message (may contain a backtrace)
        if (attributes.contains("timestamp")) {
            val timestamp = eventAttributes!!["timestamp"].toString()
            sb.append("\"timestamp\": ").append(getSafeSizeString(timestamp))
                .append(", ") // user-provided timestamp
        } else {
            sb.append("\"timestamp\": ").append(System.currentTimeMillis() / 1000.0).append(", ")
        }
        if (attributes.contains("level")) {
            val level = eventAttributes!!["level"].toString()
            sb.append("\"level\": ").append(getSafeSizeString(level)) // user-provided syslog level
        } else {
            sb.append("\"level\": 6") // Informational
        }
        for (attribute in attributes) {
            if (attribute.equals("version", ignoreCase = true) ||
                attribute.equals("host", ignoreCase = true) ||
                attribute.equals("short_message", ignoreCase = true) ||
                attribute.equals("full_message", ignoreCase = true) ||
                attribute.equals("timestamp", ignoreCase = true) ||
                attribute.equals("level", ignoreCase = true)
            ) {
                // do nothing, these fields have already been handled above
            } else {
                val jsonAttribute = attribute.replace("\\s".toRegex(),
                    "_") // Graylog omits fields whose name have white space, replace with '_'
                sb.append(underscorePrefix(jsonAttribute))
                val attributeValue = eventAttributes!![attribute]
                if (attributeValue is String) // gotta use escape quotes for JSON strings
                {
                    sb.append(jsonAttribute).append("\": \"").append(getSafeSizeString(
                        attributeValue)).append("\"")
                } else if (attributeValue is List<*>) {
                    sb.append(jsonAttribute).append("\": ").append(getJsonFromListRecursive(
                        attributeValue))
                } else if (attributeValue is Map<*, *>) {
                    sb.append(jsonAttribute).append("\": ")
                        .append(getJsonFromMapRecursive(attributeValue as Map<String, Any>))
                } else if (isNumber(attributeValue) || attributeValue is Boolean) {
                    sb.append(jsonAttribute).append("\": ").append(attributeValue)
                } else if (attributeValue is Exception) {
                    sb.append(jsonAttribute).append("\": \"").append(attributeValue.toString())
                        .append("\"")
                } else if (attributeValue is Error) {
                    sb.append(jsonAttribute).append("\": \"").append(attributeValue.toString())
                        .append("\"")
                } else {
                    throw UnsupportedOperationException("Unsupported type for GELF message: " + attributeValue!!.javaClass.simpleName)
                }
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun underscorePrefix(fieldName: String): String {
        return if (fieldName.startsWith("_")) {   // the user-provided field already has the underscore prefix, let's not prepend another underscore
            ", \""
        } else {   // the user didn't bother to prefix with underscores, let's do it for them to conform to the Graylog spec
            ", \"_"
        }
    }

    fun getJsonFromMapRecursive(attributeMap: Map<String, Any>): String {
        val json = StringBuilder("{")
        for (innerAttribute in attributeMap.keys) {
            val innerAttributeValue = attributeMap[innerAttribute]
            if (innerAttributeValue is Map<*, *>) // recursive case
            {
                // recurse
                val innerMap = attributeMap[innerAttribute] as Map<String, Any>?
                json.append("\"").append(innerAttribute).append("\": ")
                    .append(getJsonFromMapRecursive(
                        innerMap!!)).append(", ")
            } else if (innerAttributeValue is List<*>) // recursive case
            {
                json.append("\"").append(innerAttribute).append("\": ")
                    .append(getJsonFromListRecursive(
                        innerAttributeValue)).append(", ")
            } else {
                if (innerAttributeValue is String) // gotta use escape quotes for JSON strings
                {
                    json.append("\"").append(innerAttribute).append("\": \"")
                        .append(getSafeSizeString(
                            innerAttributeValue)).append("\", ")
                } else if (isNumber(innerAttributeValue) || innerAttributeValue is Boolean) {
                    json.append("\"").append(innerAttribute).append("\": ")
                        .append(innerAttributeValue).append(", ")
                } else {
                    throw UnsupportedOperationException("Unsupported type for GELF message: " + innerAttributeValue!!.javaClass.simpleName)
                }
            }
        }
        if (json.toString().length > 1) {
            json.deleteCharAt(json.toString().length - 1)
            json.deleteCharAt(json.toString().length - 1)
        }
        // No else needed: the map was empty, no need to trim the last comma and space
        json.append("}")
        return json.toString()
    }

    fun getJsonFromListRecursive(attributeList: List<*>): String {
        val json = StringBuilder("[")
        for (element in attributeList) {
            if (element is Map<*, *>) // recursive case
            {
                json.append(getJsonFromMapRecursive(element as Map<String, Any>)).append(", ")
            } else if (element is List<*>) // recursive case
            {
                json.append(getJsonFromListRecursive(element)).append(", ")
            } else {
                if (element is String) // gotta use escape quotes for JSON strings
                {
                    json.append("\"").append(getSafeSizeString(element)).append("\", ")
                } else if (isNumber(element) || element is Boolean) {
                    json.append(element).append(", ")
                } else {
                    throw UnsupportedOperationException("Unsupported type for GELF message: " + element?.javaClass?.simpleName)
                }
            }
        }
        if (json.toString().length > 1) {
            json.deleteCharAt(json.toString().length - 1)
            json.deleteCharAt(json.toString().length - 1)
        }
        // No else needed: the list was empty, no need to trim the last comma and space
        json.append("]")
        return json.toString()
    }

    private fun getSafeSizeString(input: String): String {
        return if (input.length <= MAX_FIELD_LENGTH) {
            input
        } else {
            var safeSizeValue = input.substring(0, input.length.coerceAtMost(MAX_FIELD_LENGTH))
            //correctly process UTF-16 surrogate pairs
            if (safeSizeValue.length > MAX_FIELD_LENGTH) {
                val correctedMaxWidth =
                    if (Character.isLowSurrogate(safeSizeValue[MAX_FIELD_LENGTH])) MAX_FIELD_LENGTH - 1 else MAX_FIELD_LENGTH
                safeSizeValue = safeSizeValue.substring(
                    0,
                    safeSizeValue.length.coerceAtMost(correctedMaxWidth)
                )
            }
            safeSizeValue
        }
    }

    private fun isNumber(attributeValue: Any?): Boolean {
        return attributeValue is Int ||
                attributeValue is Double ||
                attributeValue is Long ||
                attributeValue is BigInteger ||
                attributeValue is BigDecimal
    }
}
