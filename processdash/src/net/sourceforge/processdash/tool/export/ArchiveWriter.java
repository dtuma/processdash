// Copyright (C) 2004-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export;


import java.io.IOException;
import java.io.OutputStream;


public interface ArchiveWriter {


    /** Can this writer support anchors in hyperlinks? */
    public boolean supportsAnchors();


    /** Start writing an archive */
    public void startArchive(OutputStream out) throws IOException;


    /** Calculate a replacement for a particular URI.

     * @param uri a "Dashboard URI" which appears as an HREF or SRC
     *     attribute within an HTML file
     * @param contentType the content type of the item pointed to by the
     *     uri
     * @return a replacement value that can be written in the HTML file in
     *     place of the given URI.
     */
    public String mapURI(String uri, String contentType);


    /** Add a file to the archive.
     *
     * @param uri the "Dashboard URI" of the original
     * @param contentType
     * @param content
     */
    public void addFile(String uri, String contentType,
                        byte[] content, int offset)
        throws IOException;


    /** Finish writing the archive.
     */
    public void finishArchive() throws IOException;


}
