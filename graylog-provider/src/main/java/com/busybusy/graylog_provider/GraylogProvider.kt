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
import com.busybusy.analyticskit_android.AnalyticsKitProvider
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.text.DecimalFormat

/**
 * Initializes a new {@code GraylogProvider} object.
 *
 * @property client          an initialized {@link OkHttpClient} instance
 * @property graylogInputUrl the URL of the Graylog HTTP input to use. Example: {@code http://graylog.example.org:12202/gelf}
 * @param graylogHostName the name of the host application that is sending events
 * @property priorityFilter  the {@code PriorityFilter} to use when evaluating events
 *
 * @author John Hunt on 6/28/17.
 */
class GraylogProvider constructor(
    private val client: OkHttpClient,
    private val graylogInputUrl: String,
    graylogHostName: String,
    private val priorityFilter: PriorityFilter = PriorityFilter { true },
) : AnalyticsKitProvider {
    private val GELF_SPEC_VERSION = "1.1"
    private val jsonizer: EventJsonizer = EventJsonizer(GELF_SPEC_VERSION, graylogHostName)
    private val timedEvents: MutableMap<String, AnalyticsEvent> by lazy { mutableMapOf() }
    private val eventTimes: MutableMap<String, Long> by lazy { mutableMapOf() }
    var callbackListener: GraylogResponseListener? = null

    /**
     * Specifies the [GraylogResponseListener] that should listen for callbacks.
     *
     * @param callbackListener the instance that should be notified on each Graylog response
     * @return the `GraylogProvider` instance (for builder-style convenience)
     */
    fun setCallbackHandler(callbackListener: GraylogResponseListener): GraylogProvider {
        this.callbackListener = callbackListener
        return this
    }

    /**
     * Returns the filter used to restrict events by priority.
     *
     * @return the [PriorityFilter] instance the provider is using to determine if an
     *         event of a given priority should be logged
     */
    override fun getPriorityFilter(): PriorityFilter = priorityFilter

    /**
     * Sends the event using provider-specific code.
     *
     * @param event an instantiated event
     */
    @Throws(IllegalStateException::class)
    override fun sendEvent(event: AnalyticsEvent) {
        if (event.isTimed) { // Hang onto it until it is done
            eventTimes[event.name()] = System.currentTimeMillis()
            timedEvents[event.name()] = event
        } else { // Send the event to the Graylog input
            logFromJson(event.name(), event.hashCode(), jsonizer.getJsonBody(event))
        }
    }

    /**
     * End the timed event.
     *
     * @param timedEvent the event which has finished
     */
    @Throws(IllegalStateException::class)
    override fun endTimedEvent(timedEvent: AnalyticsEvent) {
        val endTime = System.currentTimeMillis()
        val startTime = eventTimes.remove(timedEvent.name())
        val finishedEvent = timedEvents.remove(timedEvent.name())
        if (startTime != null && finishedEvent != null) {
            val durationSeconds = ((endTime - startTime) / 1_000).toDouble()
            val df = DecimalFormat("#.###")
            finishedEvent.putAttribute("event_duration", df.format(durationSeconds))
            logFromJson(
                finishedEvent.name(),
                finishedEvent.hashCode(),
                jsonizer.getJsonBody(finishedEvent)
            )
        } else {
            error("Attempted ending an event that was never started (or was previously ended): ${timedEvent.name()}")
        }
    }

    /**
     * Logs a JSON payload to your Graylog instance.
     *
     * @param eventName     the result of calling [AnalyticsEvent.name]
     * @param eventHashCode the result of calling [AnalyticsEvent.hashCode]
     * @param json          the payload to send to the Graylog input
     */
    private fun logFromJson(eventName: String, eventHashCode: Int, json: String) {
        val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()
        val body: RequestBody = json.toRequestBody(jsonMediaType)
        val request: Request = Request.Builder()
            .url(graylogInputUrl)
            .post(body)
            .build()

        // Prevent the old NetworkOnMainThreadException by using async calls
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                providerCallback(GraylogResponse(420,
                    "An error occurred communicating with the Graylog server",
                    eventName,
                    eventHashCode,
                    json))
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                providerCallback(
                    GraylogResponse(
                        response.code,
                        response.message,
                        eventName,
                        eventHashCode,
                        json
                    )
                )
            }

            fun providerCallback(graylogResponse: GraylogResponse) =
                callbackListener?.onGraylogResponse(graylogResponse)
        })
    }
}
