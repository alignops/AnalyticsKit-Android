# AnalyticsKit-Android
Analytics framework for Android

## Installation
Add the JitPack repository to the end of your root build.gradle:
```groovy
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```

In your module's build.gradle file, add the dependency:
```groovy
dependencies {
		compile 'com.github.busybusy.AnalyticsKit-Android:analyticskit:0.8.2'
        ...
	}
```

You can include the implemented providers you want by adding them to the same dependency block:
```groovy
dependencies {
		...
        compile 'com.github.busybusy.AnalyticsKit-Android:mixpanel-provider:0.8.2'
	}
```

## Usage
In your Application's onCreate() method, register your provider SDKs as normal with your API keys. 
Then initialize AnalyticsKit-Android to work with those providers.

```java
AnalyticsKit.getInstance()
    .registerProvider(new MixpanelProvider(MixpanelAPI.getInstance(this, MIXPANEL_TOKEN)));
```

Send events where appropriate in your application code.

```java
new AnalyticsEvent("Your Event Name")
    .putAttribute("key", "value")
    .send();
```

The framework provides a ```ContentViewEvent``` to facilitate capturing content views:
```java
new ContentViewEvent()
    .putAttribute("screen_name", "Dashboard")
    .putAttribute("category", "navigation")
    .send();
```

### Event Priorities
By default, AnalyticsEvent objects have priority 0. However, you can
set any integer priority on your events. It is up to you to decide on your priority scheme 
and provider filtering.
```java
new AnalyticsEvent("Readme Read Event")
    .putAttribute("read", true)
    .setPriority(7)
    .send();
```

By default, providers will log all events regardless of priority. If desired, you can 
configure providers with a ```PriorityFilter``` so that only events that pass the 
```PriorityFilter```'s shouldLog() filter method will be logged by that provider. 
In the following example, only AnalyticsEvent objects with priority less than 10 will be 
logged by the Mixpanel provider:
```java
mixpanelProvider.setPriorityFilter(new AnalyticsKitProvider.PriorityFilter()
{
    @Override
    public boolean shouldLog(int priorityLevel)
    {
        return priorityLevel < 10;
    }
});
```

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
