the /**
	extracts links from the loaded document
*/
function linksGetter() {
    
    
    window.removeEventListener("load", linksGetter, true);
    
    progressListenerUnInit();
    
    var links = getBrowser().contentDocument.links;
    var res = "";

    $A(links).each(function(l) {
            var linkHref = getBrowser().contentDocument.createTextNode(l.href);
            var lineBreak = getBrowser().contentDocument.createElement("br");
            res += linkHref.data + "\r\n";
            });
            
         
    
    linksSubmitOperation(res);
}

/**
	sends extracted links to the url specified in the task
*/

function linksSubmitOperation(links) {
    
    http_request = new XMLHttpRequest();
    
    http_request.onreadystatechange = linksSubmitSuccess;
    
    http_request.open('POST',taskData.linksSubmitUrl, true);

    http_request.send(links);
    
    
}

/**
	notifies the controller of a success/failure of the links submit operation
*/
function linksSubmitSuccess() {
    
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
