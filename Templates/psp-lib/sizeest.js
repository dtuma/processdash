/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2009-2012 Tuma Solutions, LLC
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

var DashSET = {

  wizard:
  function() {
    window.open('','wizard',
        'width=800,height=600,resizable=yes,scrollbars=yes').focus();
  },

  load:
  function() {
    DashSET.registerSizePerItemEventsForTable('legacyBaseAdd');
    DashSET.registerSizePerItemEventsForTable('newObjects');
    DashSET.registerBasePartsFocusEvents();
    DashSET.initializeDiffSupport();
  },



//----------------------------------------------------------------------
// SIZE PER ITEM LOOKUP LOGIC
//----------------------------------------------------------------------

  itemSizes: {},

  // registration and setup logic
  registerSizePerItemEventsForTable:
  function(table) {
    if (!(table = $(table))) return;
    $A(table.getElementsByTagName('td'))
        .findAll(DashSET.isSizePerItemInputCell)
        .map(DashSET.getFormElemForCell)
        .each(DashSET.registerSizePerItemEventsForElem);
  },

  isSizePerItemInputCell:
  function(cell) {
    return ['typeColumn', 'itemsColumn', 'relSizeColumn'].find(
      function(className) { return Element.hasClassName(cell, className); });
  },

  registerSizePerItemEventsForElem:
  function(formElem) {
    if (formElem) {
      Event.observe(formElem, 'change', DashSET.sizeTypeChange, false);
    }
  },

  // When the user alters one of the size-per-item fields, lookup and
  // recalculate the corresponding size estimate
  sizeTypeChange:
  function(evnt) {
    var row = Event.findElement(evnt, "tr");
    if (!row) return;
    var f = DashSET.getSizePerItemFields(row);
    var sizePerItem = DashSET.itemSizes[f.type.value + "/" + f.relSize.value];
    if (sizePerItem) {
      var num  = DashSET.getField(f.items) || 1;
      var size = sizePerItem * num;
      DashSET.setField(f.estSize, size);
    }
  },

  getSizePerItemFields:
  function(row) {
    return {
      type: DashSET.getRowElem(row, 'typeColumn'),
      items: DashSET.getRowElem(row, 'itemsColumn'),
      relSize: DashSET.getRowElem(row, 'relSizeColumn'),
      estSize: DashSET.getRowElem(row, 'estSizeColumn')
    };
  },



// ----------------------------------------------------------------------
// BASE PARTS GRAPH
// ----------------------------------------------------------------------

  activeBasePartsRow: null,
  baseSlider: null,
  addedSlider: null,
  changingSlider: false,
  disableBasePartsGraph: false,

  registerBasePartsFocusEvents:
  function() {
    if (DashSET.disableBasePartsGraph) return;
    if (navigator.userAgent.indexOf('Opera') > -1) return;

    $A(document.getElementsByTagName('input'))
        .each(DashSET.registerBasePartsFocusEventsForElem);
  },

  registerBasePartsFocusEventsForElem:
  function(formElem) {
    var cell = formElem.parentNode;
    if (DashSET.isBasePartsEstCell(cell)) {
      Event.observe(formElem, 'focus', DashSET.basePartsActivate, false);
      Event.observe(formElem, 'change', DashSET.basePartsEstCellEdited, false);
    } else {
      Event.observe(formElem, 'focus', DashSET.deactivateBasePartsRow, false);
    }
  },

  isBasePartsEstCell:
  function(cell) {
    if (cell.parentNode.className == "baseTotalRow") return false;
    return (['baseColumn', 'delColumn', 'modColumn', 'addColumn']
        .indexOf(cell.className) != -1);
  },

  basePartsActivate:
  function(evnt) {
    var row = Event.findElement(evnt, "tr");
    if (row != DashSET.activeBasePartsRow) {
      DashSET.deactivateBasePartsRow();
      DashSET.activateBasePartsRow(row);
    }
  },

  // Display the base parts graph for a row in the base parts table
  activateBasePartsRow:
  function(row) {
    Element.addClassName(row, "active");
    DashSET.activeBasePartsRow = row;
    if (!row) return;

    var graph = $('basePartsGraph');
    var baseInputField = DashSET.getRowElem(row, 'baseColumn');
    Position.clone(baseInputField, graph,
         { offsetTop : Element.getHeight(baseInputField),
           offsetLeft : -12,
           setWidth : false,
           setHeight : false });
    graph.show();

    DashSET.baseSlider = new Control.Slider(
      [ 'deletedHandle', 'modifiedHandle' ], 'baseTrack', {
        range: $R(0, 100),
        alignX: -8,
        restricted: true,
        spans: ['unmodifiedSpan'],
        startSpan: 'deletedSpan',
        endSpan: 'modifiedSpan',
        onChange: DashSET.updateCellValues
      });
    DashSET.addedSlider = new Control.Slider(
      [ 'addedHandle' ], 'addedTrack', {
        range: $R(0, 100),
        alignX: -8,
        startSpan: 'addedSpan',
        onChange: DashSET.updateCellValues
      });

    DashSET.updateSliderValues();
  },

  // Hide the base parts graph if it is visible
  deactivateBasePartsRow:
  function() {
    Element.removeClassName(DashSET.activeBasePartsRow, "active");
    DashSET.activeBasePartsRow = null;

    if (DashSET.baseSlider) DashSET.baseSlider.dispose();
    if (DashSET.addedSlider) DashSET.addedSlider.dispose();

    $('basePartsGraph').hide();
  },

  // Respond to the fact that the user edited one of the numbers that
  // is being used to draw the base parts graph
  basePartsEstCellEdited:
  function(evnt) {
    var row = Event.findElement(evnt, "tr");
    if (!row) return;

    var s = DashSET.getBaseSizes(row);
    if (s.deleted + s.modified > s.base) {
      var cell = Event.findElement(evnt, "td");
      DashSET.repairBaseSizes(row, s, cell.className);
    }

    DashSET.updateSliderValues();
  },

  // If the user has entered impossible numbers in the active row of the
  // base parts table, alter those numbers to be valid.
  repairBaseSizes:
  function(row, s, which) {
    var f = DashSET.getBaseFields(row);

    if (which == 'baseColumn') {
      var chgTot = s.deleted + s.modified;
      var newDel = Math.round(s.deleted * s.base / chgTot);
      var newMod = s.base - newDel;
      DashSET.setField(f.deleted, newDel);
      DashSET.setField(f.modified, newMod);
    }

    else if (which == 'delColumn') {
      var newMod = s.base - s.deleted;
      if (newMod > 0) {
        DashSET.setField(f.modified, newMod);
      } else {
        DashSET.setField(f.modified, 0);
        DashSET.setField(f.base, s.deleted);
      }
    }

    else if (which == 'modColumn') {
      var newDel = s.base - s.modified;
      if (newDel > 0) {
        DashSET.setField(f.deleted, newDel);
      } else {
        DashSET.setField(f.deleted, 0);
        DashSET.setField(f.base, s.modified);
      }
    }
  },

  // Update the slider handles in the base parts graph so they match
  // the numbers that currently appear in the active row
  updateSliderValues:
  function() {
    var row = DashSET.activeBasePartsRow;
    if (!row) return;

    var s = DashSET.getBaseSizes(row);

    DashSET.changingSlider = true;
    if (s.base > 0) {
      DashSET.baseSlider.setValue(100, 1);
      DashSET.baseSlider.setValue (100 * s.deleted/s.base, 0);
      DashSET.baseSlider.setValue (100 * (1 - s.modified/s.base), 1);
      DashSET.addedSlider.setValue(100 * s.added/s.base);
    } else {
      DashSET.baseSlider.setValue(0, 0);
      DashSET.baseSlider.setValue(100, 1);
      DashSET.addedSlider.setValue(0, 0);
    }
    DashSET.changingSlider = false;
  },

  // Update the numerical values in the current row so they match the
  // slider handles in the base parts graph
  updateCellValues:
  function(values, slider) {
     if (DashSET.changingSlider) return;

     var row = DashSET.activeBasePartsRow;
     if (!row) return;

     var f = DashSET.getBaseFields(row);
     var base = DashSET.getField(f.base);
     if (!base) return;

     if (slider == DashSET.baseSlider) {
       DashSET.setField(f.deleted, Math.round(values[0] * base / 100));
       DashSET.setField(f.modified, Math.round(base - values[1] * base / 100));
     } else if (slider == DashSET.addedSlider) {
       DashSET.setField(f.added, Math.round(values * base / 100));
     }
  },

  // Get the form elements that appear in the "Estimated" cells of
  // a given row of the base parts table
  getBaseFields:
  function(row) {
    return {
      base:     DashSET.getRowElem(row, 'baseColumn'),
      deleted:  DashSET.getRowElem(row, 'delColumn'),
      modified: DashSET.getRowElem(row, 'modColumn'),
      added:    DashSET.getRowElem(row, 'addColumn')
    };
  },

  // Get the numerical values that appear in the "Estimated" cells of
  // a given row of the base parts table
  getBaseSizes:
  function(row) {
    return {
      base:     DashSET.getField(DashSET.getRowElem(row, 'baseColumn')),
      deleted:  DashSET.getField(DashSET.getRowElem(row, 'delColumn')),
      modified: DashSET.getField(DashSET.getRowElem(row, 'modColumn')),
      added:    DashSET.getField(DashSET.getRowElem(row, 'addColumn'))
    };
  },



//----------------------------------------------------------------------
// DRAG-AND-DROP DIFF ANNOTATIONS
//----------------------------------------------------------------------

  // Set up the event handlers and other support necessary to handle
  // drag-and-drop support between the diff report and the SET
  initializeDiffSupport:
  function() {
    // check to see if the actual size fields are read-only.
    DashSET.initializeReadOnly();

    // if the current SET template does not support drag-and-drop diff
    // operations, register events to abort those operations and exit.
    if (DashSET.diffDropUnsupported) {
      document.body.ondragover = DashSET.eventHandler(DashSET.cancelDrop);
      document.body.ondrop = DashSET.eventHandler(DashSET.dropUnsupported);
      return;
    }

    // register interest in data element "paint field" events
    addPaintFieldObserver(DashSET.checkPaintEventForDiffAnnotationData);

    if (DashSET.isReadOnly) {
      var cancelDrop = DashSET.eventHandler(DashSET.cancelDrop);
      document.body.ondragover = cancelDrop;
      document.body.ondrop     = cancelDrop;
    } else {
      document.body.ondragover = DashSET.eventHandler(DashSET.dragOver);
      document.body.ondrop     = DashSET.eventHandler(DashSET.drop);
    }

    DashSET.registerAnnotationFocusEvents();

    // check to see if a previous view of this page sent us instructions.
    DashSET.checkForStoredPageReloadInstructions();
  },

  initializeReadOnly:
  function() {
    var diffSupportMarker = $("Diff_Annotation_Data_Supported");
    DashSET.diffDropUnsupported = !diffSupportMarker;

    var reusedFields = $A($("reusedObjects").getElementsByTagName("input"));
    DashSET.isReadOnly = reusedFields.pluck("name").any(function(name) {
      return name.indexOf("Description]r") != -1; });
  },

  registerAnnotationFocusEvents:
  function() {
    // make a list of cell types that get annotated
    var cellTypes = $H({});
    DashSET.DIFF_ANNOTATION_TYPE_MAP.values().pluck("destColumns")
        .each(function(dc, idx) { dc.each( function(instr, idx) {
             cellTypes[instr[0]] = true; }) });
    DashSET.ANNOTATED_CELL_TYPES = cellTypes.keys();

    // now scan all the fields in the document and add focus handlers
    $A(document.getElementsByTagName('input'))
        .each(DashSET.registerAnnotationFocusEventsForElem);
  },

  registerAnnotationFocusEventsForElem:
  function(formElem) {
    var cell = formElem.parentNode;
    if (DashSET.isDiffAnnotationCell(cell)) {
      Event.observe(formElem, 'focus', DashSET.showOrHideDiffAnnotation, false);
    } else {
      Event.observe(formElem, 'focus', DashSET.hideDiffAnnotation, false);
    }
  },

  // return true if the given TD contains a cell that would potentially
  // be affected by a drag-and-drop operation
  isDiffAnnotationCell:
  function(cell) {
    return DashSET.ANNOTATED_CELL_TYPES.include(cell.className);
  },

  // During a drag operation, add a CSS class to mark the row that would
  // currently be the target of a drop.
  setDropTargetRow:
  function(row) {
    if (row == DashSET.currentDropTargetRow)
      return;
    if (DashSET.currentDropTargetRow)
      Element.removeClassName(DashSET.currentDropTargetRow, "dropTarget");
    if (row)
      Element.addClassName(row, "dropTarget");
    DashSET.currentDropTargetRow = row;
  },

  // respond to the "drag over" delivered by the web browser
  dragOver:
  function(event) {
    DashSET.setDropTargetRow(DashSET.getRowForDiffAnnotationEvent(event));
    Event.stop(event);
  },

  // instruct the web browser to cancel a drop operation
  cancelDrop:
  function(event) {
    Event.stop(event);
    return false;
  },

  // abort a drop operation because the user doesn't have the latest version
  // of the PSP process plugin.
  dropUnsupported:
  function(event) {
    if (DashSET.OLD_PSP_PROCESS_MATERIALS_MESSAGE)
      window.alert(DashSET.OLD_PSP_PROCESS_MATERIALS_MESSAGE);

    Event.stop(event);
    return false;
  },

  // respond to the drop event delivered by the web browser
  drop:
  function(event) {
    DashSET.handleDrop(event);

    DashSET.setDropTargetRow(null);
    Event.stop(event);
    return false;
  },

  // Update data structures in response to data dropped on a row of the SET.
  handleDrop:
  function(event) {
    // hide the annotation bubble if it is currently visible.
    DashSET.hideDiffAnnotation();

    // find the drop-capable row associated with the event.  Abort if the
    // data was not dropped onto an appropriately capable row.
    var row = DashSET.getRowForDiffAnnotationEvent(event);
    var table = DashSET.tableForEvent;
    if (!row || !table) return;

    // get the diff annotation data associated with the dropped content.
    // abort if the dropped content is not recognized.
    var dataTransfer = event.dataTransfer;
    if (!dataTransfer) return;
    var text = dataTransfer.getData("URL");
    var data = DashSET.getDiffTransferData(text);
    if (!data) {
        text = dataTransfer.getData("Text");
        data = DashSET.getDiffTransferData(text);
    }
    if (!data) return;

    // If the user is adding a new row, check to see if an empty row is
    // present in the table that we can use.  If not, reload the page to
    // create an empty row, and add the data there.
    if (DashSET.eventRowIsInsertion) {
      row = DashSET.findFirstEmptyTableRow(table);
      if (!row) {
        DashSET.reloadPageForDiffInsertion(table, data.token);
        return;
      }
    }

    // The table, row, and data are valid.  Proceed with the drop operation.
    DashSET.handleDropOfData(table.id, row, data);
  },

  // Perform the actions necessary to effect the drop of a piece of diff
  // annotation data on a particular row of a particular table.
  handleDropOfData:
  function(tableId, row, data) {
    // if the dropped content corresponds to a bit of annotation data that was
    // already associated with the current row, show the annotation bubble and
    // abort.  Otherwise, if it was previously dropped onto a different row of
    // the table, remove the annotation data from that other row.
    if (!DashSET.findAndDeleteDiffAnnotationData(data, row)) {
      DashSET.showDiffAnnotationForRow(row);
      return;
    }

    // apply the annotated diff information to this row of the SET.
    var typeMap = DashSET.DIFF_ANNOTATION_TYPE_MAP[tableId];
    if (!typeMap) return;
    DashSET.applyDiffAnnotationData(row, data, typeMap, true);

    // show the annotation bubble, reflecting the new changes.
    DashSET.showDiffAnnotationForRow(row);
  },

  // A page refresh is needed to add new rows to the SET.  When the user drops
  // a diff annotation onto an "add more rows" link, This routine reloads the
  // page to create an extra row - but first it stores data that the reloaded
  // page can use to apply the diff annotation.
  reloadPageForDiffInsertion:
  function(table, token) {
    // get the URL that should be used to perform an "add more rows" operation
    // for this table
    var addMoreCell = document.getElementsByClassName("addMoreCell", table)[0];
    if (!addMoreCell) return;
    var url = addMoreCell.getElementsByTagName("a")[0].href;

    // save the scrollbar positions so we can restore them after the reload
    DashSET.createCookie("scrollX", window.pageXOffset
                || document.documentElement.scrollLeft
                || document.body.scrollLeft
                || 0, 1);
    DashSET.createCookie("scrollY", window.pageYOffset
                || document.documentElement.scrollTop
                || document.body.scrollTop
                || 0, 1);

    // record information about the diff annotation data that is to be added
    // after the reload
    DashSET.createCookie("addDiffToTable", table.id, 1);
    DashSET.createCookie("diffToken", encodeURIComponent(token), 1);

    // reload the page to add more rows to this table
    window.location.replace(url);
  },

  // check to see whether a previous incarnation of this page recorded
  // instructions that we are to follow.
  checkForStoredPageReloadInstructions:
  function() {
    // if applicable, reset the scroll bars to their previous positions.
    var scrollX = DashSET.extractCookie("scrollX");
    var scrollY = DashSET.extractCookie("scrollY");
    if (scrollX && scrollY)
      window.scrollTo(parseInt(scrollX), parseInt(scrollY));

    // if a diff annotation was stored, make a record of it so we can insert
    // it after the data on the page has been loaded.
    var tableId = DashSET.extractCookie("addDiffToTable");
    var token = decodeURIComponent(DashSET.extractCookie("diffToken") || "");
    if (tableId && token) {
      DashSET.storedDiffInsertionInfo = [tableId, token];
      addDataLoadedObserver(DashSET.handleDiffInsertionAfterPageReload);
    }
  },

  // this method is called after all of the data on the page has been loaded,
  // to apply an inserted diff annotation that was stored immediately before
  // the page reload.
  handleDiffInsertionAfterPageReload:
  function() {
    if (!DashSET.storedDiffInsertionInfo) return;
    var tableId = DashSET.storedDiffInsertionInfo[0];
    var token   = DashSET.storedDiffInsertionInfo[1];
    DashSET.storedDiffInsertionInfo = null;

    var row = DashSET.findFirstEmptyTableRow($(tableId));
    var data = DashSET.getDiffTransferData(token);
    if (row && data)
      DashSET.handleDropOfData(tableId, row, data);
  },

  // Alter the data in a particular row of the table, to either add or
  // remove the effect of a specific set of diff annotation data.
  applyDiffAnnotationData:
  function(row, data, typeMap, add) {
    // extract numbers from the annotation data, and add those values to the
    // appropriate fields in the table row, as instructed by the typeMap.
    var incr = (add ? 1 : -1);
    var foundNums = false;
    for (var i = 0; i < typeMap.destColumns.length;  i++) {
      var colInfo = typeMap.destColumns[i];
      var field = DashSET.getRowElem(row, colInfo[0]);
      for (var j = 1; j < colInfo.length;  j++) {
        DashSET.incrementCell(field, data[colInfo[j]] * incr);
      }
      if (field.value != "0") foundNums = true;
    }

    // retrieve the hidden field used for diff annotations
    var diffData = document.getElementsByClassName(
        "diffAnnotationData", row)[0];
    var diffDataVal = diffData.value;

    // potentially alter the description field on the row based on an
    // annotation that is being added.
    var descr = DashSET.getRowElem(row, "descColumn");
    if (descr && add) {
      if (!descr.value && !diffDataVal) {
        // if we are adding annotation data, the description field is 
        // currently blank, and no other diff data is present on the
        // row, use the annotated "name" as the description
        descr.value = data.name;
      } else if (descr.value ==
                 DashSET.getSingleDiffAnnotationName(diffDataVal)) {
        // if this row contains exactly one diff annotation whose name
        // matches the description field (a sign that we probably filled
        // it in ourselves), and we are now adding a second set of diff
        // annotation data, clear out the (now-obsolete) description field.
        descr.value = "";
      }
    }

    // record the annotation token into a hidden field on the row.
    if (add) {
      if (!diffDataVal) diffDataVal = ",";
      diffDataVal = diffDataVal + data.token + ",";
    } else {
      diffDataVal = diffDataVal.replace(data.token + ",", "");
      if (diffDataVal == ",") diffDataVal = "";
    }
    diffData.value = diffDataVal;

    // potentially alter the description field on the row based on an
    // annotation that is being removed.
    if (descr && !add) {
      // if we are removing annotation data, and the description field
      // appears to match the annotated "name" (a sign that we probably
      // filled it in ourselves), remove that automatic description.
      if (descr.value == data.name) {
        descr.value = "";
      }
      // after removing this diff annotation, if only one diff token remains
      // on this row and the row has no description, use that token's name
      // as the description.
      var oneName = DashSET.getSingleDiffAnnotationName(diffDataVal);
      if (oneName && !descr.value) {
        descr.value = oneName;
      }
    }

    // check and see whether any diff annotations are in effect for this row.
    if (diffDataVal) {
      // make a note that this row is annotated, so a marker can be shown.
      Element.addClassName(row, "diffDataAnnotated");
    } else {
      // make a note that this row is no longer annotated, so the marker can
      // be removed
      Element.removeClassName(row, "diffDataAnnotated");

      // if all of the cells in this row are now zeros, remove them
      if (foundNums == false) {
        for (var i = 0; i < typeMap.destColumns.length;  i++) {
          var colInfo = typeMap.destColumns[i];
          var field = DashSET.getRowElem(row, colInfo[0]);
          field.value = "";
        }
      }
    }

    // fire changeNotifyElem events so the dashboard's data.js persistence
    // logic can save the changes we just made.
    if (descr) changeNotifyElem(descr);
    for (var i = 0; i < typeMap.destColumns.length;  i++) {
      var colInfo = typeMap.destColumns[i];
      var field = DashSET.getRowElem(row, colInfo[0]);
      if (field) changeNotifyElem(field);
    }
    changeNotifyElem(diffData);
  },

  // examine the diff data associated with a particular row.  If that data
  // contains exactly one annotation, return the name from that annotation.
  // otherwise return null.
  getSingleDiffAnnotationName:
  function(diffDataVal) {
    if (!diffDataVal) return null;
    var token = diffDataVal.slice(1, -1);
    if (token.indexOf(",") != -1) return null;
    var data = DashSET.getDiffTransferData(token);
    if (!data) return null;
    return data.name;
  },

  // If a particular set of annotation data has previously been applied to
  // a row on this SET, find that row.  If that row happens to be "unlessRow",
  // do nothing and return false.  Otherwise subtract the data back out and
  // return true.
  findAndDeleteDiffAnnotationData:
  function(data, unlessRow) {
    var row = DashSET.findRowWithToken(data.token);
    if (!row) return true;
    if (row == unlessRow) return false;

    var table = Event.findElement({target:row}, "table");
    var typeMap = DashSET.DIFF_ANNOTATION_TYPE_MAP[table.id];
    DashSET.applyDiffAnnotationData(row, data, typeMap, false);
    return true;
  },

  // If a particular set of annotation data has previously been applied to
  // a row on this SET, find that row and return it.
  findRowWithToken:
  function(token) {
    var search = "," + token + ",";
    var fields = document.getElementsByClassName("diffAnnotationData");
    for (var i = 0;  i < fields.length;  i++) {
      if (fields[i].value.indexOf(search) != -1)
        return Event.findElement({target:fields[i]}, "tr");
    }
    return null; 
  },

  // read the numeric value in a particular <input> field, and increment
  // that value by a certain amount.
  incrementCell:
  function(cell, num) {
    var cellNum = parseInt(cell.value);
    if (!cellNum) cellNum = 0;
    var newNum = cellNum + num;
    if (newNum < 0) newNum = 0;
    cell.value = newNum.toString();
  },

  // Examine a javascript event and see if is associated with an element in
  // one of the <tables> that supports diff annotations.  If so, return the
  // table row that contained the element in question.  In the process, set
  // the "DashSET.tableForEvent" and "DashSET.eventRowIsInsertion" fields.
  getRowForDiffAnnotationEvent:
  function(event) {
    if (!event) return null;
    var row = Event.findElement(event, "tr");
    if (!row) return null;
    if (!row.tagName || row.tagName.toLowerCase() != "tr") return null;
    var table = Event.findElement(event, "table");
    if (!table) return null;
    if (!DashSET.DIFF_ANNOTATION_TYPE_MAP[table.id]) return null;

    DashSET.tableForEvent = table;

    var allTableRows = table.getElementsByTagName("tr");
    DashSET.eventRowIsInsertion =
        (row == allTableRows[0] ||
         row == allTableRows[1] ||
         row == allTableRows[allTableRows.length-1]);

    return row;
  },

  // Look through a SET table, and find the first row that contains no data.
  // Returns null if no empty row was found.
  findFirstEmptyTableRow:
  function(table) {
    if (!DashSET.isTag(table, "table")) return null;
    var tableRows = $A(table.getElementsByTagName("tr"));
    if (tableRows.length < 4) return null;
    return tableRows.slice(2, -1).find(DashSET.tableRowIsEmpty);
  },

  // return true if a particular row of a SET table contains no text fields
  // containing data.
  tableRowIsEmpty:
  function(row) {
    var inputFields = $A(row.getElementsByTagName("input"));
    return inputFields.all(DashSET.inputElemIsNontextOrEmpty);
  },

  // return true if a particular input element is empty, or if it is not a
  // text field.
  inputElemIsNontextOrEmpty:
  function(field) {
    return (field.type.toLowerCase() != "text"
            || !field.value || field.value == "0");
  },

  // find the diff-annotation-capable table associated with a particular
  // event, and show or hide the annotation bubble depending on whether
  // that row contains annotations.
  showOrHideDiffAnnotation:
  function(event) {
    var row = DashSET.getRowForDiffAnnotationEvent(event);
    DashSET.showOrHideDiffAnnotationForRow(row);
  },

  // show or hide the annotation bubble depending on whether a particular row
  // contains annotations.  If the annotation bubble was currently visible
  // for that row, recalculate its contents.
  reshowOrHideDiffAnnotationForRow:
  function(row) {
    DashSET.diffAnnotationRow = null;
    DashSET.showOrHideDiffAnnotationForRow(row);
  },

  // show or hide the annotation bubble depending on whether a particular row
  // contains annotations.
  showOrHideDiffAnnotationForRow:
  function(row) {
    if (row && Element.hasClassName(row, "diffDataAnnotated"))
      DashSET.showDiffAnnotationForRow(row);
    else
      DashSET.hideDiffAnnotation();
  },

  // show the annotation bubble for a particular row that is known to
  // contain annotations.
  showDiffAnnotationForRow:
  function(row) {
    if (row == DashSET.diffAnnotationRow) return;

    var annotationBox = $("diffAnnotation");
    annotationBox.innerHTML = DashSET.buildDiffAnnotationHtml(row);

    Position.clone(row, annotationBox,
        { offsetLeft: row.offsetWidth + 10,
          setWidth : false,
          setHeight : false });
    annotationBox.show();
    DashSET.diffAnnotationRow = row;
  },

  // construct a set of HTML to display in the diff annotation bubble.
  buildDiffAnnotationHtml:
  function(row) {
    var html = "<div id=\"diffAnnotationPtr\"> </div><table>";

    var diffData = document.getElementsByClassName(
        "diffAnnotationData", row)[0];
    if (diffData.value) {
      html = html + ($A(diffData.value.split(","))
                     .map(DashSET.getOneDiffAnnotationHtml).join(""));
    }

    html = html + "</table>";
    return html;
  },

  // get the HTML for a single row within the diff annotation bubble.
  getOneDiffAnnotationHtml:
  function(token) {
    var data = DashSET.getDiffTransferData(token);
    if (!data) return "";

    var html = "<tr><td>";

    if (!DashSET.isReadOnly) {
      html = html + "<a href=\"#\" class=\"annDel\" "
        + "onclick=\"return DashSET.annDel(this);\">&nbsp;</a>"
        + "<a onclick=\"return false;\" href=\""
        + token.escapeHTML() +"\">";
    }

    html = html + data.name.escapeHTML();

    if (!DashSET.isReadOnly) html = html + "</a>";

    html = html + "</td></tr>";
    return html;
  },

  // when the dashboard's data.js class has just repainted a form field,
  // examine the paint event and see if we need to alter our annotations.
  checkPaintEventForDiffAnnotationData:
  function(elem, value, readOnly) {
    // check to see if this is a paint event for a diff annotation field.
    if (elem.name.match(/Diff_Annotation_Data/)) {
      var row = Event.findElement({target:elem}, "tr");
      if (!row) return;
      // update the CSS class to reflect whether this row has annotations.
      if (value) {
        Element.addClassName(row, "diffDataAnnotated");
      } else {
        Element.removeClassName(row, "diffDataAnnotated");
      }
      // if the annotation bubble is currently being displayed for this row,
      // update or hide it (as appropriate)
      if (row == DashSET.diffAnnotationRow)
        DashSET.reshowOrHideDiffAnnotationForRow(row);
    }
  },

  // hide the diff annotation bubble if it is visible anywhere on the page.
  hideDiffAnnotation:
  function() {
    $("diffAnnotation").hide();
    DashSET.diffAnnotationRow = null;
  },

  // this function is called when a user clicks the "delete" icon on a single
  // element within the annotation bubble.
  annDel:
  function(link) {
    // find the annotation data associated with the delete icon they clicked
    var token = link.parentNode.getElementsByTagName("a")[1].href;
    var data = DashSET.getDiffTransferData(token);
    if (!data) return false;

    // remove that annotation data from the SET, and recalculate the
    // annotation bubble.
    DashSET.findAndDeleteDiffAnnotationData(data);
    DashSET.reshowOrHideDiffAnnotationForRow(DashSET.diffAnnotationRow);
    return false;
  },

  // look at a URL or other source of annotation data, and extract the
  // embedded size accounting data.  If no data could be extracted,
  // returns null.
  getDiffTransferData:
  function(text) {
    if (!text) return null;
    var match = DashSET.DIFF_ANNOTATION_TRANSFER_PAT.exec(text);
    if (!match) return null;
    return {
      token:    match[0],
      base:     parseInt(match[1]),
      deleted:  parseInt(match[2]),
      modified: parseInt(match[3]),
      added:    parseInt(match[4]),
      total:    parseInt(match[5]),
      name:     decodeURIComponent(match[6].replace(/_/g, '%'))
    };
  },

  // The regular expression describing the format of annotation tokens.
  DIFF_ANNOTATION_TRANSFER_PAT:
      /#diffFile:B(\d+):D(\d+):M(\d+):A(\d+):T(\d+):(.*)/,

  // Information about which cells in a row should receive annotation numbers,
  // based on the table that contains that row
  DIFF_ANNOTATION_TYPE_MAP: $H({
      legacyBaseAdd: {
        destColumns: [
          ["actSizeColumn", "added", "modified" ]
        ]
      },
      baseParts: {
        destColumns: [
          ["actBaseColumn", "base" ],
          ["actDelColumn", "deleted" ],
          ["actModColumn", "modified" ],
          ["actAddColumn", "added" ]
        ]
      },
      newObjects: {
        destColumns: [
          ["actSizeColumn", "added", "modified" ]
        ]
      },
      reusedObjects: {
        destColumns: [
          ["actSizeColumn", "total" ]
        ]
      }
    }),



//----------------------------------------------------------------------
// UTILITY METHODS
//----------------------------------------------------------------------

  useCommaForDecimal: false,

  eventHandler:
  function(func) {
    return func.bindAsEventListener(DashSET);
  },

  setField:
  function(destElem, num) {
      num = Math.round(num * 10) / 10;
      num = num.toString();
      if (DashSET.useCommaForDecimal) {
        num = num.toString().replace('.', ',');
      }

      destElem.value = num;
      changeNotifyElem(destElem);
  },

  getField:
  function(elem) {
    if (!elem) return 0;
    num = String(elem.value);
    if (!num) return 0;
    if (DashSET.useCommaForDecimal) num = num.replace(',', '.');
    return Number(num);
  },

  getRowElem:
  function(row, cellClassName) {
    return (document.getElementsByClassName(cellClassName, row)
      .map(DashSET.getFormElemForCell))[0];
  },

  getFormElemForCell:
  function(cell) {
    return $A(cell.childNodes).find(function(elem) {
      return ['INPUT', 'SELECT'].include(elem.tagName.toUpperCase());
    });
  },

  isTag:
  function(elem, tagName) {
    return (elem && elem.tagName
            && elem.tagName.toLowerCase() == tagName.toLowerCase());
  },

  createCookie:
  function(name, value, minutes) {
    var date = new Date();
    date.setTime(date.getTime() + (minutes * 60 * 1000));
    var expires = "; expires=" + date.toUTCString();
    document.cookie = "DashSET_" + name + "=" + value + expires + "; path=/";
  },

  readCookie:
  function(name) {
    var nameEQ = "DashSET_" + name + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i < ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) == ' ') c = c.substring(1, c.length);
      if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
  },

  eraseCookie:
  function(name) {
    DashSET.createCookie(name, "", -1000);
  },

  extractCookie:
  function(name) {
    var result = DashSET.readCookie(name);
    DashSET.eraseCookie(name);
    return result;
  },

  _ignoredField: true

};

Event.observe(window, 'load', DashSET.load, false);
