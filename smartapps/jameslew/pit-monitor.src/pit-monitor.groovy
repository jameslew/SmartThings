/**
 *  pit monitor
 *
 *  Author: jameslew@live.com
 *  Date: 2014-01-07
 */


// Automatically generated. Make future change here.
definition(
    name: "pit monitor",
    namespace: "jameslew",
    author: "jameslew@live.com",
    description: "Monitoring the BBQ Pit via @SmartThings",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")
{
	appSetting "grovestreamsKey"
}

preferences {
	section ("BBQ Temps to Threshhold At..."){
        input "targetPitTempF", "number", title: "Target Pit Temp", required: true, defaultValue: 225
		input "minPitTempF", "number", title: "Minimum Pit Temp", required: true, defaultValue: 200
    	input "maxPitTempF", "number", title: "Maximum Pit Temp", required: true, defaultValue: 250
    	input "firstAlertFoodTemp", "number", title: "First Alert Food Temp", required: true, defaultValue: 185
    	input "foodDoneTemp", "number", title: "Food Done Temperature", required: true, defaultValue: 195
    }
    section("BBQ Virtual Device"){
        input "pitDevice", "capability.temperatureMeasurement", title: "pitTemp"
        input "foodDevice", "capability.temperatureMeasurement", title: "foodTemp"
    }
     
	section ("In addition to push notifications, send text alerts to...") {
		input "phone1", "phone", title: "Phone Number 1", required: false
		input "phone2", "phone", title: "Phone Number 2", required: false
		input "phone3", "phone", title: "Phone Number 3", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
}

mappings {
  path("/updateTemps/:pitTemp/:foodTemp") {
    action: [
      PUT: "updateTemps"
    ]
  }
}

void updateTemps() {
        log.debug "received input"
        log.debug "update, request: params: ${params}"
        def pitTemp = params.param2.toFloat()
        def foodTemp = params.param3.toFloat()
		processResponse(pitTemp, foodTemp)
        //sendEvent(foodDevice, [foodTemp: ${foodTemp}])
        //sendEvent(pitDevice, [pitTemp: ${pitTemp}])
}

def processResponse(pitTemp, foodTemp) {
	if (appSettings.grovestreamsKey != null)
        httpPut('http://grovestreams.com:80/api/feed?api_key='+appSettings.grovestreamsKey+'&compid=bbqPit&pitProbe='+pitTemp+'&foodProbe='+foodTemp,'')
        { gsResponse ->
            if (gsReponse != null) {
            	log.debug gsResponse.data
            }
        }
    
	if(state.lastPitTempF == null || state.lastFoodTempF == null || state.lastFirstAlertFoodTempF == null) {
    	state.lastPitTempF = pitTemp;
        state.lastFoodTempF = foodTemp;
        state.lastFirstAlertFoodTempF = (float) 45.0;
    }
    
    log.debug "*** Trace Block ***"
    log.debug "Latest: ${pitTemp}, ${foodTemp}"
    log.debug "Threshholds: ${minPitTempF}, ${maxPitTempF}, ${firstAlertFoodTemp}, ${foodDoneTemp}"
    log.debug "State: ${state.lastPitTempF}, ${state.lastFoodTempF}, ${state.lastFirstAlertFoodTempF}"
    log.debug "*** End Trace Block ***"
    
    if (pitTemp > maxPitTempF) {
        if (pitTemp >= state.lastPitTempF + 3.0 ) {
            state.lastPitTempF = pitTemp
            notifyEveryone("The BBQ Pit is Too Hot: ${pitTemp}F")
        } else {log.debug "not enough high temp change"}
    } else { state.lastPitTempF = pitTemp }
    
    if (pitTemp < minPitTempF) {
        if (pitTemp <= state.lastPitTempF - 3.0 ) {
            state.lastPitTempF = pitTemp
            notifyEveryone("The BBQ Pit is Too Cold: ${pitTemp}F")  
        } else { log.debug "not enough low temp change" }
    } else { state.lastPitTempF = pitTemp }
    
    if (foodTemp > foodDoneTemp) {
        if (foodTemp  >= state.lastFoodDoneTemp + 3.0 ) {
            state.lastFoodDoneTemp = foodTemp 
            notifyEveryone("BBQ is done, food is: ${foodTemp} degrees")
        } else {log.debug "temp hasn't changed enough"}
    } else { state.lastFoodDoneTemp = foodTemp }
        
	if (foodTemp > firstAlertFoodTemp) {
        if (foodTemp  >= state.lastFirstAlertFoodTempF + 3.0 ) {
            state.lastFirstAlertFoodTempF = foodTemp 
            notifyEveryone("BBQ getting closer to done, food is: ${foodTemp} degrees")
        } else {log.debug "temp hasn't changed enough"}
    } else { state.lastFirstAlertFoodTempF = foodTemp }

}

def logTraceWithPush(message) {
    log.debug message
	sendPush(message)
}

private notifyEveryone(message) {
    log.debug "We got one! ${message}"
    send(message)
}

private send(message) {
	sendPush(message)
	if (settings.phone1) {
		sendSms phone1, message
	}
	if (settings.phone2) {
		sendSms phone2, message
	}
	if (settings.phone3) {
		sendSms phone3, message
	}
}

