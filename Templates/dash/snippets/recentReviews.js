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

function showOlderReviews(link) {
   // find the enclosing table
   var table = link;
   while (table.tagName != "TABLE") {
      table = table.parentNode;
   }

   // display the last 5 hidden rows
   var rows = table.getElementsByTagName("TR");
   var numShown = 0;
   for (rowNum = rows.length; rowNum-- > 0; ) {
      var row = rows[rowNum];
      if (row.style.display == "none") {
         Element.show(row);
         if (++numShown == 5) { return; }
      }
   }

   // if we finished the loop above without finding enough
   // rows to display, hide the hyperlink.
   link.style.display = "none";
}
