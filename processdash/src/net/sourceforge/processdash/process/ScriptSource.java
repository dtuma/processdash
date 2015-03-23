// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process;

import java.util.List;

/**
 * Interface implemented by components which would like to dynamically add items
 * to the script menu.
 * 
 * @author Tuma
 */
public interface ScriptSource {

    /**
     * Return an ID for this script source.  Script sources with the same
     * ID will be considered identical, and only one will be consulted during
     * script enumeration operations.
     * 
     * @return a unique ID for this script source
     */
    public String getUniqueID();

    /**
     * Calculate a list of scripts that would be appropriate to display when a
     * certain task is active.
     * 
     * @param path
     *            the full path of the active task
     * @return a list of scripts that should be displayed on behalf of the
     *         active task and its ancestors.
     */
    public List<ScriptID> getScripts(String path);

}
