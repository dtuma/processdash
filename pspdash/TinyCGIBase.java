// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

public class TinyCGIBase implements TinyCGI {

    protected InputStream inStream = null;
    protected OutputStream outStream = null;
    protected PrintWriter out = null;
    protected Map env = null;
    protected Map parameters = new HashMap();

    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        this.inStream = in;
        this.outStream = out;
        this.out = new PrintWriter(out);
        this.env = env;
        parseInput();
        writeHeader();
        writeContents();
        this.out.flush();
    }

    /** Parse CGI query parameters, and store them in the Map
     *  <code>parameters</code>.
     *
     * Multivalued parameters are currently not supported.
     * In the future, if form handling is required, this method might
     * be extended to parse POSTed form data.
     */
    protected void parseInput() {
        parameters.clear();

        String query = (String) env.get("QUERY_STRING");
        if (query == null || query.length() == 0)
            return;

        StringTokenizer params = new StringTokenizer(query, "&");
        String param;
        int equalsPos;
        while (params.hasMoreTokens()) {
            param = params.nextToken();
            equalsPos = param.indexOf('=');
            if (equalsPos == -1)
                parameters.put(URLDecoder.decode(param), Boolean.TRUE);
            else
                parameters.put
                    (URLDecoder.decode(param.substring(0, equalsPos)),
                     URLDecoder.decode(param.substring(equalsPos+1)));
        }
    }


    /** Write a standard CGI header.
     *
     * This method can be overridden by children that might need to generate
     * a special header, or might need to vary the header on the fly.
     */
    protected void writeHeader() {
        out.print("Content-type: text/html\r\n\r\n");
        // flush in case writeContents wants to use outStream instead of out.
        out.flush();
    }


    /** Generate CGI script output.
     *
     * This method should be overridden by child classes to generate
     * the contents of the script.
     */
    protected void writeContents() throws IOException {
        out.println("<HTML><BODY>");
        out.println("This space intentionally left blank.");
        out.println("</BODY></HTML>");
    }
}
