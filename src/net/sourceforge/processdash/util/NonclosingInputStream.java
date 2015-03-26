// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A filtering input stream which refuses to close the underlying stream.
 * 
 * Certain libraries are not well-behaved, and choose to close an InputStream
 * that is passed into a function. The XML parsing libraries are one example.
 * When reading through the entries in a ZIP, this becomes a problem, because
 * the library will close the entire ZIP if you attempt to parse one of its
 * entries as XML.
 * 
 * This class wraps another input stream, then ignores calls to the close()
 * method. The end result is that we can guard against unwanted closures by
 * ill-behaved libraries.
 * 
 * @since 2.1
 */
public class NonclosingInputStream extends FilterInputStream {

    public NonclosingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {}

}
