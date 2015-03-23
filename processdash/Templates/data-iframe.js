// -*- mode: c++ -*- <!--#server-parsed--><!--#resources file="data-js" -->
// <!--#echo defaultEncoding="html,javaStr" -->
/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2000-2013 Tuma Solutions, LLC
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net
****************************************************************************/


                                // if this (no-op) statement is removed, a
                                // bug in Netscape causes this entire script
document.write(" ");            // to be overlooked...



/***********************************************************************
 ***                      GLOBAL VARIABLES                           ***
 ***********************************************************************/

/*
 * debug
 *
 * setting this variable to true will cause debugging output to be inserted
 * in your HTML file, and will cause ongoing output to be sent to stderr as
 * events occur.
 */
var debug = false;

if (debug) { document.write("running data.js<P>"); }

var SILENT;

var ieVersion = 0;
var nsVersion = 0;



/***********************************************************************
 ***                         UTILITY ROUTINES                        ***
 ***********************************************************************/

/*
 * The next block of code scans the document for form elements, and calls
 * a given function on each one.  The parameter must be a Function object
 * that contains a member called func which points to a function taking one
 * argument. Here is how to call someArbitraryFunction on all the elements
 * in the page:
 *
 *      function arbitraryObject() { this.func = someArbitraryFunction; }
 *      elementIterate(new arbitraryObject());
 *
 * This level of indirection is necessary because Netscape JavaScript does not
 * support passing functions as parameters.  Both Netscape and IE do support
 * passing objects as parameters, and objects can contain member functions.
 */

function elementIterate(funcObj) {
    var numForms, formNum, form, numElements, elementNum, elem;
    numForms = document.forms.length;
    for (formNum = 0; formNum < numForms; formNum++) {
        form = document.forms[formNum];
        if (shouldIgnore(form) == false) {
            numElements = document.forms[formNum].elements.length;
            for (elemNum = 0; elemNum < numElements; elemNum++) {
                funcObj.func(document.forms[formNum].elements[elemNum]);
            } // for elemNum...
        } // if shouldIgnore...
    } // for formNum...
}

function shouldIgnore(elem) {
  return shouldIgnoreStr(elem.name)
    || shouldIgnoreStr(elem.id)
    || shouldIgnoreStr(elem.className);
}

function shouldIgnoreStr(str) {
  if (str && str.indexOf("NOT_DATA") != -1)
    return true;
  else if (str && str.indexOf("notData") != -1)
    return true;
  else
    return false;
}


/*
 * escape backslashes and tabs.
 */

function escStr(s) { return s.replace(/\\/g,"\\\\").replace(/\t/g, "\\t"); }


/*
 * escape HTML entities.
 */

function textToHTML(text) {
    return text.replace(/&/, "&amp;").replace(/</, "&lt;")
        .replace(/>/, "&gt;").replace(/"/, "&quot;"); //")
}


/*
 * URL-encode strings in UTF-8 format
 */

var HEXDIGITS = "0123456789ABCDEF";
function toHex(b) {
    return "%" + HEXDIGITS.charAt(b >> 4) + HEXDIGITS.charAt(b & 0xF);
}

var SAFE_URI_CHARS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";

function urlEncode(text) {
    var c, s;
    var result = "";
    var i = 0;
    while (i < text.length) {
        c = text.charCodeAt(i++);

        // handle UTF-16 surrogates
        if (c >= 0xDC00 && c < 0xE000) continue;
        if (c >= 0xD800 && c < 0xDC00) {
            if (i >= text.length) continue;
            s = text.charCodeAt(i++);
            if (s < 0xDC00 || c >= 0xDE00) continue;
            c = ((c - 0xD800) << 10) + (s - 0xDC00) + 0x10000;
        }

        if (SAFE_URI_CHARS.indexOf(String.fromCharCode(c)) != -1) {
            result += String.fromCharCode(c);
        } else if (c < 0x80) {
            result += toHex(c);
        } else if (c < 0x800) {
            result += (toHex(0xC0+(c>>6)) + toHex(0x80+(c&0x3F)));
        } else if (c<0x10000) {
            result += (toHex(0xE0+(c>>12)) + toHex(0x80+(c>>6&0x3F)) +
                       toHex(0x80+(c&0x3F)));
        } else {
            result += (toHex(0xF0+(c>>18)) + toHex(0x80+(c>>12&0x3F)) +
                       toHex(0x80+(c>>6&0x3F)) + toHex(0x80+(c&0x3F)));
        }
    }
    return result;
}


/*
 * Generate a random number between 0 and 999.
 */

function randNum() {
    return Math.floor(Math.random() * 1000);
}



/*
 * return Microsoft Internet Explorer (major) version number, or 0 for other
 * browsers.  This function works by finding the "MSIE " string and
 * extracting the version number following the space, up to the decimal point
 * for the minor version, which is ignored.
 *
 * This code was extracted from the Microsoft Internet SDK.
 */

function MSIEversion() {
    var ua = window.navigator.userAgent;
    var msie = ua.indexOf ( "MSIE " );
    if ( msie > 0 )    // is Microsoft Internet Explorer; return version number
        return parseInt ( ua.substring ( msie+5, ua.indexOf ( ".", msie ) ) );
    else
        return 0;        // is other browser
}


/*
 * return Netscape version number, or 0 for other browsers.
 */

function NSversion() {
    var aN = window.navigator.appName;
    var aV = window.navigator.appVersion;

    if (aN == "Netscape")
        return parseInt (aV.substring (0, aV.indexOf(".", 0)));
    else
        return 0;
}


function isWindows() {
    var agt=navigator.userAgent.toLowerCase();
    return ( (agt.indexOf("win")!=-1) || (agt.indexOf("16bit")!=-1) )
}






/***********************************************************************
 ***                    PAGE FOOTER ROUTINES                         ***
 ***********************************************************************/

/*
 * Should read-only data be unlocked?
 */

var unlocked = (window.location.search.indexOf("unlock") != -1);

var unlockURL;
if (unlocked) {
    unlockURL = window.location.href.replace(/unlock/, "")
        .replace(/([?&])&/, "$1").replace(/[?&]$/, "");
    unlockHTML =
        '<A HREF="javascript:gotoUnLockURL();">' +
        '<!--#echo Lock_Message --></A>';
} else {
    if (window.location.search == "") {
        unlockURL = window.location.href + "?unlock";
    } else {
        unlockURL = window.location.href + "&unlock";
    }
    unlockHTML =
        '<A HREF="javascript:displayUnlockWarning();">' +
        '<!--#echo Unlock_Message --></A>';
}


/*
 * Functions used for unlocking
 */

function displayUnlockWarning() {
    var msg = "<!--#echo var='Unlock_Warning' encoding='javaStr' -->";
    if (window.confirm(msg))
        displayDefaultMessage();
}

function displayDefaultMessage() {
    window.alert("<!--#echo var='DEFAULT_Message' encoding='javaStr' -->");
    gotoUnLockURL();
}
function gotoUnLockURL() {
    window.location.replace(unlockURL);
}


/*
 * Functions used for exporting
 */

function writeExportHTML() {
    document.writeln("<!--#echo Export_To --> ");
    document.writeln("<A HREF='/reports/form2html.class'>" +
                     "<!--#echo Export_To_HTML --></A>");
    var url = urlEncode(window.location.pathname +
                        window.location.search);
    url = "/reports/form2html.class?uri=" + url;
    url = urlEncode(url);

    document.writeln("<A HREF='/reports/excel.iqy?uri=" +url+
                     "&fullPage'><!--#echo Export_To_Excel --></A>");
}

function writeHelpLink() {
    document.writeln("&nbsp; &nbsp; &nbsp; &nbsp;" +
                     "<A HREF='/help/Topics/Planning/EnteringData.html' " +
                     "TARGET='_blank'><I><!--#echo Help_Dots --></I></A>");
}

function writeFooter() {
    if (!SILENT) {
        document.write('<div id="dataExportFooter" class="doNotPrint">');
	<!--#if !READ_ONLY -->
        document.write(unlockHTML);
        document.write("&nbsp; &nbsp; &nbsp; &nbsp;");
	<!--#endif-->
        writeExportHTML();
        writeHelpLink();
        document.write('</div>');
    }
}

function writeExcelOnlyFooter() {
    document.write('<div id="dataExportFooter" class="doNotPrint">');

    var url = urlEncode(window.location.pathname +
                        window.location.search);
    url = "/reports/form2html.class?uri=" + url;
    url = urlEncode(url);

    document.writeln("<A HREF='/reports/excel.iqy?uri=" +url+
                     "&fullPage'><!--#echo Export_to_Excel --></A>");

    document.write('</div>');
}




/***********************************************************************
 ***                      MESSAGE DISPATCH                           ***
 ***********************************************************************
 *
 * These routines manage the dispatch of messages from the browser to
 * the dashboard.  Within the limits set by javascript (i.e., no
 * synchronization), they attempt to ensure guaranteed delivery of
 * all messages, in the order they were originally sent.
 */

var DISPATCH_IDLE = 0;
var DISPATCH_SENDING = 1;
var DISPATCH_RECEIVING = 2;
var DISPATCH_CONNECTION_LOST = 2;
var dispatchState = DISPATCH_IDLE;


var DISPATCH_MAX_MSG_SIZE = 2000;
var DISPATCH_ACK_TIMEOUT = 5000;
var DISPATCH_DONE_TIMEOUT = 30000;
var DISPATCH_FRAME = "listener";

var DISPATCH_NULL_MESSAGE_GENERATOR = "alert('need to call initDispatch')";
var DISPATCH_ACK_TIMEOUT_COMMAND = "alert('need to call initDispatch')";


var messageQueue = new Array();
var nextMessageID = randNum();
var ackTimeoutID = -1;
var waitTimeoutID = -1;

// public - should be called by other javascript on page load.
function initDispatch(nullMsgFunc, connLostFunc) {

    DISPATCH_NULL_MESSAGE_GENERATOR = nullMsgFunc;
    DISPATCH_ACK_TIMEOUT_COMMAND = connLostFunc;

    if (!self.frames[DISPATCH_FRAME]) {
        document.write('<iframe id="' + DISPATCH_FRAME +
                       '" name="' + DISPATCH_FRAME + '" ');
        if (!debug) document.write('style="width:0; height:0; border:0" ');
        document.writeln('></iframe>');
    }
}


// public - this method can be called freely by other javascript.
function addMessage(msg) {
    if (msg.length < DISPATCH_MAX_MSG_SIZE) {
        addMessageSimple(msg);
    } else {
        addMessageBatch(msg);
    }
}

// private (should only be called by dispatch logic)
function addMessageSimple(msg) {
    var msgID = nextMessageID++;
    var fullMessage = msg + getMsgIDSuffix(msgID);

    messageQueue.push(fullMessage);
    if (dispatchState == DISPATCH_IDLE)
        dispatchMessage(true);
}

// private (should only be called by dispatch logic)
function addMessageBatch(msg) {
    var pos = msg.indexOf("?");
    if (pos == -1) {
        // not much else we can do!
        addMessageSimple(msg);
	return;
    }

    var url = msg.substr(0, pos+1);
    var query = msg.substr(pos+1);
    var batchID = nextMessageID++;
    var partLen = DISPATCH_MAX_MSG_SIZE - 100;

    while (query.length > 0) {
        var queryPart = "";
        if (query.length > partLen) {
	    queryPart = query.substr(0, partLen) + "&batchID=" + batchID;
	    query = query.substr(partLen);
        } else {
	    queryPart = query + "&batchDoneID=" + batchID;
	    query = "";
	}
	
	var msgPart = url + queryPart;
	var msgPartID = nextMessageID++;
	var fullMessage = msgPart + getMsgIDSuffix(msgPartID);
	messageQueue.push(fullMessage);
    }

    if (dispatchState == DISPATCH_IDLE)
        dispatchMessage(true);
}


// private (should only be called by dispatch logic)
function getMsgIDSuffix(msgID) {
    return "&msgid=" + msgID;
}
function clearDispatchTimeout(timeoutID) {
    if (timeoutID != -1)
        self.clearTimeout(timeoutID);
    return -1;
}



// private (should only be called by dispatch logic)
function dispatchMessage(expectIdle) {
    if (messageQueue.length == 0)
        return;  // shouldn't happen?
    if (expectIdle && dispatchState != DISPATCH_IDLE)
        return;    // shouldn't happen?

    dispatchState = DISPATCH_SENDING;

    waitTimeoutID = clearDispatchTimeout(waitTimeoutID);

    var nextMessage = messageQueue[0];
    var messageURL = "http://" + self.location.host + nextMessage;

    ackTimeoutID = self.setTimeout("ackTimeout()", DISPATCH_ACK_TIMEOUT);
    self.frames[DISPATCH_FRAME].location.replace(messageURL);
}

// protected (should only be called by script generated by the dashboard
// in response to a delivered message)
function ackMessage(msgID) {
    ackTimeoutID = clearDispatchTimeout(ackTimeoutID);

    dispatchState = DISPATCH_RECEIVING;

    var currentMessage = messageQueue[0];
    var msgIDSuffix = getMsgIDSuffix(msgID);
    if (currentMessage.indexOf(msgIDSuffix) == -1)
        alert("messages were delivered out of order!!");
    else
        messageQueue.shift();

    waitTimeoutID = self.setTimeout("waitTimeout()", DISPATCH_DONE_TIMEOUT);
}

// protected (should only be called by script generated by the dashboard
// in response to a delivered message)
function messageDone(suggestedDelay) {
    waitTimeoutID = clearDispatchTimeout(waitTimeoutID);

    if (messageQueue.length > 0)
        dispatchMessage(false);
    else
        wait(suggestedDelay);
}

// private (should only be called by dispatch logic)
function ackTimeout() {
    if (ackTimeoutID != -1) {
        dispatchState = DISPATCH_CONNECTION_LOST;
        eval(DISPATCH_ACK_TIMEOUT_COMMAND);
    }
}


// private (should only be called by dispatch logic)
function wait(suggestedDelay) {
    dispatchState = DISPATCH_IDLE;
    waitTimeoutID = self.setTimeout("waitTimeout()", suggestedDelay);
}


// private (should only be called by dispatch logic)
function waitTimeout() {
    waitTimeoutID = -1;
    if (self.frames[DISPATCH_FRAME].stop)
        self.frames[DISPATCH_FRAME].stop();
    addMessage(eval(DISPATCH_NULL_MESSAGE_GENERATOR));
}




/***********************************************************************
 ***                     DATA HANDLING ROUTINES                      ***
 ***********************************************************************/

var pageContainsElements = false;

/*
 * A tag that must be present in the data for this form to display this data.
 * This value is set by the presence of an <INPUT> element (probably of
 * TYPE=HIDDEN) with the NAME "requiredTag".  That element's VALUE property
 * will determine the value of this variable.
 */
var requiredTag = "";

/*
 * A list of all the data elements on the page.
 */
var elementList;

/*
 * The textual values of the elements in the list above.
 */
var valueList;



/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will block the event from reaching the
 * object if the object has the class "readOnlyElem".
 */

function checkEditable() {
    return checkElemEditable(this);
}
function checkElemEditable(elem) {
    return (elem.className != "readOnlyElem");
}
function checkEditableKeystroke(event) {
    var e = event || window.event;
    if (e && e.keyCode == 9) return true; // allow the tab key
    return checkEditable();
}


/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will invoke the notifyListener method on
 * the DataApplet just before the event occurs.
 */

function changeNotify()  {
    changeNotifyElem(this);
    return checkEditable();
}

function changeNotifyElem(elem) {
    var elemNum = getElemNum(elem.id);
    var value = getElemValue(elem);

    if (value != valueList[elemNum]) {

        if (checkElemEditable(elem))
            addMessage("/dash/formEdit" +
                       "?s=" + sessionID +
                       "&c=" + changeCoupon +
                       "&f=" + elemNum +
                       "&v=" + urlEncode(value));
        else
            paintField(elemNum, valueList[elemNum], true, changeCoupon);

    }
}




/*
 * Examine a form element during startup, and setup any required event
 * handlers.
 */

function registerElement(elem) {

    pageContainsElements = true;

    if (elem.name.toLowerCase() == "requiredtag")
        requiredTag = elem.value;

    else if (shouldIgnore(elem) == false) {
        elementList.push(elem);
        switch (elem.type.toLowerCase()) {

        case "select-one" :
        case "select-multiple":
            if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
            elem.onchange = changeNotify;
            setupSelectValues(elem);
            break;

        case "text" :
        case "textarea" :
            if (debug) document.writeln("Setting "+elem.name+".onKeyDown<BR>");
            elem.onkeydown = checkEditableKeystroke;
            if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
            elem.onchange = changeNotify;
            break;

        case "checkbox":
            elem.onclick = changeNotify;
            break;

        default:
            // elem is of type HIDDEN, RESET, SUBMIT, FILEUPLOAD, PASSWORD,
            // BUTTON, or RADIO.  no event handlers need to be setup
            // for these elements.
        }
    }
}
                                // function object for use with elementIterate
function registerElementObj() { this.func = registerElement; }



/*
 * if the current element is a "select-one" element, make sure it all its
 * <OPTION> elements have their "value" property set (in particular, IE
 * doesn't bother to set the option-values).  If any <OPTION> is missing
 * a value, give it a value equal to its "text" property.
 */

function setupSelectValues(elem) {
    if (elem.type.toLowerCase() != "select-one") return;

    var numOptions, optionNum, opt;
    numOptions = elem.options.length;
    for (optionNum = 0;   optionNum < numOptions;  optionNum++) {
        opt = elem.options[optionNum];
        if (opt.hasAttribute ? !opt.hasAttribute("value") : !opt.value)
            opt.value = opt.text.replace(/^\s+|\s+$/g, '');
    }
}

function setSelectValue(select, value, readOnly) {
    maybeRemoveUnexpectedOption(select);

    select.value = value;
    if (select.value == value) {
	return;
    } else {
	addUnexpectedOption(select, value);
	select.value = value;
    }
}

function maybeRemoveUnexpectedOption(select) {
    var optLen = select.options.length;
    if (select.options[optLen - 1].className == "unexpected") {
        select.options.length = optLen - 1;
    }
}

function addUnexpectedOption(select, value) {
    var opt = new Option(value, value);
    opt.className = "unexpected";
    var optLenth = select.options.length;
    select.options[optLenth] = opt;
}


/*
 * Several utility routines that look up various attributes of an element
 */

function getElemID(elemNum) { return "dashelem_" + elemNum; }
function getElemNum(elemID) { return elemID.substring(9);   }
function getElemType(elem) {
    switch (elem.type.toLowerCase()) {
    case "select-one" : case "select-multiple": return "s";
    case "checkbox": return "c";
    }
    return "t";
}
function getElemValue(elem) {
    if (elem.type.toLowerCase() == "checkbox")
        return (elem.checked ? "true" : "false");
    else
        return elem.value;
}


var parameterString = "";     // a list of parameters for registration

function assignIdentifiers() {
    parameterString = "requiredTag=" + urlEncode(requiredTag);
    valueList = new Array();

    var elem;
    for (i = 0;   i < elementList.length;   i++) {
        elem = elementList[i];
        elem.id = getElemID(i);
        parameterString = parameterString +
            "&f" + i + "=" + getElemType(elem) + urlEncode(elem.name);
        valueList.push(getElemValue(elem));
    }
}

function setupElements() {
    elementList = new Array();
    elementIterate(new registerElementObj());
    assignIdentifiers();
}

var sessionID = "";
var changeCoupon = 0;

function setSessionID(id) {
    sessionID = id;
}

function getListenURL() {
    return "/dash/formListen?s="+sessionID+"&c="+changeCoupon;
}

function connectionLost() {
    for (i = 0;   i < elementList.length;   i++) {
        elem = elementList[i];
        paintField(getElemNum(elem.id), "NO CONNECTION", true, 0);
    }
}

function paintField(elemNum, value, readOnly, coupon) {
    if (elemNum == -1) {
        if (value == "page-refresh") {
            var url = window.location.href;
            if (window.location.search == "")
                url += "?refresh=" + randNum();
            else
                url += "&refresh=" + randNum();
            window.location.replace(url);
        }
        return;
    }

    if (elemNum >= elementList.length) return;
    var elem = document.getElementById(getElemID(elemNum));
    if (elem == null) return;

    elem.className = (readOnly ? "readOnlyElem" : "editableElem");
    switch (elem.type.toLowerCase()) {

    case "checkbox":
        elem.checked = (value == "true");
        elem.readOnly = readOnly;
        break;

    case "text" :
    case "hidden" :
    case "textarea" :
        elem.value = value;
        elem.readOnly = readOnly;
        break;

    case "select-one" :
    case "select-multiple":
        setSelectValue(elem, value, readOnly);
        break;
    }
    valueList[elemNum] = value;
    changeCoupon = coupon;
}

function getRegistrationURL() {
    var uri = urlEncode(window.location.pathname +
                        window.location.hash +
                        window.location.search);
    return '/dash/formRegister?uri=' + uri + '&' + parameterString;
}

function registerData() {
    assignIdentifiers();
    changeCoupon = 0;
    addMessage(getRegistrationURL());
}

/*
 * Top-level setup procedure.
 */

function setupData() {

    if (debug) document.writeln("<p>Setting up using javascript approach.");

    setupElements();

    if (pageContainsElements == true) {
        if (debug) document.writeln("<p>creating dispatch frame.");
        initDispatch("getListenURL()", "connectionLost()");

        if (debug) document.writeln("<p>registering for data notifications.");
        registerData();

        if (debug) document.writeln("<p>writing footer.");
        writeFooter();
<!--#if showExcelLink -->
    } else {
        writeExcelOnlyFooter();
<!--#endif-->
    }
}


/***********************************************************************
 ***                         MAIN PROCEDURE                          ***
 ***********************************************************************/


if (debug) document.writeln("Starting setup process.");

ieVersion = MSIEversion();
nsVersion = NSversion();

setupData();

if (debug) document.writeln("<p>done with data.js.");
