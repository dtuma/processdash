// -*- mode: c++ -*-
/****************************************************************************
// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
// 
// E-Mail POC:  ken.raisor@hill.af.mil
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




/***********************************************************************
 ***             UTILITY ROUTINES (used by both browsers)            ***
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




/***********************************************************************
 ***                  Internet Explorer definitions                  ***
 ***********************************************************************/

/*
 * global variables used by IE functions
 */
var IEparameterString = "";     // a list of parameters for the IEDataApplet.
var IEfieldNum = 0;             // start numbering elements with 0.

/*
 * determine if the given element is read only, and reconfigure its
 * appearance and properties appropriately. This routine should be only 
 * be called AFTER the page has finished loading and the IEDataApplet
 * has been created.
 */

function IEsetupReadOnly(elem) {
  if (elem.dataFld != null) {
    if (elem.readOnly = (! IEDataAppl.isEditable(elem.dataFld))) {
      elem.style.backgroundColor = IEDataAppl.readOnlyColor();
      elem.tabIndex = -1;
    } else {
      elem.style.backgroundColor = "";
      elem.tabIndex = "";
    }
  }
  //elem.disabled = false;
}

                                // call setupReadOnly on the "this" element.
function IEcheckEditable() { IEsetupReadOnly(this); }
                                // call setupReadOnly on all form elements.
function IEsetupReadOnlyObj() { this.func = IEsetupReadOnly; }
function IEscanForReadOnly(event) { elementIterate(new IEsetupReadOnlyObj()); }



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


/*
 * Examine a form element during page startup.  Save information about the
 * element for IEDataApplet use. Initially make the element disabled, so the
 * user cannot enter the element or type into it until the applet is ready.
 */

function IEregisterElement(elem) {
                         // only setup this element if it has a NAME property.
  if (elem.name != null && elem.name != "") {

                                                  // if this is the requiredTag
    if (elem.name.toLowerCase() == "requiredtag") // <INPUT> element, save the
      requiredTag = elem.value;                   // requiredTag value.

    else if (elem.dataSrc == "") {         // if elem isn't already bound,
      elem.dataSrc = "#IEDataAppl";        // bind it to the IEDataApplet
      elem.dataFld = "field" + IEfieldNum; // with a new, unique dataFld value,
 					   // and add info about the element to
				           // the IEparameterString.
      IEparameterString = IEparameterString + 
	'<param name=field'+ IEfieldNum +' value="'+ escStr(elem.name) +'">'+
	'<param name=type' + IEfieldNum +' value="'+ elem.type         +'">';
      IEfieldNum++;
    }
    IEsetupSelectValues(elem);	// ensure <SELECT> elements are setup.

				// disable the element until the IEDataApplet
    //elem.disabled = true;	// is ready for the user to interact with it.

        // IEcheckEditable cannot be called until the IEDataApplet is
        // created.  We are safe setting up this handler, however, since the
        // element is disabled and thus cannot receive the focus until the
        // IEDataApplet is ready.
    elem.onfocus = IEcheckEditable;
    // elem.onblur = IEscanForReadOnly;
  }
}
				// function object for use by elementIterate
function IEregisterElementObj() { this.func = IEregisterElement; }



/*
 * Internet Explorer top-level setup procedure.
 */

function IEsetup() {
				// scan the document for form elements and
				// perform setup for each.
  elementIterate(new IEregisterElementObj());

				// if any elements were found,
  if (IEparameterString != "") {

				// add a data applet to the page.
    document.writeln('<applet id=IEDataAppl'+
		            ' codebase="/1/IE"' +
		            ' code=pspdash.data.IEDataApplet'+
		            ' width=1 height=1>');
    document.writeln(IEparameterString);
    if (requiredTag != "")
      document.writeln('<param name=requiredTag value="' + requiredTag +'">');
    document.writeln('</applet>');

    IEDataAppl.ondatasetcomplete = IEscanForReadOnly;
    IEDataAppl.ondatasetchanged  = IEscanForReadOnly;
  }
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




/***********************************************************************
 ***                      Netscape definitions                       ***
 ***********************************************************************/

/*
 * Note about our current version of Netscape:
 *
 * There appear to be bugs in netscape's JavaScript event handling code.
 * 1.   In a windows environment, attempting to set more than one event
 *      handler on an object causes all but the last event handler to be
 *      forgotten.  (consequence: TEXT fields cannot have both KeyPress
 *      handlers and Blur / Change handlers.  Thus, the only way to both
 *      prevent input on read-only fields and track changes to fields is to
 *      set window.onKeyPress to handleEvent() above.)
 * 2.   In a UNIX environment, many events captured by the window seem to have
 *      their target attributes undefined.  (consequence: you cannot trap
 *      KeyPress events at the window level and handle them, because after
 *      you catch them you cannot determine which target they were bound for.
 *      Thus, the only way to examine events sent to TEXT fields is to set
 *      handlers on the individual elements.)
 * A careful reading of the two bugs above help you to see that no one event
 * handling approach will currently work for both platforms.  Hopefully these
 * bugs will be fixed soon.
 *
 * In the meantime, this eventMethod variable is provided to choose the event
 * handling method.  If you can type in read-only fields, or if changing a
 * data value does not cause dependent values to be recalculated, change the
 * value of this variable to one of the constants below.
 */
var CAPTURE_ELEMENT_EVENTS = 1;
var CAPTURE_WINDOW_EVENTS = 2;
var eventMethod = CAPTURE_ELEMENT_EVENTS;

var pageContainsElements = false;
	 
/*
 * When this routine is established as the event handler for a particular
 * event on a particular object, it will block the event from reaching the
 * object if the object has an "isEditable" property which is set to false.
 */

function NScheckEditable() {
  if (debug) {
    java.lang.System.out.print("NScheckEditable called by ");
    java.lang.System.out.print(this);
    java.lang.System.out.print(", isEditable = ");
    java.lang.System.out.println(this.isEditable);
  }
  
  if ((typeof(this.isEditable) == "object") &&
      (this.isEditable == false))
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
  if (debug) {
    java.lang.System.out.print("NSchangeNotify called by ");
    java.lang.System.out.println(this);
  }
  if (document.applets["NSDataAppl"] != null)
    document.applets["NSDataAppl"].notifyListener(this);
  return true;
}


/*
 * This routine is a combination of checkEditable and changeNotify.  It must
 * be set as a capturing event handler on the window, not as an event handler
 * on individual elements.  It will block events from reaching any object 
 * which has its "isEditable" property set to false.  Otherwise, it will
 * route the event, and when the events effects are completed, it will invoke
 * the notifyListener method on the DataApplet.
 */

function NShandleEvent(e) {
  if (debug) java.lang.System.out.println("handleEvent called by "+e.target);

  if ((typeof(e.target.isEditable) != "undefined") &&
      (e.target.isEditable == false))
    return false;
  else {
    var returnValue = routeEvent(e);
  if (document.applets["NSDataAppl"] != null)
    document.applets["NSDataAppl"].notifyListener(e.target);
    return returnValue;
  }
}


/*
 * Examine a form element during startup, and setup any required event
 * handlers.
 */

function NSregisterElement(elem) {

  pageContainsElements = true;

  if (elem.name.toLowerCase() == "requiredtag")
    requiredTag = elem.value;

  else if (eventMethod == CAPTURE_ELEMENT_EVENTS)
    switch (elem.type.toLowerCase()) {

    case "select-one" :
    case "select-multiple":
      if (debug) document.writeln("Setting "+elem.name+".onChange<BR>");
      elem.onChange = NSchangeNotify;
      break;

    case "text" :
    case "textarea" :
      if (debug) document.writeln("Setting "+elem.name+".onKeyDown<BR>");
      elem.onKeyDown = NScheckEditable;
      if (debug) document.writeln("Setting "+elem.name+".onBlur<BR>");
      elem.onBlur = NSchangeNotify;
      break;

    default:
      // elem is of type HIDDEN, RESET, SUBMIT, FILEUPLOAD, PASSWORD,
      // BUTTON, CHECKBOX, or RADIO.  no event handlers need to be setup
      // for these elements.
    }
}
				// function object for use with elementIterate
function NSregisterElementObj() { this.func = NSregisterElement; }


/*
 * Netscape top-level setup procedure.
 */

function NSSetup() {

  if (debug) document.writeln("<p>Setting up under Netscape, ");

  /*
   * The next lines of code capture and handle events at the window level.
   */
  if (eventMethod == CAPTURE_ELEMENT_EVENTS) {
    if (debug) document.writeln("capturing events on individual elements.");
    window.captureEvents(Event.CLICK);
    window.onClick=NShandleEvent;
  } else if (eventMethod == CAPTURE_WINDOW_EVENTS) {
    if (debug) document.writeln("capturing events at the window level.");
    window.captureEvents(Event.CLICK | Event.KEYPRESS | Event.BLUR);
    window.onClick=NShandleEvent;
    window.onKeyPress = NScheckEditable;
    window.onBlur = NSchangeNotify;
  }

  elementIterate(new NSregisterElementObj());

  if (pageContainsElements == true) {
    if (debug) document.writeln("<p>creating applet.");
    document.writeln('<applet name=NSDataAppl'+
		            ' codebase="/1/NS" '+
		            ' code=pspdash.data.NSDataApplet'+
		            ' width=1 height=1 MAYSCRIPT>');
    if (requiredTag != "")
      document.writeln('<param name=requiredTag value="' + requiredTag +'">');
    document.writeln('</applet>');
  }
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




/***********************************************************************
 ***                         MAIN PROCEDURE                          ***
 ***********************************************************************/


if (debug) document.writeln("Starting setup process.");


if (MSIEversion() >= 4)
  IEsetup();
else if (NSversion() >= 4 && NSversion() < 6)
  NSSetup();
else {
    document.write("<HR><CENTER>");
    document.write("HTML Data support of the PSP Dashboard requires either ");
    document.write("Microsoft Internet Explorer 4.0 (or later) or ");
    document.write("Netscape Navigator 4.0-4.75.  To use these ");
    document.write("features, please click on one of the icons below to ");
    document.write("download the latest version.<P>");
    document.write('<A HREF="http://www.microsoft.com/ie/logo.asp"><IMG SRC="http://www.microsoft.com/sitebuilder/graphics/ie4get_animated.gif" WIDTH="88" HEIGHT="31" BORDER="0" ALT="Get Microsoft Internet Explorer"></A>');
    document.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
    document.write('<A HREF="http://home.netscape.com/comprod/mirror/index.html"><IMG SRC="http://home.netscape.com/comprod/mirror/images/now_anim_button.gif" ALT="Choose Netscape Now" HEIGHT=31 WIDTH=88 BORDER=0></A>');
}


if (debug) document.writeln("<p>done with data.js.");
