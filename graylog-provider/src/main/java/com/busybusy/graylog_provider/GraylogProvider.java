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
import com.busybusy.analyticskit_android.AnalyticsKitProvider;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author John Hunt on 6/28/17.
 */

public class GraylogProvider implements AnalyticsKitProvider
{
    protected final OkHttpClient                    client;
    protected final String                          inputUrl;
    protected final String                          host;
    protected       PriorityFilter                  priorityFilter;
    protected       HashMap<String, AnalyticsEvent> timedEvents;
    protected       HashMap<String, Long>           eventTimes;
    protected GraylogResponseListener callbackListener = null;
    protected final EventJsonizer jsonizer;

    final String    GELF_SPEC_VERSION = "1.1";
    final MediaType JSON              = MediaType.parse("application/json; charset=utf-8");

    /**
     * Initializes a new {@code GraylogProvider} object.
     *
     * @param client          an initialized {@link OkHttpClient} instance
     * @param graylogInputUrl the URL of the Graylog HTTP input to use. Example: {@code http://graylog.example.org:12202/gelf}
     * @param graylogHostName the name of the host application that is sending events
     */
    public GraylogProvider(@NonNull OkHttpClient client, @NonNull String graylogInputUrl, @NonNull String graylogHostName)
    {
        this(client, graylogInputUrl, graylogHostName, new PriorityFilter()
        {
            @Override
            public boolean shouldLog(int priorityLevel)
            {
                return true; // Log all events, regardless of priority
            }
        });
    }

    /**
     * Initializes a new {@code GraylogProvider} object.
     *
     * @param client          an initialized {@link OkHttpClient} instance
     * @param graylogInputUrl the URL of the Graylog HTTP input to use. Example: {@code http://graylog.example.org:12202/gelf}
     * @param graylogHostName the name of the host application that is sending events
     * @param priorityFilter  the {@code PriorityFilter} to use when evaluating events
     */
    public GraylogProvider(@NonNull OkHttpClient client, @NonNull String graylogInputUrl, @NonNull String graylogHostName, PriorityFilter priorityFilter)
    {
        this.jsonizer = new EventJsonizer(GELF_SPEC_VERSION, graylogHostName);
        this.client = client;
        this.host = graylogHostName;
        this.inputUrl = graylogInputUrl;
        this.priorityFilter = priorityFilter;
    }

    /**
     * Specifies the {@code PriorityFilter} to use when evaluating event priorities.
     *
     * @param priorityFilter the filter to use
     * @return the {@code GraylogProvider} instance (for builder-style convenience)
     */
    public GraylogProvider setPriorityFilter(@NonNull PriorityFilter priorityFilter)
    {
        this.priorityFilter = priorityFilter;
        return this;
    }

    /**
     * Specifies the {@link GraylogResponseListener} that should listen for callbacks.
     *
     * @param callbackListener the instance that should be notified on each Graylog response
     * @return the {@code GraylogProvider} instance (for builder-style convenience)
     */
    public GraylogProvider setCallbackHandler(@NonNull GraylogResponseListener callbackListener)
    {
        this.callbackListener = callbackListener;
        return this;
    }

    @NonNull
    @Override
    public PriorityFilter getPriorityFilter()
    {
        return this.priorityFilter;
    }

    @Override
    public void sendEvent(@NonNull AnalyticsEvent event) throws IllegalStateException
    {
        if (event.isTimed()) // Hang onto it until it is done
        {
            ensureTimeTrackingMaps();

            this.eventTimes.put(event.name(), System.currentTimeMillis());
            timedEvents.put(event.name(), event);
        }
        else // Send the event to the Graylog input
        {
            logFromJson(event.name(), event.hashCode(), jsonizer.getJsonBody(event));
        }
    }

    /**
     * @see AnalyticsKitProvider
     */
    @Override
    public void endTimedEvent(@NonNull AnalyticsEvent timedEvent) throws IllegalStateException
    {
        ensureTimeTrackingMaps();

        long           endTime       = System.currentTimeMillis();
        Long           startTime     = this.eventTimes.remove(timedEvent.name());
        AnalyticsEvent finishedEvent = this.timedEvents.remove(timedEvent.name());

        if (startTime != null && finishedEvent != null)
        {
            double        durationSeconds = (endTime - startTime) / 1000;
            DecimalFormat df              = new DecimalFormat("#.###");
            finishedEvent.putAttribute("event_duration", df.format(durationSeconds));

            logFromJson(finishedEvent.name(), finishedEvent.hashCode(), jsonizer.getJsonBody(finishedEvent));
        }
        else
        {
            throw new IllegalStateException("Attempted ending an event that was never started (or was previously ended): " + timedEvent.name());
        }
    }

    /**
     * Retrieves the {@link EventJsonizer} in use by the Graylog provider.
     *
     * @return the JSON-izer
     */
    public EventJsonizer getJsonizer()
    {
        return jsonizer;
    }

    private void ensureTimeTrackingMaps()
    {
        if (this.eventTimes == null)
        {
            eventTimes = new HashMap<>(); // lazy initialization
        }
        if (this.timedEvents == null)
        {
            timedEvents = new HashMap<>(); // lazy initialization
        }
    }

    /**
     * Logs a JSON payload to your Graylog instance.
     *
     * @param eventName     the result of calling {@link AnalyticsEvent#name()}
     * @param eventHashCode the result of calling {@link AnalyticsEvent#hashCode()}
     * @param json          the payload to send to the Graylog input
     */
    public void logFromJson(@NonNull final String eventName, final int eventHashCode, final @NonNull String json)
    {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(inputUrl)
                .post(body)
                .build();

        // Prevent the old NetworkOnMainThreadException by using async calls
        client.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException exception)
            {
                providerCallback(new GraylogResponse(420, "An error occurred communicating with the Graylog server", eventName, eventHashCode, json));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                providerCallback(new GraylogResponse(response.code(), response.message(), eventName, eventHashCode, json));
            }

            void providerCallback(final GraylogResponse graylogResponse)
            {
                if (callbackListener != null)
                {
                    callbackListener.onGraylogResponse(graylogResponse);
                }
            }
        });
    }
}
