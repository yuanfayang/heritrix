function old_magic_button() {
    window.addEventListener("load", linksGetter, true);
    getBrowser().contentDocument.location = "http://www.google.com";
}

function magic_button() {
    //window.addEventListener("load", linksGetter, true);
    //getBrowser().contentDocument.location = "http://www.google.com";
	
	http_request = new XMLHttpRequest();
	http_request.onreadystatechange = showResponse;
	http_request.open('GET', 'http://localhost:8081/monkey?method=getTask&mid=MonkeyOne', true);
	http_request.send(null);
}

/* Sends a ping request to the harness servlet */
function pinger() {
	//getBrowser().contentDocument.write("pinging...<br/>");
	http_request = new XMLHttpRequest();
	//http_request.onreadystatechange = showResponse;
	http_request.open('GET', 'http://0.0.0.0:8082/harness?method=heartBeat', true);
	http_request.send(null);
}

function showResponse() {
	if (http_request.readyState == 4) {
		getBrowser().contentDocument.write("["+http_request.responseText+"]");
	}
}

/* Initializes the monkey plugin. */
function monkey_load() {
	try {
		netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
		var jsLoader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].
					   getService(Components.interfaces.mozIJSSubScriptLoader);
		jsLoader.loadSubScript("chrome://monkey/content/prototype.js");
		jsLoader.loadSubScript("chrome://monkey/content/link_getter.js");
	} catch (e) {
		alert(e);
	}
    var prefs = Components.classes["@mozilla.org/preferences-service;1"].
                    getService(Components.interfaces.nsIPrefBranch);
    prefs.setCharPref("browser.startup.homepage", "about:blank");
    window.setInterval("pinger()", 10000);
}

