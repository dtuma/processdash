// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.data.applet;

import net.sourceforge.processdash.data.repository.Repository;

public interface HTMLFieldManager {

    /**
     * Setup all internal data structures and establish connections to data.
     * @param data is the repository to get data from, or null if the applet
     *        was unable to establish a connection.
     * @param dataPath is the prefix of the data being accessed.  If data was
     *        null, dataPath will contain an applicable error message instead.
     **/

    public void initialize(Repository data, String dataPath);

    public void registerElement(String elementID, Object elementName, Object elementType);

    public String getDataNotification();


    /**
     * Notify the data repository that the user has edited a value.
     * @param id a unique ID assigned to the HTML input element
     **/

    public void notifyListener(Object elementID, Object elementValue);


    /**
     * Shutdown and cleanup.
     * if @param repositoryExists is true, the field manager must also
     *        remove all data connections.
     **/

    public void dispose(boolean repositoryExists);
}
