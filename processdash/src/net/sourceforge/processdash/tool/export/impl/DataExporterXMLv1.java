// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.util.HashTree;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.XmlNumberFormatter;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class DataExporterXMLv1 implements DataExporter, DataXmlConstantsv1 {

    private static final String DATA_XML_HEADER = "<?xml version='1.1' encoding='"
            + ENCODING + "' standalone='yes' ?>";

    private NumberFormat numberFormat = new XmlNumberFormatter();

    public void export(OutputStream out, Iterator dataElements)
            throws IOException {
        HashTree sorted = sortDataElements(dataElements);
        writeDataElements(new OutputStreamWriter(out, ENCODING), sorted);
    }

    private HashTree sortDataElements(Iterator dataElements) {
        HashTree result = new HashTree(TreeMap.class);
        while (dataElements.hasNext()) {
            ExportedDataValue v = (ExportedDataValue) dataElements.next();
            String name = v.getName();
            SimpleData simpleValue = v.getSimpleValue();
            if (simpleValue != null)
                result.put(name, simpleValue);
        }
        return result;
    }

    private void writeDataElements(Writer out, HashTree sorted)
            throws IOException {
        XmlSerializer xml = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            xml = factory.newSerializer();
        } catch (XmlPullParserException xppe) {
            throw new RuntimeException("Couldn't obtain xml serializer", xppe);
        }

        // we need to write our header manually, because we need to specify
        // XML version 1.1
        out.write(DATA_XML_HEADER + NEWLINE + NEWLINE);

        xml.setOutput(out);
        xml.startTag(null, DATA_ELEM);
        xml.ignorableWhitespace(NEWLINE);

        writeDataElementsForNode(xml, sorted, 0);

        xml.endTag(null, DATA_ELEM);
        xml.ignorableWhitespace(NEWLINE);
        xml.endDocument();

        out.flush();
    }

    private void writeDataElementsForNode(XmlSerializer xml, HashTree data,
            int depth) throws IOException {
        // write all the tags for this node first.
        for (Iterator iter = data.getContents(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String dataName = (String) e.getKey();
            SimpleData dataValue = (SimpleData) e.getValue();
            if (dataValue instanceof TagData)
                writeDataElement(xml, dataName, dataValue, depth);
        }

        // now, write the rest of the data elements.
        for (Iterator iter = data.getContents(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String dataName = (String) e.getKey();
            SimpleData dataValue = (SimpleData) e.getValue();
            if (!(dataValue instanceof TagData))
                writeDataElement(xml, dataName, dataValue, depth);
        }

        // finally, write all the children.
        for (Iterator iter = data.getChildren(); iter.hasNext();) {
            Map.Entry e = (Map.Entry) iter.next();
            String childName = (String) e.getKey();
            HashTree child = (HashTree) e.getValue();
            writeChildElement(xml, childName, child, depth);
        }

    }

    private void writeDataElement(XmlSerializer xml, String name,
            SimpleData value, int depth) throws IOException {
        String elemName = null;
        String text = null;

        if (value instanceof TagData) {
            elemName = TAG_ELEM;

        } else if (value instanceof DateData) {
            elemName = DATE_ELEM;
            Date d = ((DateData) value).getValue();
            if (d != null)
                text = XMLUtils.saveDate(d);

        } else if (value instanceof NumberData) {
            elemName = NUMBER_ELEM;
            text = numberFormat.format(((NumberData) value).getDouble());

        } else {
            elemName = STRING_ELEM;
            text = value.toString();
        }

        if (elemName != null) {
            indent(xml, depth);
            xml.startTag(null, elemName);
            xml.attribute(null, NAME_ATTR, name);
            if (text != null)
                xml.text(text);
            xml.endTag(null, elemName);
            xml.ignorableWhitespace(NEWLINE);
        }
    }

    private void writeChildElement(XmlSerializer xml, String childName,
            HashTree child, int depth) throws IOException {
        childName = childName.substring(0, childName.length() - 1);

        indent(xml, depth);
        xml.startTag(null, NODE_ELEM);
        xml.attribute(null, NAME_ATTR, childName);
        xml.ignorableWhitespace(NEWLINE);

        writeDataElementsForNode(xml, child, depth + 1);

        indent(xml, depth);
        xml.endTag(null, NODE_ELEM);
        xml.ignorableWhitespace(NEWLINE);
    }

    private void indent(XmlSerializer xml, int depth) throws IOException {
        while (depth-- >= 0)
            xml.ignorableWhitespace(INDENT);
    }

}
