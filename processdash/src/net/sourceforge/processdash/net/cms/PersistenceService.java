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

/** Interface for an object which can load and/or retrieve a stream of
 * bytes representing a named object.
 */
public interface PersistenceService {

    /** A page metadata attribute that holds the persistence qualifier */
    public String QUALIFIER_ATTRIBUTE_NAME = "PersistenceQualifier";

    /** Return an InputStream of bytes representing the named file, or null if
     * no such file can be found by this service.
     */
    public InputStream open(String filename) throws IOException;

    /** Return an OutputStream which can save bytes representing the named
     * file, or null if no such stream can be handled by this service.
     */
    public OutputStream save(String qualifier, String filename) throws IOException;

}
