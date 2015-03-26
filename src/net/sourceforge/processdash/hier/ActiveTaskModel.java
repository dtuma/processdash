// Copyright (C) 2005-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier;

import java.beans.PropertyChangeListener;


public interface ActiveTaskModel {

    /** Returns the node representing the currently active task.
     */
    public PropertyKey getNode();

    /** Set the node representing the currently active task.
     *
     * @return true if the change was successful.  (The change could fail
     * if the specified node does not exist in the dashboard hierarchy.)
     */
    public boolean setNode(PropertyKey node);


    /** Returns the path name of the currently active task.
     */
    public String getPath();

    /** Set the currently active task to the node named by the given path.
     *
     * @return true if the change was successful.  (The change could fail
     * if the named node does not exist in the dashboard hierarchy.)
     */
    public boolean setPath(String path);


    /** Attempt to change to a named phase of the currently active "project".
     * 
     * @param phase the name of a phase to select
     * @return true if the change was successful.  (The change could fail
     * if the specified phase does not exist in the current project.)
     */
    public boolean setPhase(String phase);


    /** Add a listener for changes in the currently active task.
     */
    public void addPropertyChangeListener(PropertyChangeListener l);

    /** Remove a listener for changes in the currently active task.
     */
    public void removePropertyChangeListener(PropertyChangeListener l);

}
