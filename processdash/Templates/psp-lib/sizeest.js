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

  itemSizes: {},

  useCommaForDecimal: false,

  wizard:
  function() {
    window.open('','wizard', 
        'width=800,height=600,resizable=yes,scrollbars=yes').focus();
  },

  sizeTypeChange:
  function(evnt) {
    var row = Event.findElement(evnt, "tr");
    var type = DashSET._getTypeElem(row).value;
    var rel  = DashSET._getRelSizeElem(row).value;
    var sizePerItem = DashSET.itemSizes[type + "/" + rel];
    if (sizePerItem) {
      var num  = Number(DashSET._getItemsElem(row).value || 1);
      var size = sizePerItem * num;
      size = Math.round(size * 10) / 10;
      size = size.toString();
      if (DashSET.useCommaForDecimal) {
        size = size.toString().replace('.', ',');
      }
      var destElem = DashSET._getEstSizeElem(row);
      destElem.value = size;
      changeNotifyElem(destElem);
    }
  },

  load:
  function() {
    DashSET._registerSizeEventsForTable('legacyBaseAdd');
    DashSET._registerSizeEventsForTable('newObjects');
  },

  _registerSizeEventsForTable:
  function(table) {
    if (!(table = $(table))) return;
    $A(table.getElementsByTagName('td'))
        .findAll(DashSET._isSizeInputCell)
        .map(DashSET._getFormElemForCell)
        .each(DashSET._registerSizeEventsForElem);
  },

  _isSizeInputCell:
  function(cell) {
    return ['typeColumn', 'itemsColumn', 'relSizeColumn'].find(
      function(className) { return Element.hasClassName(cell, className); });
  },

  _registerSizeEventsForElem:
  function(formElem) {
    if (formElem) {
      Event.observe(formElem, 'change', DashSET.sizeTypeChange, false);
    }
  },

  _getTypeElem: 
  function(row) { return DashSET._getRowElem(row, 'typeColumn'); },

  _getItemsElem: 
  function(row) { return DashSET._getRowElem(row, 'itemsColumn'); },

  _getRelSizeElem:
  function(row) { return DashSET._getRowElem(row, 'relSizeColumn'); },

  _getEstSizeElem:
  function(row) { return DashSET._getRowElem(row, 'estSizeColumn'); },

  _getRowElem:
  function(row, cellClassName) {
    return (document.getElementsByClassName(cellClassName, row)
      .map(DashSET._getFormElemForCell))[0];
  },

  _getFormElemForCell:
  function(cell) {
    return $A(cell.childNodes).find(function(elem) {
      return ['INPUT', 'SELECT'].include(elem.tagName.toUpperCase());
    });
  }

};

Event.observe(window, 'load', DashSET.load, false);
