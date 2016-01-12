/**
 *  ThingSpeak ZW100-a Logger
 *
 *  Copyright 2016 Tim Heckman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "ZW100-A TS Logger",
    namespace: "manofheck",
    author: "Tim Heckman",
    description: "Logs ZW100-A Data to ThingSpeak",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
        input "illuminance", "capability.illuminanceMeasurement", title: "Light Sensors", required: false, multiple: true     
    }

    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel id"
    }

    section ("ThinkSpeak write key...") {
        input "channelKey", "text", title: "Channel key"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(temperatures, "temperature", handleTemperatureEvent)
    subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(illuminance, "illuminance", handleIlluminanceEvent)

    updateChannelInfo()
    log.debug state.fieldMap //returns - [humidity:field2, temperature:field1]
}

//need to grab the event from each of the event handlers and pass to logField
def handleTemperatureEvent(evt) {
	logField(evt, 1)
}

def handleHumidityEvent(evt) {
	logField(evt, 2)
}

def handleIlluminanceEvent(evt) {
	logField(evt, 3)
}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "http://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
            state.channelInfo = response.data?.channel
        }
    }
    state.fieldMap = getFieldMap(state.channelInfo)
}


private logField(evt, fieldNum) {
    
    switch(fieldNum) {
   		case 1:
        	state.myMap.key1 = evt.value
        	break
     	case 2:
        	state.myMap.key2 = evt.value
        	break
    	case 3:
        	state.myMap.key3 = evt.value
        	break
        }

	runIn(5, UpdateTS)
}


def UpdateTS() {

    //log.debug "Logging to channel:1,${state.myMap.key1},2,${state.myMap.key2},3,${state.myMap.key3}"
    
    def url = "http://api.thingspeak.com/update?key=${channelKey}&1=${state.myMap.key1}&2=${state.myMap.key2}&3=${state.myMap.key3}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
          log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
}