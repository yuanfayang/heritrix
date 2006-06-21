function magic_button() {
    window.addEventListener("load", linksGetter, true);
    getBrowser().contentDocument.location = "http://www.eggkeg.com";
}

function pinger() {
	//getBrowser().contentDocument.write("pinging...<br/>");
	http_request = new XMLHttpRequest();
	//http_request.onreadystatechange = showResponse;
	http_request.open('GET', 'http://0.0.0.0:8080/?method=heartBeat', true);
	http_request.send(null);
}

function showResponse() {
	getBrowser().contentDocument.write("["+http_request.responseText+"]");
}

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

