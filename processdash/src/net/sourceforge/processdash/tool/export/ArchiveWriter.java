// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export;


import java.io.IOException;
import java.io.OutputStream;


public interface ArchiveWriter {


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
