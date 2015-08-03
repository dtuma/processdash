/****************************************************************************
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2015 Tuma Solutions, LLC
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

var DashScanner = {

   clearItem:
   function(checkbox) {
      var tr = checkbox.parentNode.parentNode;
      tr.className = (checkbox.checked ? "scanItemCleared" : "");
      return true;
   },

   clearAllItems:
   function(checkbox) {
      var div = checkbox.parentNode.parentNode;
      var checkboxes = div.getElementsByTagName("INPUT");
      for (var i=1; i < checkboxes.length; i++) {
         checkboxes[i].checked = checkbox.checked;
         DashScanner.clearItem(checkboxes[i]);
         changeNotifyElem(checkboxes[i]);
      }
   }

};
