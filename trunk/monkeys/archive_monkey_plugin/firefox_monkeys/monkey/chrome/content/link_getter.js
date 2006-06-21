function linksGetter() {
	window.removeEventListener("load", linksGetter, true);
	var links = getBrowser().contentDocument.links;
/*	for(var i = 0; i < links.length; i++) {
	  var linkHref = getBrowser().contentDocument.createTextNode(links[i].href);
	  var lineBreak = getBrowser().contentDocument.createElement("br");
	  getBrowser().contentDocument.body.appendChild(linkHref);
	  getBrowser().contentDocument.body.appendChild(lineBreak);
	}*/
	
	$A(links).each(function(l) {
	  var linkHref = getBrowser().contentDocument.createTextNode(l.href);
	  var lineBreak = getBrowser().contentDocument.createElement("br");
	  getBrowser().contentDocument.body.appendChild(linkHref);
	  getBrowser().contentDocument.body.appendChild(lineBreak);
	});
}