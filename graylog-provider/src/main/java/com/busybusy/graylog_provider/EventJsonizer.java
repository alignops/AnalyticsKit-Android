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

import android.support.annotation.NonNull;

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

class EventJsonizer
{
	final String host;
	final String GELF_SPEC_VERSION;

	EventJsonizer(String gelfSpecVersion, String host)
	{
		this.GELF_SPEC_VERSION = gelfSpecVersion;
		this.host = host;
	}

	@SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "WeakerAccess"})
	@NonNull
	String getJsonBody(AnalyticsEvent event)
	{
		Map<String, Object> eventAttributes = event.getAttributes() != null ? event.getAttributes() : new HashMap<String, Object>();
		Set<String>         attributes      = event.getAttributes() != null ? event.getAttributes().keySet() : new HashSet<String>();

		// guard clause: Libraries SHOULD not allow to send id as additional field (_id).
		if (attributes.contains("id"))
		{
			throw new UnsupportedOperationException("id is NOT allowed as an additional field according to the GELF spec!");
		}

		@SuppressWarnings("StringBufferReplaceableByString")
		StringBuilder sb = new StringBuilder("{");
		sb.append("\"version\": \"" + GELF_SPEC_VERSION + "\", ");
		sb.append("\"host\": \"" + host + "\", ");
		sb.append("\"short_message\": \"" + event.name() + "\", ");

		if (attributes.contains("full_message"))
		{
			sb.append("\"full_message\": \"" + eventAttributes.get("full_message") + "\", ");
		}
		// No else needed: this is an optional long message (may contain a backtrace)

		if (attributes.contains("timestamp"))
		{
			sb.append("\"timestamp\": " + eventAttributes.get("timestamp") + ", "); // user-provided timestamp
		}
		else
		{
			sb.append("\"timestamp\": " + System.currentTimeMillis() / 1000d + ", ");
		}

		if (attributes.contains("level"))
		{
			sb.append("\"level\": " + eventAttributes.get("level")); // user-provided syslog level
		}
		else
		{
			sb.append("\"level\": 6"); // Informational
		}

		for (String attribute : attributes)
		{
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
				Object attributeValue = eventAttributes.get(attribute);
				if (attributeValue instanceof String) // gotta use escape quotes for JSON strings
				{
					sb.append(", \"_" + attribute + "\": \"" + attributeValue + "\"");
				}
				else if (attributeValue instanceof List)
				{
					sb.append(", \"_" + attribute + "\": " + getJsonFromListRecursive((List) attributeValue));
				}
				else if (attributeValue instanceof Map)
				{
					//noinspection unchecked
					sb.append(", \"_" + attribute + "\": " + getJsonFromMapRecursive((Map<String, Object>) attributeValue));
				}
				else if (attributeValue instanceof Integer ||
						attributeValue instanceof Double ||
						attributeValue instanceof Long ||
						attributeValue instanceof BigInteger ||
						attributeValue instanceof BigDecimal)
				{
					sb.append(", \"_" + attribute + "\": " + attributeValue);
				}
				else if (attributeValue instanceof Exception)
				{
					sb.append(", \"_" + attribute + "\": \"" + attributeValue.toString() + "\"");
				}
				else if (attributeValue instanceof Error)
				{
					sb.append(", \"_" + attribute + "\": \"" + attributeValue.toString() + "\"");
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

	String getJsonFromMapRecursive(Map<String, Object> attributeMap)
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
				json.append("\"");
				json.append(innerAttribute);
				json.append("\": ");
				json.append(getJsonFromMapRecursive(innerMap));
				json.append(", ");
			}
			else if (innerAttributeValue instanceof List) // recursive case
			{
				json.append("\"");
				json.append(innerAttribute);
				json.append("\": ");
				json.append(getJsonFromListRecursive((List) innerAttributeValue));
				json.append(", ");
			}
			else
			{
				if (innerAttributeValue instanceof String) // gotta use escape quotes for JSON strings
				{
					json.append("\"");
					json.append(innerAttribute);
					json.append("\": \"");
					json.append(innerAttributeValue);
					json.append("\", ");
				}
				else if (innerAttributeValue instanceof Integer ||
						innerAttributeValue instanceof Double ||
						innerAttributeValue instanceof Long ||
						innerAttributeValue instanceof BigInteger ||
						innerAttributeValue instanceof BigDecimal)
				{
					json.append("\"");
					json.append(innerAttribute);
					json.append("\": ");
					json.append(innerAttributeValue);
					json.append(", ");
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

	String getJsonFromListRecursive(List attributeList)
	{
		StringBuilder json = new StringBuilder("[");

		for (Object element : attributeList)
		{
			if (element instanceof Map) // recursive case
			{
				//noinspection unchecked
				json.append(getJsonFromMapRecursive((Map<String, Object>) element));
				json.append(", ");
			}
			else if (element instanceof List) // recursive case
			{
				json.append(getJsonFromListRecursive((List) element));
				json.append(", ");
			}
			else
			{
				if (element instanceof String) // gotta use escape quotes for JSON strings
				{
					json.append("\"");
					json.append(element);
					json.append("\", ");
				}
				else if (element instanceof Integer ||
						element instanceof Double ||
						element instanceof Long ||
						element instanceof BigInteger ||
						element instanceof BigDecimal)
				{
					json.append(element);
					json.append(", ");
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
}
