// -*- tab-width: 2 -*- <!--#server-parsed--><!--#resources bundle="CMS" -->
// <!--#echo defaultEncoding="javaStr" -->
/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Tuma Solutions, LLC
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

var DashCMS = {

  cancelEdit:
  function() {
    var message = "<!--#echo Edit_Page.Cancel_Prompt -->";
    if (window.confirm(message))
      history.back(1);
  },

  addSnippet:
  function() {
    var namespace = "addSnip" + (++this.namespaceNum) + "_";
    var snippetDiv = document.createElement("div");
    snippetDiv.id = namespace;
    $('snippetContainer').appendChild(snippetDiv);
    var url = window.location.pathname + "?mode=addNew&ns=" + namespace +
      this._addNewExtraArgs.replace(/NS_/, namespace);
    new Ajax.Updater(snippetDiv, url, { evalScripts:true,
        onComplete: Element.scrollTo.bind(Element, snippetDiv) });
  },

  _addNewExtraArgs: "",

  setAddNewExtraArgs:
	function(args) {
    this._addNewExtraArgs = args;
  },

  _snipDivIdPattern: /^(snip|addSnip)[0-9]+_/,

  _snipDiv:
  function(elem) {
    while (elem != null && !this._snipDivIdPattern.test(elem.id))
      elem = elem.parentNode;
    return elem;
  },

  deleteSnippet:
  function(elem) {
    var snipDiv = this._snipDiv(elem);
    if (snipDiv == null) return;

    var namespace = snipDiv.id;
    var formData = Form.serialize(snipDiv);
    if (formData.indexOf("snippetDiscarded" + namespace + "=t") != -1) {
      snipDiv.parentNode.removeChild(snipDiv);
      return;
    }

    var deleteMsg = snipDiv.getElementsByTagName("DIV")[1];
    var editRegion = snipDiv.getElementsByTagName("DIV")[2];
    this._setDiscardFields(snipDiv, "t");
    Element.show(deleteMsg);
    Element.hide(editRegion);

  },

  undeleteSnippet:
  function(elem) {
    var snipDiv = this._snipDiv(elem);
    if (snipDiv == null) return;

    var deleteMsg = snipDiv.getElementsByTagName("DIV")[1];
    var editRegion = snipDiv.getElementsByTagName("DIV")[2];
    this._setDiscardFields(snipDiv, "");
    Element.hide(deleteMsg);
    Element.show(editRegion);
  },

  _setDiscardFields:
  function(snipDiv,value) {
    var deleteFields = $A(snipDiv.getElementsByTagName("INPUT")).findAll(
      function(field) { return /^snippetDiscarded/.test(field.name); } );
    deleteFields.each( function(field) { field.value = value; } );
  },

  selectSnippet:
  function(elem,snipID) {
    var snipDiv = this._snipDiv(elem);
    if (snipDiv == null) return;

    var snipID = elem.id.replace(/^[^_]+_/, "");
    var url = window.location.pathname + "?mode=addNew&ns=" + snipDiv.id +
      "&snippetID=" + snipID;
    new Ajax.Updater(snipDiv, url, { evalScripts:true });
  },

  showAddItemDescr:
  function(elem) {
    var li = Event.findElement({target:elem}, "li");
    var td = Event.findElement({target:li}, "td");
    var lis = $A(td.getElementsByTagName("li"));
    lis.each(function(item, i) { item.className = ""; });

    li.className = "cmsNewItemDescription";
    var descr = $(elem.id.replace(/^link/, "descr"));
    Element.show(descr);
    var descrs = $A(descr.parentNode.getElementsByTagName("div"));
    descrs.each(function(e, i) { if (e != descr) Element.hide(e); } );

    li = td = lis = descr = descrs = null;
  },

  hideAddItemDescr:
  function(elem) {
    elem.parentNode.className = "";
    var descrId = elem.id.replace(/^link/, "descr");
    Element.hide(descrId);
  },

  _fixupSortable:
  function() {
    if (this.sortable != null) { this.sortable.destroy(); }
    this.sortable = Sortable.create('snippetContainer',
        { handle:'cmsEditingTitle', tag:'div' });
  },

  _moveDivUp:
  function(snipDiv) {
    if (snipDiv == null) return;
    var prevSnip = snipDiv.previousSibling;
    if (prevSnip == null) return;
    var parentDiv = snipDiv.parentNode;
    parentDiv.removeChild(snipDiv);
    parentDiv.insertBefore(snipDiv, prevSnip);
  },

  moveSnippetUp:
  function(elem) {
    var snipDiv = this._snipDiv(elem);
    if (snipDiv == null) return;
    this._moveDivUp(snipDiv);
    Element.scrollTo(snipDiv);
  },

  moveSnippetDown:
  function(elem) {
    var snipDiv = this._snipDiv(elem);
    if (snipDiv == null) return;
    this._moveDivUp(snipDiv.nextSibling);
    Element.scrollTo(snipDiv);
  },

  requireScript:
  function(src) {
    window.alert("WARNING - not yet implemented. Cannot add script " + src);
  },

  requireStyleSheet:
  function(href) {
    window.alert("WARNING - not yet implemented. Cannot add stylesheet "+href);
  },

  initPage:
  function() {
    this._fixupSortable();
  },

  afterItemAdded:
  function() {
    this._fixupSortable();
  },

  _autocompleteMatch:
  function(text, tokens) {
    for (var i=0;  i < tokens.length;   i++)
      if (text.indexOf(tokens[i]) == -1) return false;
    return true;
  },

  autocompletionSelector:
  function(instance) {
    var ret = [];
    var idNum = this.autocompleteNum++;
    var tokens = instance.getToken().toLowerCase().split(" ");
    for (var i = 0;   i < instance.options.array.length;  i++) {
      var elem = instance.options.array[i];
      if (this._autocompleteMatch(elem.toLowerCase(), tokens)) {
	var elemId = "";
	if (instance.options.internalValues)
	  elemId = " id='cmsac" + idNum + "_" + 
	    instance.options.internalValues[i] + "'";
	ret.push("<li" + elemId + ">" + elem + "</li>");
      }
    }
    if (ret.length > 10) {
      Element.addClassName(instance.update, "cmsAutocompleteOverflow");
      instance.update.scrollTop = 0;
    } else {
      Element.removeClassName(instance.update, "cmsAutocompleteOverflow");
    }
    return "<ul>" + ret.join('') + "</ul>";
  },

  autocompletionPost:
  function(userFunctionName, inputField, selectedItem) {
    var guiID = inputField.id;
    var fieldID = guiID.substring(4);
    var val = inputField.value;

    if (selectedItem.id)
      val = selectedItem.id.replace(/^[^_]+_/, "");

    $(fieldID).value = val;

    if (userFunctionName != null) try {
      var userFunc = eval(userFunctionName);
      if (userFunc != null) 
			  userFunc.apply(DashCMS, [$(fieldID), inputField]);
    } catch (e) {}
  },

  addAutocompleteToList:
  function(hiddenField, displayField) {
    var id = hiddenField.id.substring(4);

    this._addItemToList(id, [ /_VALUE_/, hiddenField.value,
        /_DISPLAY_/, displayField.value ]);

    hiddenField.value = "";
    displayField.value = "";
    displayField.blur();
  },

  addItemToList:
  function(id) {
    this._addItemToList(id, null);
  },

  _addItemToList:
  function(id, uriReplacements) {
    var uriId = id + "_uri";
    var uri = $(uriId).value;
    if (uriReplacements != null) {
        for (var i=0;  i < uriReplacements.length;  i=i+2) {
            uri = uri.replace(uriReplacements[i], 
                encodeURIComponent(uriReplacements[i+1]));
        }
    }
    uri = uri.replace(/_ITEM_/, "add" + DashCMS.namespaceNum++, "g");
    
    var baseUri = window.location.pathname;
    var slashPos = baseUri.indexOf("//");
    if (slashPos == -1) slashPos = baseUri.indexOf("/+/");
    if (slashPos != -1)
      uri = baseUri.substring(0, slashPos) + "//" + uri;
    else
      uri = "/" + uri;

    var containerId = id + "_container";
    new Ajax.Updater(containerId, uri, { insertion:Insertion.Bottom,
      method:'get',
      onComplete:DashCMS.fixupSortableList.bind(DashCMS, containerId) });
  },

  fixupSortableList:
  function(containerId, ajax) {
    var oldSortable = this.subSortables[containerId];
    if (oldSortable != null) oldSortable.destroy();
    var newSortable = Sortable.create(containerId, {tag:'div'});
    this.subSortables[containerId] = newSortable;
    this._fixupSortable();
  },

  sortable: null,
  subSortables: {},

  namespaceNum: 0,
  autocompleteNum: 0

};
