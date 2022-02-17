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

/**
 * Turns an [AnalyticsEvent] into a JSON String.
 *
 * @author John Hunt on 6/29/17.
 */
class EventJsonizer internal constructor(gelfSpecVersion: String, host: String) {
    private val HOST: String
    private val GELF_SPEC_VERSION: String

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
    @Suppress("UNCHECKED_CAST")
    @Throws(UnsupportedOperationException::class)
    fun getJsonBody(event: AnalyticsEvent): String {
        val eventAttributes = if (event.attributes != null) event.attributes else mutableMapOf()
        val attributes: Set<String> = when {
            event.attributes != null -> event.attributes!!.keys
            else -> mutableSetOf()
        }

        // guard clause: Libraries SHOULD NOT allow to send id as additional field (_id).
        if (attributes.contains("id")) {
            throw UnsupportedOperationException("id is NOT allowed as an additional field according to the GELF spec!")
        }

        return buildString {
            append("{")
            putGraylogSpecFields(this, event.name(), attributes, requireNotNull(eventAttributes))

            for (attribute in attributes.filterNot {
                // these fields have already been handled above
                it.equals("version", ignoreCase = true) ||
                        it.equals("host", ignoreCase = true) ||
                        it.equals("short_message", ignoreCase = true) ||
                        it.equals("full_message", ignoreCase = true) ||
                        it.equals("timestamp", ignoreCase = true) ||
                        it.equals("level", ignoreCase = true)
            }) {
                // Graylog omits fields whose name have white space, replace with '_'
                val jsonAttribute = attribute.replace("\\s".toRegex(), "_")

                append(", \"${underscorePrefix(jsonAttribute)}")
                when (val attributeValue = eventAttributes[attribute]) {
                    is String -> append(jsonAttribute).append("\": \"")
                        .append(getSafeSizeString(attributeValue)).append("\"")
                    is List<*> -> append(jsonAttribute).append("\": ")
                        .append(getJsonFromListRecursive(attributeValue))
                    is Map<*, *> -> append(jsonAttribute).append("\": ")
                        .append(getJsonFromMapRecursive(attributeValue as Map<String, Any>))
                    is Number, is Boolean -> append(jsonAttribute).append("\": ")
                        .append(attributeValue)
                    is Exception, is Error -> append(jsonAttribute).append("\": \"")
                        .append(attributeValue.toString()).append("\"")
                    else -> {
                        throw UnsupportedOperationException("Unsupported type for GELF message: " + attributeValue!!.javaClass.simpleName)
                    }
                }
            }
            append("}")
        }
    }

    private fun putGraylogSpecFields(
        builder: StringBuilder,
        eventName: String,
        attributes: Set<String>,
        eventAttributes: Map<String, Any>,
    ) = builder.apply {
        append("\"version\": \"$GELF_SPEC_VERSION").append("\", ")
        append("\"host\": \"$HOST").append("\", ")
        append("\"short_message\": \"").append(getSafeSizeString(eventName)).append("\", ")

        if (attributes.contains("full_message")) {
            val fullMessage = eventAttributes["full_message"].toString()
            append("\"full_message\": \"").append(getSafeSizeString(fullMessage)).append("\", ")
        }
        // No else needed: this is an optional long message (may contain a backtrace)

        if (attributes.contains("timestamp")) { // user-provided timestamp
            append("\"timestamp\": ")
                .append(getSafeSizeString(eventAttributes["timestamp"].toString())).append(", ")
        } else {
            append("\"timestamp\": ").append(System.currentTimeMillis() / 1000.0).append(", ")
        }

        if (attributes.contains("level")) { // user-provided syslog level
            append("\"level\": ").append(getSafeSizeString(eventAttributes["level"].toString()))
        } else {
            append("\"level\": 6") // default to Informational syslog level
        }
    }

    /**
     * Ensures that user-provided fields conform to the Graylog spec (prefixed by an underscore)
     */
    private fun underscorePrefix(fieldName: String): String =
        if (fieldName.startsWith("_")) "" else "_"

    @Suppress("UNCHECKED_CAST")
    internal fun getJsonFromMapRecursive(attributeMap: Map<String, Any>): String = buildString {
        append("{")
        for (innerAttribute in attributeMap.keys) {
            when (val innerAttributeValue = attributeMap[innerAttribute]) {
                is Map<*, *> -> { // recursive case
                    val innerMap = attributeMap[innerAttribute] as Map<String, Any>
                    append("\"").append(innerAttribute).append("\": ")
                        .append(getJsonFromMapRecursive(innerMap)).append(", ")
                }
                is List<*> -> { // recursive case
                    append("\"").append(innerAttribute).append("\": ")
                        .append(getJsonFromListRecursive(innerAttributeValue)).append(", ")
                }
                is String -> { // gotta use escape quotes for JSON strings
                    append("\"").append(innerAttribute).append("\": \"")
                        .append(getSafeSizeString(innerAttributeValue)).append("\", ")
                }
                is Number, is Boolean -> {
                    append("\"").append(innerAttribute).append("\": ")
                        .append(innerAttributeValue).append(", ")
                }
                else -> throw UnsupportedOperationException(
                    "Unsupported type for GELF message: " + innerAttributeValue!!.javaClass.simpleName
                )
            }
        }
        if (this.toString().length > 1) {
            deleteCharAt(this.toString().length - 1)
            deleteCharAt(this.toString().length - 1)
        }
        // No else needed: the map was empty, no need to trim the last comma and space
        append("}")
    }

    @Suppress("UNCHECKED_CAST")
    internal fun getJsonFromListRecursive(attributeList: List<*>): String = buildString {
        append("[")
        for (element in attributeList) {
            when (element) {
                is Map<*, *> -> { // recursive case
                    append(getJsonFromMapRecursive(element as Map<String, Any>)).append(", ")
                }
                is List<*> -> { // recursive case
                    append(getJsonFromListRecursive(element)).append(", ")
                }
                is String -> append("\"").append(getSafeSizeString(element)).append("\", ")
                is Number, is Boolean -> append(element).append(", ")
                else -> throw UnsupportedOperationException(
                    "Unsupported type for GELF message: " + element?.javaClass?.simpleName
                )
            }
        }
        if (this.toString().length > 1) {
            deleteCharAt(this.toString().length - 1)
            deleteCharAt(this.toString().length - 1)
        }
        // No else needed: the list was empty, no need to trim the last comma and space
        append("]")
    }

    private fun getSafeSizeString(input: String): String =
        if (input.length <= MAX_FIELD_LENGTH) {
            input
        } else {
            var safeSizeValue = input.substring(0, input.length.coerceAtMost(MAX_FIELD_LENGTH))
            //correctly process UTF-16 surrogate pairs
            if (safeSizeValue.length > MAX_FIELD_LENGTH) {
                val correctedMaxWidth =
                    if (Character.isLowSurrogate(safeSizeValue[MAX_FIELD_LENGTH])) {
                        MAX_FIELD_LENGTH - 1
                    } else {
                        MAX_FIELD_LENGTH
                    }
                safeSizeValue = safeSizeValue.substring(
                    0,
                    safeSizeValue.length.coerceAtMost(correctedMaxWidth)
                )
            }
            safeSizeValue
        }
}

private const val MAX_FIELD_LENGTH = 32_000
