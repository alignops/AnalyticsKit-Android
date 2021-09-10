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

import androidx.annotation.NonNull;

import com.busybusy.analyticskit_android.AnalyticsEvent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns an {@link AnalyticsEvent} into a JSON String.
 *
 * @author John Hunt on 6/29/17.
 */
public class EventJsonizer
{
    final String HOST;
    final String GELF_SPEC_VERSION;
    final int MAX_FIELD_LENGTH = 32000;

    EventJsonizer(@NonNull String gelfSpecVersion, @NonNull String host)
    {
        this.GELF_SPEC_VERSION = getSafeSizeString(gelfSpecVersion);
        this.HOST = getSafeSizeString(host);
    }

    /**
     * Builds a JSON string from an {@link AnalyticsEvent}.
     *
     * @param event the event to serialize to JSON
     * @return the JSON string representation of the given event
     */
    @SuppressWarnings({"WeakerAccess"})
    @NonNull
    public String getJsonBody(AnalyticsEvent event)
    {
        Map<String, Object> eventAttributes = event.getAttributes() != null ? event.getAttributes() : new HashMap<String, Object>();
        Set<String>         attributes      = event.getAttributes() != null ? event.getAttributes().keySet() : new HashSet<String>();

        // guard clause: Libraries SHOULD not allow to send id as additional field (_id).
        if (attributes.contains("id"))
        {
            throw new UnsupportedOperationException("id is NOT allowed as an additional field according to the GELF spec!");
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"version\": \"").append(GELF_SPEC_VERSION).append("\", ");
        sb.append("\"host\": \"").append(HOST).append("\", ");
        sb.append("\"short_message\": \"").append(getSafeSizeString(event.name())).append("\", ");

        if (attributes.contains("full_message"))
        {
            String fullMessage = eventAttributes.get("full_message").toString();
            sb.append("\"full_message\": \"").append(getSafeSizeString(fullMessage)).append("\", ");
        }
        // No else needed: this is an optional long message (may contain a backtrace)

        if (attributes.contains("timestamp"))
        {
            String timestamp = eventAttributes.get("timestamp").toString();
            sb.append("\"timestamp\": ").append(getSafeSizeString(timestamp)).append(", ");  // user-provided timestamp
        }
        else
        {
            sb.append("\"timestamp\": ").append(System.currentTimeMillis() / 1000d).append(", ");
        }

        if (attributes.contains("level"))
        {
            String level = eventAttributes.get("level").toString();
            sb.append("\"level\": ").append(getSafeSizeString(level)); // user-provided syslog level
        }
        else
        {
            sb.append("\"level\": 6"); // Informational
        }

        for (String attribute : attributes)
        {
            //noinspection StatementWithEmptyBody
            if (attribute.equalsIgnoreCase("version") ||
                    attribute.equalsIgnoreCase("host") ||
                    attribute.equalsIgnoreCase("short_message") ||
                    attribute.equalsIgnoreCase("full_message") ||
                    attribute.equalsIgnoreCase("timestamp") ||
                    attribute.equalsIgnoreCase("level"))
            {
                // do nothing, these fields have already been handled above
            }
            else
            {
                String jsonAttribute = attribute.replaceAll("\\s", "_"); // Graylog omits fields whose name have white space, replace with '_'
                sb.append(underscorePrefix(jsonAttribute));

                Object attributeValue = eventAttributes.get(attribute);
                if (attributeValue instanceof String) // gotta use escape quotes for JSON strings
                {
                    sb.append(jsonAttribute).append("\": \"").append(getSafeSizeString((String) attributeValue)).append("\"");
                }
                else if (attributeValue instanceof List)
                {
                    sb.append(jsonAttribute).append("\": ").append(getJsonFromListRecursive((List) attributeValue));
                }
                else if (attributeValue instanceof Map)
                {
                    //noinspection unchecked
                    sb.append(jsonAttribute).append("\": ").append(getJsonFromMapRecursive((Map<String, Object>) attributeValue));
                }
                else if (isNumber(attributeValue) || attributeValue instanceof Boolean)
                {
                    sb.append(jsonAttribute).append("\": ").append(attributeValue);
                }
                else if (attributeValue instanceof Exception)
                {
                    sb.append(jsonAttribute).append("\": \"").append(attributeValue.toString()).append("\"");
                }
                else if (attributeValue instanceof Error)
                {
                    sb.append(jsonAttribute).append("\": \"").append(attributeValue.toString()).append("\"");
                }
                else
                {
                    throw new UnsupportedOperationException("Unsupported type for GELF message: " + attributeValue.getClass().getSimpleName());
                }

            }
        }

        sb.append("}");
        return sb.toString();
    }

    @NonNull
    String underscorePrefix(@NonNull String fieldName)
    {
        if (fieldName.startsWith("_"))
        {   // the user-provided field already has the underscore prefix, let's not prepend another underscore
            return ", \"";
        }
        else
        {   // the user didn't bother to prefix with underscores, let's do it for them to conform to the Graylog spec
            return ", \"_";
        }
    }

    @NonNull
    String getJsonFromMapRecursive(@NonNull Map<String, Object> attributeMap)
    {
        StringBuilder json = new StringBuilder("{");

        for (String innerAttribute : attributeMap.keySet())
        {
            final Object innerAttributeValue = attributeMap.get(innerAttribute);

            if (innerAttributeValue instanceof Map) // recursive case
            {
                // recurse
                //noinspection unchecked
                Map<String, Object> innerMap = (Map<String, Object>) attributeMap.get(innerAttribute);
                json.append("\"").append(innerAttribute).append("\": ").append(getJsonFromMapRecursive(innerMap)).append(", ");
            }
            else if (innerAttributeValue instanceof List) // recursive case
            {
                json.append("\"").append(innerAttribute).append("\": ").append(getJsonFromListRecursive((List) innerAttributeValue)).append(", ");
            }
            else
            {
                if (innerAttributeValue instanceof String) // gotta use escape quotes for JSON strings
                {
                    json.append("\"").append(innerAttribute).append("\": \"").append(getSafeSizeString((String) innerAttributeValue)).append("\", ");
                }
                else if (isNumber(innerAttributeValue) || innerAttributeValue instanceof Boolean)
                {
                    json.append("\"").append(innerAttribute).append("\": ").append(innerAttributeValue).append(", ");
                }
                else
                {
                    throw new UnsupportedOperationException("Unsupported type for GELF message: " + innerAttributeValue.getClass().getSimpleName());
                }
            }
        }

        if (json.toString().length() > 1)
        {
            json.deleteCharAt(json.toString().length() - 1);
            json.deleteCharAt(json.toString().length() - 1);
        }
        // No else needed: the map was empty, no need to trim the last comma and space

        json.append("}");
        return json.toString();
    }

    @NonNull
    String getJsonFromListRecursive(@NonNull List attributeList)
    {
        StringBuilder json = new StringBuilder("[");

        for (Object element : attributeList)
        {
            if (element instanceof Map) // recursive case
            {
                //noinspection unchecked
                json.append(getJsonFromMapRecursive((Map<String, Object>) element)).append(", ");
            }
            else if (element instanceof List) // recursive case
            {
                json.append(getJsonFromListRecursive((List) element)).append(", ");
            }
            else
            {
                if (element instanceof String) // gotta use escape quotes for JSON strings
                {
                    json.append("\"").append(getSafeSizeString((String) element)).append("\", ");
                }
                else if (isNumber(element) || element instanceof Boolean)
                {
                    json.append(element).append(", ");
                }
                else
                {
                    throw new UnsupportedOperationException("Unsupported type for GELF message: " + element.getClass().getSimpleName());
                }
            }
        }

        if (json.toString().length() > 1)
        {
            json.deleteCharAt(json.toString().length() - 1);
            json.deleteCharAt(json.toString().length() - 1);
        }
        // No else needed: the list was empty, no need to trim the last comma and space

        json.append("]");
        return json.toString();
    }

    @NonNull
    String getSafeSizeString(@NonNull String input)
    {
        if (input.length() <= MAX_FIELD_LENGTH)
        {
            return input;
        }
        else
        {
            String safeSizeValue = input.substring(0, Math.min(input.length(), MAX_FIELD_LENGTH));
            //correctly process UTF-16 surrogate pairs
            if (safeSizeValue.length() > MAX_FIELD_LENGTH)
            {
                int correctedMaxWidth = Character.isLowSurrogate(safeSizeValue.charAt(MAX_FIELD_LENGTH)) ? MAX_FIELD_LENGTH - 1 : MAX_FIELD_LENGTH;
                safeSizeValue = safeSizeValue.substring(0, Math.min(safeSizeValue.length(), correctedMaxWidth));
            }
            return safeSizeValue;
        }
    }

    private boolean isNumber(Object attributeValue)
    {
        return attributeValue instanceof Integer ||
                attributeValue instanceof Double ||
                attributeValue instanceof Long ||
                attributeValue instanceof BigInteger ||
                attributeValue instanceof BigDecimal;
    }
}
