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

package net.sourceforge.processdash.ui.web.reports;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import pspdash.TinyCGIBase;
import pspdash.data.FormToHTML;


public class FormReport extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String uri = getURI(), prefix = "";
        int slashPos = uri.indexOf("//");
        if (slashPos != -1)
            prefix = URLDecoder.decode(uri.substring(0, slashPos));

        byte [] contents = getTinyWebServer().getRequest(uri, true);
        StringBuffer results = new StringBuffer(new String(contents, "UTF-8"));

        FormToHTML.translate(results, getDataRepository(), prefix);
        out.print(results.toString());
    }

    private String getURI() {
        String referer = (String) env.get("HTTP_REFERER");

        // if the query parameter "URI" is set, return the value.
        String uri = (String) parameters.get("uri");
        if (uri != null) return uri;
        //return (referer == null ? uri : resolveRelativeURI(referer, uri));

        uri = (String) env.get("REQUEST_URI");

        // If no query parameters were sent to this request, use the
        // uri of the referer.
        if (uri.indexOf('?') == -1 && referer != null) try {
            return (new URL(referer)).getFile();
        } catch (MalformedURLException mue) {}

        return null;
    }

}
