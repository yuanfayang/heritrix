/*******************
 * Task processing *
 *******************/

var prefs = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
var prefs=Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
var originalProxy = prefs.getCharPref("network.proxy.http");
var originalPort = prefs.getIntPref("network.proxy.http_port");
var originalType = prefs.getIntPref("network.proxy.type");
var originalNoProxies = prefs.getCharPref("network.proxy.no_proxies_on");
		
var debugWin;
var debugDoc;
var debugDiv;
		

// records the http response logs, needed for the responseLogger functionality 		
var responseString = ""; 		
	
const NOTIFY_STATE_DOCUMENT =
        Components.interfaces.nsIWebProgress.NOTIFY_STATE_DOCUMENT;
const STATE_IS_DOCUMENT =
        Components.interfaces.nsIWebProgressListener.STATE_IS_DOCUMENT;
const STATE_STOP =
        Components.interfaces.nsIWebProgressListener.STATE_STOP; 

// add progress listener
var plistener = {
				onStateChange:function(aProgress,aRequest,aFlag,aStatus) {
                  
                  //debugOut("aFlag: (" + aFlag + ") aStatus: (" + aStatus+")");
                  if ((aFlag & (STATE_IS_DOCUMENT|STATE_STOP)) && (aStatus != 0)) {
	
				      loadErrorHandler();
                      
                  }
              	},
				onLocationChange:function(a,b,c){},
              	onProgressChange:function(a,b,c,d,e,f){},
                onStatusChange:function(a,b,c,d){},
                onSecurityChange:function(a,b,c){}  
};


function debugOut(msg) {

	if(debugDoc) { 
		var newNode = debugDoc.createTextNode("msg:"+msg);
		var brNode = debugDoc.createElement("BR");
		debugDiv.appendChild(newNode);
		debugDiv.appendChild(brNode);
	}
}
function launchDebugWindow() {
	debugWin = window.open("about:blank","debugWindow","menubar=no,location=no,resizable=no,scrollbars=yes,status=yes,height=200,width=400");
	debugDoc = debugWin.document;
	debugDoc.write("<div id=\"daDiv\">daDiv</div>");
	debugDoc.close();
	debugDiv = debugDoc.getElementById("daDiv");
}



// the observer for http responses
var httpResponseObserver =
{
  observe: function(subject, topic, data)
  {
  
    if (topic == "http-on-examine-response") {
    
        
   	 subject.QueryInterface(Components.interfaces.nsIHttpChannel);
         
     var tmpString = subject.URI.asciiSpec;
     
     //ignore the requests made to the harness, controller / where logs are posted
     var tmpIndex1 = tmpString.indexOf("http://127.0.0.1");
     var tmpIndex2 = tmpString.indexOf("method=responseLogger");
     
     if( (-1 == tmpIndex1) && (-1 == tmpIndex2) ) {    
      var str = subject.responseStatus + " " + subject.URI.asciiSpec + " " + taskData.URL;
      responseString+=str+"\n";     
     }       
           
    }
  },

  get observerService() {
    return Components.classes["@mozilla.org/observer-service;1"]
                     .getService(Components.interfaces.nsIObserverService);
  },

  register: function()
  {
  	this.observerService.addObserver(this, "http-on-examine-response", false);
  	
  },

  unregister: function()
  {
  	this.observerService.removeObserver(this, "http-on-examine-response");
  	
  }
};

  
// Starts the monkey task processing cycle
function startCycle() {
    
    taskGetterTimer = window.setInterval(getNewTask, 5000);

}

// Stops the monkey task processing cycle
function stopCycle() {
    debugOut("Old Cycle halted");
    window.clearInterval(taskGetterTimer);
}

// Send a request for a new task to the controller
function getNewTask() {

    stopCycle();

	debugOut("New cycle begun");

	//make sure the request is made to the live web    
    resetProxy();
    
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = processControllerResponse;
    http_request.open('GET', CONTROLLER_URL + '?method=getTask&mid=' + MONKEY_ID, true);
    http_request.send(null);
}

// Process newly received task
function processControllerResponse() {
    if (http_request.readyState == 4) {
        try {
            if (http_request.status != 200) {

                // start the cycle to fetch a new task from the controller
                startCycle();
            
            } else {

                taskData = http_request.responseText.parseJSON();
                // add auth data
                setAuthData(taskData);
		
	            // add a listener that will execute the operation when the page is loaded
    
    			//debugOut("Got task" + taskData.URL);
    
                window.addEventListener("load", eval(taskData.operation), true);

				//sets the proxy to point to the one specified in the taskData / live web
				setProxy(taskData);
		
				//progressListenerInit();
		
				//needed for responseLogger functionality
				
				if(taskData.operation == "responseLogger") {
					
					httpResponseObserver.register();
				
				}
		
				debugOut("loading document");
				getBrowser().contentDocument.location = taskData.URL;
		    
            }
        } catch (e) {
        	    
            	startCycle();
        }
    }
}

// add the progress listener
function progressListenerInit() {

	getBrowser().addProgressListener(plistener);

}

// remove the progress listener
function progressListenerUnInit() {

	getBrowser().removeProgressListener(plistener);

}

// function to handle a failed load of document
function loadErrorHandler() {

     //progressListenerUnInit();

	 //window.removeEventListener("unload", function() {progressListenerUnInit() }, false);

	notifyControllerOfFailure();
	 
     
     //progressListenerUnInit();


     //FIXME: temporary - closing window
     
     window.close();
     
     
}

//sets the browser proxy
function setProxy(taskData) {
    try {

		if(taskData.proxySettings.mode == "on") {

			prefs.setIntPref("network.proxy.type", 1);
			prefs.setCharPref("network.proxy.http", taskData.proxySettings.host);
			prefs.setIntPref("network.proxy.http_port", taskData.proxySettings.port);
			
		}
		else {
		
			resetProxy();
		
		}
	

    } catch (e) {
        ;
    }
}

//unset the browser proxy
function resetProxy() {
    try {
		prefs.setIntPref("network.proxy.type", 0);
		
    } catch (e) {
        ;
    }
}


function setAuthData(taskData) {
    try {

	   var isupports = Components.classes['@mozilla.org/network/http-auth-manager;1'].getService();
        var mHAService = isupports.QueryInterface(Components.interfaces.nsIHttpAuthManager);
        
        if(taskData.auth.scheme) {
			         mHAService.setAuthIdentity(taskData.auth.scheme, taskData.auth.host, taskData.auth.port,
           	    	 taskData.auth.authType, taskData.auth.realm, taskData.auth.path, taskData.auth.usedDomain,
           		     taskData.auth.userName, taskData.auth.userPassword);
                }
    } catch (e) {
        ;
    }
}

/* Notify the controller that a task has failed */
function notifyControllerOfFailure() {
  

	debugOut("removing event listener in failure routine");  
	window.removeEventListener("load", eval(taskData.operation), true);

	debugOut("notifying controller of failure");  

//	progressListenerUnInit();	
	
	
	resetProxy();

    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = taskReportAccepted;
    http_request.open('GET', CONTROLLER_URL + '?method=failTask&mid=' + MONKEY_ID + '&tid=' + taskData.id, true);
    http_request.send(null);
    
	
    
}

/* Notify the controller that a task was completed successfully */
function notifyControllerOfSuccess(taskId) {

	debugOut("removing event listener in success routine");    
    window.removeEventListener("load", eval(taskData.operation), true);

//	progressListenerUnInit();
    
    resetProxy();
    
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = taskReportAccepted;
    http_request.open('GET', CONTROLLER_URL + '?method=completeTask&mid=' + MONKEY_ID + '&tid=' + taskId, true);
    http_request.send(null);
    
    
}

function taskReportAccepted() {
    
       
    if (http_request.readyState == 4) {
    	
         startCycle();
    }
    
}



/**************************
 * End of task processing *
 **************************/


/******************************
 * Communication with harness *
 ******************************/

/* Sends a heartbeat signal to the harness servlet */
function pinger() {

	http_request = new XMLHttpRequest();
    http_request.open('GET', 'http://127.0.0.1:8082/harness?method=heartBeat', true);
    http_request.send(null);
}

/* set the url of the controller after receiving it from the harness */
function processControllerUrl() {
    if (http_request.readyState == 4) {
        var tempData = http_request.responseText.parseJSON();
        CONTROLLER_URL = tempData.controller_url;
        MONKEY_ID = tempData.monkey_id;
        startCycle();
    }
}

/*************************************
 * End of Communication with harness *
 *************************************/

/* Initializes the monkey plugin. */
function monkey_load() {
    
    // load helper scripts
    try {
        netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
        var jsLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].
            getService(Components.interfaces.mozIJSSubScriptLoader);
        jsLoader.loadSubScript("chrome://monkey/content/prototype.js");
        jsLoader.loadSubScript("chrome://monkey/content/link_getter.js");
        jsLoader.loadSubScript("chrome://monkey/content/json.js");
    } catch (e) {
        ;
    }

    
    prefs.setCharPref("browser.startup.homepage", "about:blank");

	// start periodical heartbeat notification of harness
    window.setInterval("pinger()", 10000);
    
    progressListenerInit();

	window.addEventListener("unload", function() {progressListenerUnInit() }, false);
	window.addEventListener("unload", function() {httpResponseObserver.unregister()}, false);
	
	resetProxy();

    // get the controller URL
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = processControllerUrl;
    http_request.open('GET', 'http://localhost:8082/harness?method=getControllerUrl', true);
    http_request.send(null);   
}

// HELPER FUNCTIONS

/* The magic button activates the plugin behavior.
   When the work is done, it will be removed and the behavior will be
   activated automatically
 */
function magic_button() {
    
    startCycle();
	
}

function showResponse() {
    if (http_request.readyState == 4) {
        getBrowser().contentDocument.write("["+http_request.status + "," + http_request.responseText+"]");
    }
}



function responseLogger() {
    
    debugOut("done loading document");
    
  	debugOut("removing event listener in logger routine");
   
    window.removeEventListener("load", responseLogger, true);

	//progressListenerUnInit();
    
    responseLoggerSubmitOperation();
}

// submit the recorded response logs to the specified server 
function responseLoggerSubmitOperation() {
    
    // specific to this function
    httpResponseObserver.unregister();

    var res = "";

    res += responseString + "\r\n";
  
    //reset the global response string
    responseString = "";

    resetProxy();
    
    http_request = new XMLHttpRequest();
    
    http_request.onreadystatechange = responseLoggerSubmitSuccess;
    
    http_request.open('POST',taskData.responseLoggerSubmitUrl, true);

    http_request.send(res);
    
      
}

// notify the controller of a failure / success of the response logger task
function responseLoggerSubmitSuccess() {
    
    try {
        if (http_request.readyState == 4) {
            if (http_request.status == 400) {
		        notifyControllerOfFailure();
            } else {
                notifyControllerOfSuccess(taskData.id);
            }
        }
    } catch (e) {
        startCycle();
    }
    
}

