// Copyright (C) 2017 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package teamdash.sync;

import java.util.Map;

/**
 * A change that should be made to an external system to bring it into sync with
 * the WBS.
 */
public class ExtChange {

    /**
     * The node in the external system which this change applies to.
     */
    public ExtNode extNode;

    /**
     * A list of attribute values that should be changed in the external system.
     */
    public Map<String, Object> attrValues;

    /**
     * A collection of metadata values that should be recorded after the change
     * has been successfully applied to the external system.
     */
    public SyncMetadata metadata;


    public static final String EST_TIME_ATTR = "estHours";

    public static final String ACT_TIME_ATTR = "actHours";

}
