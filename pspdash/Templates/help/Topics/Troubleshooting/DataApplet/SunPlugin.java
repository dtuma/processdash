// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net



package pspdash.data;

import java.applet.Applet;
import java.io.IOException;
import java.net.URL;

public class IEDataApplet extends Applet {

    private static final String PAGE_URL =
        "/help/Topics/Troubleshooting/DataApplet/SunPlugin.htm";

    public IEDataApplet() { }
    public void oldstart() {
        try {
            URL u = new URL(getDocumentBase(), PAGE_URL);
            System.out.println("URL=" + u);
            getAppletContext().showDocument(u, "_top");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
    public void start() {
        try {
            String urlStr = getParameter("docURL");
            if (urlStr == null || urlStr.length() == 0)
                urlStr = getDocumentBase().toString();
            if (urlStr.indexOf('?') == -1)
                urlStr = urlStr + "?ForceJavaPlugIn";
            else
                urlStr = urlStr + "&ForceJavaPlugIn";

            URL url = new URL(urlStr);
            getAppletContext().showDocument(url, "_self");
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

}