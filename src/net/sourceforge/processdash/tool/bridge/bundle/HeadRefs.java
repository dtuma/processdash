// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface HeadRefs {

    /**
     * Get the ID of the HEAD ref for the bundle with a given name.
     */
    public FileBundleID getHeadRef(String bundleName) throws IOException;


    /**
     * Get a Map whose keys are the names of bundles tracked by this file, and
     * whose values are the HEAD refs for each.
     */
    public Map<String, FileBundleID> getHeadRefs() throws IOException;


    /**
     * Store a new HEAD ref for a given bundle.
     */
    public void storeHeadRef(FileBundleID bundleID) throws IOException;


    /**
     * Store a set of new HEAD refs for a list of bundles.
     */
    public void storeHeadRefs(Collection<FileBundleID> headRefs)
            throws IOException;

}
