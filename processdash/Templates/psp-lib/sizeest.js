// -*- tab-width: 2 -*-
/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2009 Tuma Solutions, LLC
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
// UTILITY METHODS
//----------------------------------------------------------------------

  useCommaForDecimal: false,

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

  _ignoredField: true

};

Event.observe(window, 'load', DashSET.load, false);
