// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class EVExporterXMLv1 implements EVExporter, EVXmlConstantsv1 {

    public void export(OutputStream outStream, Map schedules)
            throws IOException {
        Writer out = new OutputStreamWriter(outStream, ENCODING);

        out.write(XML_HEADER + NEWLINE + NEWLINE);
        out.write("<" + DOCUMENT_ELEM + ">" + NEWLINE);

        for (Iterator iter = schedules.entrySet().iterator(); iter.hasNext();) {
            Map.Entry e = (Entry) iter.next();
            String taskScheduleName = (String) e.getKey();
            EVTaskList tl = (EVTaskList) e.getValue();

            String xml = tl.getAsXML(true);
            xml = StringUtils.findAndReplace(xml, "\n", NEWLINE + INDENT + INDENT);
            out.write(INDENT + "<" + SCHEDULE_ELEM + " " + SCHEDULE_NAME_ATTR
                    + "='");
            out.write(XMLUtils.escapeAttribute(taskScheduleName));
            out.write("'>" + NEWLINE + INDENT + INDENT);
            out.write(xml);
            out.write(NEWLINE + INDENT + "</" + SCHEDULE_ELEM + ">" + NEWLINE);
        }

        out.write("</" + DOCUMENT_ELEM + ">" + NEWLINE);
        out.flush();
    }

}
