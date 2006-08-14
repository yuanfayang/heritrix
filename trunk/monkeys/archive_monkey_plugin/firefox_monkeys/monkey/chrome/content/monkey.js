/*******************
 * Task processing *
 *******************/

// Starts the monkey task processing cycle
function startCycle() {
    taskGetterTimer = window.setInterval(getNewTask, 5000);
}

// Stops the monkey task processing cycle
function stopCycle() {
    window.clearInterval(taskGetterTimer);
}

// Send a request for a new task to the controller
function getNewTask() {
    stopCycle();
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = processControllerResponse;
    http_request.open('GET', CONTROLLER_URL + '?method=getTask&mid=MonkeyOne', true);
    http_request.send(null);
}

// Process newly received task
function processControllerResponse() {
    if (http_request.readyState == 4) {
        try {
            if (http_request.status != 200) {
                // handle failed task request
                startCycle();
            } else {
                taskData = http_request.responseText.parseJSON();
                // add auth data
                setAuthData(taskData);
                // add a listener that will execute the operation when the page is loaded
                window.addEventListener("load", eval(taskData.operation), true);
                getBrowser().contentDocument.location = taskData.URL;
            }
        } catch (e) {
            startCycle();
        }
    }
}

function setAuthData(taskData) {
    try {
        var isupports = Components.classes['@mozilla.org/network/http-auth-manager;1'].getService();
        var mHAService = isupports.QueryInterface(Components.interfaces.nsIHttpAuthManager);
        mHAService.setAuthIdentity(taskData.auth.scheme, taskData.auth.host, taskData.auth.port,
                taskData.auth.authType, taskData.auth.realm, taskData.auth.path, taskData.auth.usedDomain,
                taskData.auth.userName, taskData.auth.userPassword);
    } catch (e) {
        alert(e);
    }
}

/* Notify the controller that a task has failed */
function notifyControllerOfFailure() {
    window.removeEventListener("load", eval(taskData.operation), true);
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = taskReportAccepted;
    http_request.open('GET', CONTROLLER_URL + '?method=failTask&mid=MonkeyOne&tid=' + taskData.id, true);
    http_request.send(null);
}

/* Notify the controller that a task was completed successfuly */
function notifyControllerOfSuccess(taskId) {
    window.removeEventListener("load", eval(taskData.operation), true);
    http_request = new XMLHttpRequest();
    http_request.onreadystatechange = taskReportAccepted;
    http_request.open('GET', CONTROLLER_URL + '?method=completeTask&mid=MonkeyOne&tid=' + taskId, true);
    http_request.send(null);
}

function taskReportAccepted() {
    if (http_request.readyState == 4) {
        startCycle();
    }
}

// Adds a listener that activates task failure routines if a page
// fails to load
function addLoadProgressListener() {
    const NOTIFY_STATE_DOCUMENT =
        Components.interfaces.nsIWebProgress.NOTIFY_STATE_DOCUMENT;
    const STATE_IS_DOCUMENT =
        Components.interfaces.nsIWebProgressListener.STATE_IS_DOCUMENT;
    const STATE_STOP =
        Components.interfaces.nsIWebProgressListener.STATE_STOP; 
    // add progress listener
    var plistener = {
onStateChange:function(aProgress,aRequest,aFlag,aStatus) {
                  if ((aFlag & (STATE_IS_DOCUMENT|STATE_STOP)) && (aStatus != 0)) {
                      //        alert("Wait a moment! [" + aStatus + "]");
                      notifyControllerOfFailure();
                  }
              },
onLocationChange:function(a,b,c){},
                 onProgressChange:function(a,b,c,d,e,f){},
                 onStatusChange:function(a,b,c,d){},
                 onSecurityChange:function(a,b,c){}  
    };

    getBrowser().addProgressListener(plistener); 
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
    http_request.open('GET', 'http://0.0.0.0:8082/harness?method=heartBeat', true);
    http_request.send(null);
}

/* set the url of the controller after receiving it from the harness */
function processControllerUrl() {
    if (http_request.readyState == 4) {
        var tempData = http_request.responseText.parseJSON();
        CONTROLLER_URL = tempData.controller_url;
        LINKS_SUBMIT_URL = tempData.linksSubmit_url;
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
        alert(e);
    }

    // start periodical heartbeat notification of harness
    var prefs = Components.classes["@mozilla.org/preferences-service;1"].
        getService(Components.interfaces.nsIPrefBranch);
    prefs.setCharPref("browser.startup.homepage", "about:blank");
    window.setInterval("pinger()", 10000);

    // handle failed page loads
    addLoadProgressListener();

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
    //  startCycle();
}

function showResponse() {
    if (http_request.readyState == 4) {
        getBrowser().contentDocument.write("["+http_request.status + "," + http_request.responseText+"]");
    }
}
