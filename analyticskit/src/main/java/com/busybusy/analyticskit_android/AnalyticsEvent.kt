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

import androidx.annotation.NonNull
import java.io.Serializable

/**
 * Defines information that is needed to distribute the event to the registered analytics providers.
 *
 * @author John Hunt on 3/5/16.
 */
open class AnalyticsEvent(
    @NonNull val name: String,
    var attributes: MutableMap<String, Any>? = null,
    var timed: Boolean = false,
    var priorityLevel: Int = 0,
) : Serializable {

    /**
     * Access the event name.
     *
     * @return the name of the custom event
     */
    fun name(): String {
        return name
    }

    /**
     * Adds an attribute to the event.
     *
     * @param attributeName the name of the attribute (should be unique)
     * @param value         the [Object] to associate with the name given
     * @return the [AnalyticsEvent] instance
     */
    fun putAttribute(attributeName: String, value: Any): AnalyticsEvent {
        // guard clause - make sure the dictionary is initialized
        if (attributes == null) {
            attributes = LinkedHashMap()
        }
        attributes!![attributeName] = value
        return this
    }

    /**
     * Gets the priority of this event.
     *
     * @return the priority of the event. Returns `0` when [.setPriority] has not been called.
     */
    fun getPriority(): Int = priorityLevel

    /**
     * Sets the priority of the event. The event defaults to `0` when this method is not called.
     *
     *
     * **Note:** It is up to the developer to define what priority scheme to use (if any).
     *
     * @param priorityLevel the priority the event should use
     * @return the [AnalyticsEvent] instance (for builder-style convenience)
     */
    fun setPriority(priorityLevel: Int): AnalyticsEvent {
        this.priorityLevel = priorityLevel
        return this
    }

    /**
     * Access a single attribute of this event.
     *
     * @param name the name the of the attribute to retrieve
     * @return the value associated with the given attribute name.
     * Returns `null` if the attribute has not been set.
     */
    fun getAttribute(name: String): Any? {
        return if (attributes == null) null else attributes!![name]
    }

    /**
     * Indicates if this event is a timed event.
     *
     * @return `true` if the event has been set to be a timed event. Returns `false` otherwise.
     */
    fun isTimed(): Boolean = timed

    /**
     * Sets whether this event should capture timing.
     *
     * @param timed `true` to set the event to track the time
     * @return the [AnalyticsEvent] instance
     */
    fun setTimed(timed: Boolean): AnalyticsEvent {
        this.timed = timed
        return this
    }

    /**
     * Sends the event out to the registered/specified providers.
     * This is a convenience method that wraps [AnalyticsKit.logEvent]
     */
    fun send(): AnalyticsEvent {
        AnalyticsKit.getInstance().logEvent(this)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyticsEvent) return false

        if (name != other.name) return false
        if (attributes != other.attributes) return false
        if (timed != other.timed) return false
        if (priorityLevel != other.priorityLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (attributes?.hashCode() ?: 0)
        result = 31 * result + timed.hashCode()
        result = 31 * result + priorityLevel
        return result
    }

    override fun toString(): String {
        return "AnalyticsEvent(name='$name', attributes=$attributes, timed=$timed, priorityLevel=$priorityLevel)"
    }

    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 1
    }
}
