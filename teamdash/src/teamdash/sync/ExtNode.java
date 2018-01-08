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

/**
 * A generic representation of a node in an external system, that should be
 * synchronized with the WBS.
 */
public interface ExtNode {

    /**
     * @return the unique ID assigned to this node by the external system
     */
    public String getID();

    /**
     * @return the name of this node in the external system
     */
    public String getName();

    /**
     * @return the URL to display a web-based view of this node in the external
     *         system, or null if the external system does not provide such a
     *         page
     */
    public String getUrl();

    /**
     * @return the estimated hours for this node in the external system
     */
    public Double getEstimatedHours();

    /**
     * @return the actual hours for this node in the external system
     */
    public Double getActualHours();

}
