<%
	/**
	 * An include file that handles the "look" and navigation of a web page. 
	 * Include at top (where you would normally begin the HTML code).
	 * If used, the include "foot.jsp" should be included at the end of the HTML
	 * code. It will close any table, body and html tags left open in this one.
	 * Any custom HTML code is thus placed between the two.
	 *
	 * The following variables must exist prior to this file being included:
	 *
	 * String title - Title of the web page
	 *
	 * @author Kristinn Sigurdsson
	 */
%>

<html>
	<head>
		<title>Heritrix: <%=title%></title>
		<link rel="stylesheet" href="/admin/css/heritrix.css">
		<script type="text/javascript">
		<!--
			// MENU MOUSE OVER
			function menuOver(itemName) {
			 clearTimeout(timeOn)
			 menuActive = 1
			}

			// MENU MOUSE OUT
			function menuOut(itemName) {
			 if(document.layers) {
				menuActive = 0
				timeOn = setTimeout("hideAllMenus()", 400)
			  }
			}

			// SET BACKGROUND COLOR
			function getImage(name) {
			  if (document.layers) {
				return findImage(name, document);
			  }
			  return null;
			}

			function findImage(name, doc) {
			  var i, img;
			  for (i = 0; i < doc.images.length; i++)
				if (doc.images[i].name == name)
				  return doc.images[i];
			  for (i = 0; i < doc.layers.length; i++)
				if ((img = findImage(name, doc.layers[i].document)) != null) {
				  img.container = doc.layers[i];
				  return img;
				}
			  return null;
			}

			function getImagePageLeft(img) {
			  var x, obj;
			  if (document.layers) {
				if (img.container != null)
				  return img.container.pageX + img.x;
				else
				  return img.x;
			  }
			  return -1;
			}

			function getImagePageTop(img) {
			  var y, obj;
			  if (document.layers) {
				if (img.container != null)
				  return img.container.pageY + img.y;
				else
				  return img.y;
			  }
			  return -1;
			}

			var timeOn = null
			numMenus = 4;
			document.onmouseover = hideAllMenus;
			document.onclick = hideAllMenus;
			window.onerror = null;

			function getStyleObject(objectId) {
				// cross-browser function to get an object's style object given its id
				if(document.getElementById && document.getElementById(objectId)) {
				// W3C DOM
				return document.getElementById(objectId).style;
				} else if (document.all && document.all(objectId)) {
				// MSIE 4 DOM
				return document.all(objectId).style;
				} else if (document.layers && document.layers[objectId]) {
				// NN 4 DOM.. note: this won't find nested layers
				return document.layers[objectId];
				} else {
				return false;
				}
			} // getStyleObject

			function changeObjectVisibility(objectId, newVisibility) {
				// get a reference to the cross-browser style object and make sure the object exists
				var styleObject = getStyleObject(objectId);
				if(styleObject) {
				styleObject.visibility = newVisibility;
				return true;
				} else {
				//we couldn't find the object, so we can't change its visibility
				return false;
				}
			} // changeObjectVisibility


			function showMenu(menuNumber, eventObj, labelID) {
				hideAllMenus();
				if(document.layers) {
				img = getImage("img" + menuNumber);
				x = getImagePageLeft(img);
				y = getImagePageTop(img);
				menuTop = y + 10; // LAYER TOP POSITION
				eval('document.layers["menu'+menuNumber+'"].top="'+menuTop+'"');
				eval('document.layers["menu'+menuNumber+'"].left="'+x+'"');
				}
				eventObj.cancelBubble = true;
				var menuId = 'menu' + menuNumber;
				if(changeObjectVisibility(menuId, 'visible')) {
				return true;
				} else {
				return false;
				}
			}

			function hideAllMenus() {
				for(counter = 1; counter <= numMenus; counter++) {
				changeObjectVisibility('menu' + counter, 'hidden');
				}
			}

			function moveObject(objectId, newXCoordinate, newYCoordinate) {
				// get a reference to the cross-browser style object and make sure the object exists
				var styleObject = getStyleObject(objectId);
				if(styleObject) {
				styleObject.left = newXCoordinate;
				styleObject.top = newYCoordinate;
				return true;
				} else {
				// we couldn't find the object, so we can't very well move it
				return false;
				}
			} // moveObject


			// initialize hacks whenever the page loads
			window.onload = initializeHacks;

			// setup an event handler to hide popups for generic clicks on the document
			function initializeHacks() {
				// this ugly little hack resizes a blank div to make sure you can click
				// anywhere in the window for Mac MSIE 5
				if ((navigator.appVersion.indexOf('MSIE 5') != -1)
				&& (navigator.platform.indexOf('Mac') != -1)
				&& getStyleObject('blankDiv')) {
				window.onresize = explorerMacResizeFix;
				}
				resizeBlankDiv();
				// this next function creates a placeholder object for older browsers
				createFakeEventObj();
			}



			function createFakeEventObj() {
				// create a fake event object for older browsers to avoid errors in function call
				// when we need to pass the event object to functions
				if (!window.event) {
				window.event = false;
				}
			} // createFakeEventObj



			function resizeBlankDiv() {
				// resize blank placeholder div so IE 5 on mac will get all clicks in window
				if ((navigator.appVersion.indexOf('MSIE 5') != -1)
				&& (navigator.platform.indexOf('Mac') != -1)
				&& getStyleObject('blankDiv')) {
				getStyleObject('blankDiv').width = document.body.clientWidth - 20;
				getStyleObject('blankDiv').height = document.body.clientHeight - 20;
				}
			}
		-->
		</script>
	</head>

	<body>
		<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%">
			<tr>
				<td width="150" height="60" valign="top" nowrap>
					<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%">
						<tr>
							<td class="heritrix">
								<a class="heritrix" href="/admin/main.jsp">Heritrix</a>
							</td>
						</tr>
						<tr>
							<td class="subheading">
								<%=title%>
							</td>
						</tr>
					</table>
				</td>
				<td>
					&nbsp;&nbsp;
				</td>
				<td width="100%">
					<iframe name="frmHeadStatus" src="/admin/iframes/headstatus.jsp" width="100%" height="60" frameborder="0" ></iframe>
				</td>
			</tr>
			<tr>
				<td colspan="3" height="20">
					<table border="0" cellspacing="0" cellpadding="0" width="100%" height="20">
						<tr>
							<td class="menu" nowrap>
								<div id="jobs">
									<img src="/admin/images/blank.gif" width="5" height="10" border="0" name="img1">
									<a class="MenuHeader" onMouseover="return !showMenu('1', event, 'jobs'); menuOver('rollimg2');"  onMouseOut =" window.status='';return true;">Jobs</a>
								</div>
								<div id="menu1" style="width: 150px; height: 72px; position:absolute; z-index:1; visibility:hidden" onmouseover="event.cancelBubble = true;" class="menuitem">
									<table border=0 cellpadding=2 cellspacing=0 width="100%">
										<tr id="1b" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/jobs/new.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">New job</a>
											</td>
										</tr>
										<tr id="1a" class="menu">
											<td>
												<a class="menuitem"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Current job</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/jobs/updatecurrent.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Update</a>
											</td>
										</tr>
										<tr id="1ab" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/jobs/viewstatistics.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">View statistics</a>
											</td>
										</tr>
										<tr id="1c" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/jobs/pending.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Pending jobs</a>
											</td>
										</tr>
										<tr id="1d" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/jobs/completed.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Completed jobs</a>
											</td>
										</tr>
									</table>
								</div>
							</td>
							<td class="menu" nowrap>
								<div id="settings">
									<img src="/admin/images/blank.gif" width="5" height="10" border="0" name="img1">
									<a class="MenuHeader" onMouseover="return !showMenu('2', event, 'settings'); menuOver('rollimg2');"  onMouseOut =" window.status='';return true;">Settings</a>
								</div>
								<div id="menu2" style="width: 150px; height: 35px; position:absolute; z-index:1; visibility:hidden" onmouseover="event.cancelBubble = true;" class="menuitem">
									<table border=0 cellpadding=2 cellspacing=0 width="100%">
										<tr id="1b" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/settings/defaultcrawlorder.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Default crawl order</a>
											</td>
										</tr>
										<tr id="1c" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="#" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Access tool</a>
											</td>
										</tr>
									</table>
								</div>
							</td>
							<td class="menu" nowrap>
								<div id="logs">
									<img src="/admin/images/blank.gif" width="5" height="10" border="0" name="img1">
									<a class="MenuHeader" onMouseover="return !showMenu('3', event, 'logs'); menuOver('rollimg2');"  onMouseOut =" window.status='';return true;">Logs/reports</a>
								</div>
								<div id="menu3" style="width: 150px; height: 52px; position:absolute; z-index:1; visibility:hidden" onmouseover="event.cancelBubble = true;" class="menuitem">
									<table border=0 cellpadding=2 cellspacing=0 width="100%">
										<tr id="1a" class="menu">
											<td>
											<a class="menuitem"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Logs</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/reports/logs.jsp?log=crawl.log" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">crawl.log</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/reports/logs.jsp?log=local-errors.log" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">local-errors.log</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/reports/logs.jsp?log=progress-statistics.log" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">progress-statistics.log</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/reports/logs.jsp?log=runtime-errors.log" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">runtime-errors.log</a>
											</td>
										</tr>
										<tr id="1aa" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menusubitem" href="/admin/reports/logs.jsp?log=uri-errors.log" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">uri-errors.log</a>
											</td>
										</tr>
										<tr id="1b" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/reports/frontier.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Frontier report</a>
											</td>
										</tr>
										<tr id="1c" class="menu">
											<td onMouseover="this.style.backgroundColor = '#0000FF';" onMouseout =" this.style.backgroundColor = '#5555FF';" width="100%">
												<a class="menuitem" href="/admin/reports/threads.jsp" onMouseOut="menuOut('rollimg2');" onMouseOver="menuOver('rollimg2');"><img src="/admin/images/blank.gif" width="5" height="2" border="0">Threads report</a>
											</td>
										</tr>
									</table>
								</div>
							</td>
							<td class="menu" width="100%">
							</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="3" height="100%" valign="top">
					<!-- MAIN BODY -->
