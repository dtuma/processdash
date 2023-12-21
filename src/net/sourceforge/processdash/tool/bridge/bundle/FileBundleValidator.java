// Copyright (C) 2023 Tuma Solutions, LLC
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

/**
 * Bundle information is stored in separate XML and ZIP files to optimize
 * bandwidth and performance. Unfortunately, we don't control third-party sync
 * clients, and those aren't guaranteed to copy these files atomically.
 * 
 * For example, if a user has a large pending sync queue, their client might
 * publish some of a bundle's files long before it publishes the others.
 * 
 * This interface is used to test the validity of a given bundle, checking to
 * make sure all of its files are present.
 */
public interface FileBundleValidator {

    /**
     * @return true if all the files for a given bundle are present, false if
     *         some appear to be missing
     */
    public boolean isBundleValid(FileBundleID bundleID);

}
