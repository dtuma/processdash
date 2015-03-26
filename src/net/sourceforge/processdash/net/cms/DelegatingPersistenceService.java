// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Implementation of a persistence service which delegates to several other
 * persistence services, until if finds one that can handle a given request.
 */
public class DelegatingPersistenceService implements PersistenceService {

    private List delegates;

    public DelegatingPersistenceService() {
        this.delegates = new ArrayList();
    }

    public DelegatingPersistenceService add(PersistenceService s) {
        delegates.add(s);
        return this;
    }

    public InputStream open(String filename) throws IOException {
        for (Iterator i = delegates.iterator(); i.hasNext();) {
            PersistenceService s = (PersistenceService) i.next();
            InputStream result = s.open(filename);
            if (result != null)
                return result;
        }

        return null;
    }

    public OutputStream save(String qualifier, String filename)
            throws IOException {
        for (Iterator i = delegates.iterator(); i.hasNext();) {
            PersistenceService s = (PersistenceService) i.next();
            OutputStream result = s.save(qualifier, filename);
            if (result != null)
                return result;
        }

        return null;
    }

}
