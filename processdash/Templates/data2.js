// -*- mode: c++ -*- <!--#server-parsed--><!--#resources file="data-js" -->
// <!--#echo defaultEncoding="html,javaStr" -->
/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
// 
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// 
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
// 
// E-Mail POC:  processdash-devel@lists.sourceforge.net
****************************************************************************/


				// if this (no-op) statement is removed, a
				// bug in Netscape causes this entire script
document.write(" ");		// to be overlooked...



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

/*
 * A tag that must be present in the data for this form to display this data.
 * This value is set by the presence of an <INPUT> element (probably of
 * TYPE=HIDDEN) with the NAME "requiredTag".  That element's VALUE property
 * will determine the value of this variable.
 */
var requiredTag = "";

var SILENT;

var ieVersion = 0;
var nsVersion = 0;

var AppletName = "DataApplet161";   // VERSION



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
    numElements = document.forms[formNum].elements.length;
    for (elemNum = 0; elemNum < numElements; elemNum++) {
      funcObj.func(document.forms[formNum].elements[elemNum]);
    } // for elemNum...
  } // for formNum...
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
 * Should read-only data be unlocked?
 */

var unlocked = (window.location.search.indexOf("unlock") != -1);

var unlockURL;
if (unlocked) {
  unlockURL = window.location.href.replace(/unlock/, "")
              .replace(/([?&])&/, "$1").replace(/[?&]$/, "");
  unlockHTML =
    '<br><A HREF="javascript:gotoUnLockURL();"><!--#echo Lock_Message --></A>';
} else {
  if (window.location.search == "") {
    unlockURL = window.location.href + "?unlock";
  } else {
    unlockURL = window.location.href + "&unlock";
  }
  unlockHTML = 
    '<br><A HREF="javascript:displayUnlockWarning();"><!--#echo Unlock_Message --></A>';
}


/*
 * Functions used for unlocking
 */

function displayUnlockWarning() {
if (window.confirm("<!--#echo var="Unlock_Warning" encoding="javaStr" -->"))
  displayDefaultMessage();
}

function displayDefaultMessage() {
  window.alert("<!--#echo var="DEFAULT_Message" encoding="javaStr" -->");
  gotoUnLockURL();
}
function gotoUnLockURL() {
  window.location.replace(unlockURL);
}

/*
 * Functions used for exporting
 */

function eesc(str) {
    str = escape(str);
    str = str.replace(/\//g, "%2F");
    str = str.replace(/\./g, "%2E");
    str = str.replace(/\+/g, "%2B");
    return str;
}

function writeExportHTML() {
    document.writeln("&nbsp; &nbsp; &nbsp; &nbsp;<!--#echo Export_To --> ");
    document.writeln("<A HREF='/reports/form2html.class'><!--#echo Export_To_HTML --></A>");
    var url = urlEncode(window.location.pathname +
		        window.location.hash +
		        window.location.search);
    url = "/reports/form2html.class?uri=" + url;
    url = urlEncode(url);
	
    document.writeln("<A HREF='/reports/excel.iqy?uri=" +url+ 
		     "&fullPage'><!--#echo Export_To_Excel --></A>");
}

function writeHelpLink() {
  document.writeln("&nbsp; &nbsp; &nbsp; &nbsp;<A HREF='/help/Topics/Planning/EnteringData.html' TARGET='_blank'><I><!--#echo Help_Dots --></I></A>");
}

function writeFooter() {
    if (!SILENT) {
	document.write('<span class=doNotPrint>');
	document.write(unlockHTML);
	writeExportHTML();
	writeHelpLink();
	document.write('</span>');
    }
}


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





var pageContainsElements = false;
	 
/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will block the event from reaching the
 * object if the object has an "isEditable" property which is set to false.
 */

function NScheckEditable() {
  if (this.className == "readOnlyElem")
    return false;
  else
    return true;
}


/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will invoke the notifyListener method on
 * the DataApplet just before the event occurs.
 */

function NSchangeNotify()  {
  NSchangeNotifyElem(this);
  return NScheckEditable();
}

function NSchangeNotifyElem(elem) {
  var value;
  if (elem.type.toLowerCase() == "checkbox")
      value = (elem.checked ? "true" : "false");
  else
      value = elem.value;

  var url = "http://" + self.location.host + "/dash/editForm" +
      "?s=" + sessionID + 
      "&c=" + changeCoupon +
      "&f=" + getElemNum(elem.id) +
      "&v=" + urlEncode(value);
  
  if (ieVersion == 0)
      self.frames["listener"].stop();

  self.frames["listener"].location.replace(url);

  /* FIXME - use new mechanism here 
  if (document.applets["NSDataAppl"] != null)
    document.applets["NSDataAppl"].notifyListener(elem.id, value);
  else if (document.all && document.all.NSDataAppl)
    document.all.NSDataAppl.notifyListener(elem.id, value);
  */
}


/*
 * Examine a form element during startup, and setup any required event
 * handlers.
 */

var NSelementList;

function NSregisterElement(elem) {

  pageContainsElements = true;

  if (elem.name.toLowerCase() == "requiredtag")
    requiredTag = elem.value;

  else if (elem.name && elem.name.indexOf("NOT_DATA") == -1) {
    NSelementList.push(elem);
    switch (elem.type.toLowerCase()) {

    case "select-one" :
    case "select-multiple":
      if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
      elem.onchange = NSchangeNotify;
      if (ieVersion > 0)
        IEsetupSelectValues(elem);
      break;

    case "text" :
    case "textarea" :
      if (debug) document.writeln("Setting "+elem.name+".onKeyDown<BR>");
      elem.onkeydown = NScheckEditable;
      if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
      elem.onchange = NSchangeNotify;
      break;

    case "checkbox":
      elem.onclick = NSchangeNotify;
      break;

    default:
      // elem is of type HIDDEN, RESET, SUBMIT, FILEUPLOAD, PASSWORD,
      // BUTTON, or RADIO.  no event handlers need to be setup
      // for these elements.
    }
  }
}
				// function object for use with elementIterate
function NSregisterElementObj() { this.func = NSregisterElement; }



/*
 * if the current element is a "select-one" element, make sure it all its
 * <OPTION> elements have their "value" property set (since IE databinding
 * binds select elements based on option-values).  If any <OPTION> is missing
 * a value, give it a value equal to its "text" property.
 */

function IEsetupSelectValues(elem) {
  if (elem.type.toLowerCase() != "select-one") return;

  var numOptions, optionNum;
  numOptions = elem.options.length;
  for (optionNum = 0;   optionNum < numOptions;  optionNum++)
    if (elem.options(optionNum).value == "")
      elem.options(optionNum).value = elem.options(optionNum).text;
}


var NSparameterString = "";     // a list of parameters for registration

function getElemID(elemNum) { return "dashelem_" + elemNum; }
function getElemNum(elemID) { return elemID.substring(9);   }
function getElemType(elem) { 
    switch (elem.type.toLowerCase()) {
    case "select-one" : case "select-multiple": return "s";
    case "checkbox": return "c";
    }
    return "t";
}

function NSAssignIdentifiers() {
  NSparameterString = "requiredTag=" + escStr(requiredTag);

  var elem;
  for (i = 0;   i < NSelementList.length;   i++) {
    elem = NSelementList[i];
    elem.id = getElemID(i);
    NSparameterString = NSparameterString +
	"&f" + i + "=" + getElemType(elem) + urlEncode(elem.name);
  }
}

function NSSetupElements() {
  NSelementList = new Array();
  elementIterate(new NSregisterElementObj());
  NSAssignIdentifiers();
}

var sessionID = "";
var changeCoupon = 0;
var uniqueID = 0;

function setSessionID(id) {
    sessionID = id;
    ack();
}

function getListenURL() {
    anticipateAcknowledge();
    uniqueID++;
    var uri = "/dash/listen?"+uniqueID+"&s="+sessionID+"&c="+changeCoupon;
    var url = "http://" + self.location.host + uri;
    return url;
}

function doListen() {
    var url = getListenURL();
    self.frames["listener"].location.replace(url);
}

var acknowledged = true;

function ack() { acknowledged = true; }
function anticipateAcknowledge() {
    acknowledged = false;
    self.setTimeout("checkAcknowledge()", 5000);
}
function checkAcknowledge() {
    if (!acknowledged) acknowledgedFailed();
}
function acknowledgedFailed() {
    for (i = 0;   i < NSelementList.length;   i++) {
	elem = NSelementList[i];
	paintField(getElemNum(elem.id), "NO CONNECTION", true, 0);
    }
}

function checkForUpdates() {
  var url = getListenURL();
  listener.src = url;
}

function paintField(elemNum, value, readOnly, coupon) {
  ack();

  if (elemNum == -1) {
      if (value == "page-refresh") {
	  var url = window.location.href;
	  if (window.location.search == "")
	      url += "?refresh";
	  else
	      url += "&refresh";
	  window.location.replace(url);
      }
      return;
  }

  if (elemNum >= NSelementList.length) return;
  var elem = document.getElementById(getElemID(elemNum));
  if (elem == null) return;


  elem.className = (readOnly ? "readOnlyElem" : "editableElem");
  switch (elem.type.toLowerCase()) {

    case "checkbox":
      elem.checked = (value == "true");
      elem.readOnly = readOnly;
      break;

    case "text" :
    case "textarea" :
      elem.value = value;
      elem.readOnly = readOnly;
      break;

    case "select-one" :
    case "select-multiple":
      elem.value = value;
      break;
  }
  changeCoupon = coupon;
}

function getRegistrationURL() {
    var uri = urlEncode(window.location.pathname +
		        window.location.hash +
		        window.location.search);
    return '/dash/register?uri=' + uri + '&' + NSparameterString;
}

function reregister() {
    NSAssignIdentifiers();
    changeCoupon = 0;
    self.frames["listener"].location.replace(getRegistrationURL());
}

/*
 * Netscape top-level setup procedure.
 */

function NSSetup() {

  if (debug) document.writeln("<p>Setting up under Netscape, ");

  NSSetupElements();

  if (pageContainsElements == true) {
    if (debug) document.writeln("<p>creating applet.");
    var url = getRegistrationURL();
    document.write('<iframe id=listener name=listener ');
    if (!debug) document.write('style="width:0; height:0; border:0" ');
    document.writeln('src="' + url + '"></iframe>');

    writeFooter();
  }
}


/***********************************************************************
 ***                       SNIFFING ROUTINES                         ***
 ***********************************************************************/


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
 ***                         MAIN PROCEDURE                          ***
 ***********************************************************************/


if (debug) document.writeln("Starting setup process.");

ieVersion = MSIEversion();
nsVersion = NSversion();

NSSetup();

if (debug) document.writeln("<p>done with data.js.");
