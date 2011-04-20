// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.util.XMLUtils;

public class StudataExporterXml {

    static final SimpleDateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");

    private static final String ENCODING = "UTF-8";

    private static final String OD_NAMESPACE_ATTR =
            "urn:schemas-microsoft-com:officedata";

    public static void writeXmlData(OutputStream out, DataContext profileData,
            List<String> projectPaths, ResultSet data) throws IOException {
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, ENCODING);
        xml.startDocument(ENCODING, null);

        xml.startTag(null, "dataroot");
        xml.attribute(null, "xmlns:od", OD_NAMESPACE_ATTR);
        xml.attribute(null, "generated", DATE_FMT.format(new Date()));

        StudataExporterXmlProfile profileExporter = new StudataExporterXmlProfile(
                xml, profileData);
        profileExporter.writeXmlProfileData();

        StudataExporterXmlPrograms programExporter = new StudataExporterXmlPrograms(
                xml, projectPaths, data);
        programExporter.writeProgramData();

        xml.endTag(null, "dataroot");
        xml.endDocument();
    }

    static String formatDouble(double d) {
        // cleanup bad double values
        if (Double.isInfinite(d) || Double.isNaN(d))
            d = 0;

        // format using Java formatting (not localized formatting)
        String result = Double.toString(d);

        // cleanup the display of integer values
        if (result.endsWith(".0"))
            result = result.substring(0, result.length()-2);

        // only retain 3 digits after the decimal point
        int dotPos = result.indexOf('.');
        if (dotPos > 0 && dotPos < result.length() - 4)
            result = result.substring(0, dotPos + 4);

        return result;
    }
}
