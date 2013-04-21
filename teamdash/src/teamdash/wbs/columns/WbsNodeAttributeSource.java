// Copyright (C) 2013 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs.columns;

import java.util.List;

import teamdash.wbs.WBSNode;

/**
 * Interface for a column which contributes arbitrary plain-text attribute
 * values to WBS nodes, which should be included in the XML dump file for the
 * WBS model.
 * 
 * In current usage, these typically represent values managed by custom WBS
 * columns.
 */
public interface WbsNodeAttributeSource {

    public String getAttributeId();

    public String getAttributeName();

    public boolean isAttributeAutoInherited();

    public List<String> getAttributeValues(WBSNode node);

}
