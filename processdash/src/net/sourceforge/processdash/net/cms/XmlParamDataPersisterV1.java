// Copyright (C) 2006 Tuma Solutions, LLC
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
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class XmlParamDataPersisterV1 implements ParamDataPersister {

    public static final String IDENTIFIER = "xml.v1";

    public String getIdentifier() {
        return IDENTIFIER;
    }

    private static final String TOP_TAG = "parameters";

    private static final String ENUM_TAG = "enum";

    private static final String ITEM_TAG = "item";

    private static final String PARAM_TAG = "param";

    private static final String NAME_ATTR = "name";

    public String getQueryString(String persistedText) {
        StringBuffer result = new StringBuffer();
        try {
            getQueryStringImpl(result, persistedText);
        } catch (Exception e) {
        }

        return result.toString();
    }

    private void getQueryStringImpl(StringBuffer result, String persistedText)
            throws SAXException, IOException {
        Element e = XMLUtils.parse(persistedText).getDocumentElement();
        getQueryStringImpl(result, e, "");
    }

    private void getQueryStringImpl(StringBuffer result, Element e,
            String namePrefix) {
        List children = XMLUtils.getChildElements(e);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Element child = (Element) i.next();
            String tag = child.getTagName();
            if (ENUM_TAG.equals(tag))
                appendEnum(result, child, namePrefix);
            else if (PARAM_TAG.equals(tag))
                appendParam(result, child, namePrefix);
        }
    }

    private void appendEnum(StringBuffer result, Element child,
            String namePrefix) {
        String enumName = child.getAttribute(NAME_ATTR);
        List items = XMLUtils.getChildElements(child);
        for (int i = 0; i < items.size(); i++) {
            String itemSpace = namePrefix + enumName + i + "_";
            result.append('&').append(namePrefix).append(enumName).append(
                    "Enum=").append(String.valueOf(i));
            getQueryStringImpl(result, (Element) items.get(i), itemSpace);
        }
    }

    private void appendParam(StringBuffer result, Element child,
            String namePrefix) {
        String prefix = HTMLUtils.urlEncode(namePrefix);
        String name = HTMLUtils.urlEncode(child.getAttribute(NAME_ATTR));
        String val = HTMLUtils.urlEncode(XMLUtils.getTextContents(child));
        result.append('&').append(prefix).append(name).append('=').append(val);
    }



    public String getTextToPersist(Map postedData) {
        try {
            return getTextToPersistImpl(postedData);
        } catch (IOException ioe) {
            return null;
        }
    }

    private String getTextToPersistImpl(Map postedData) throws IOException {
        XmlSerializer ser = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            ser = factory.newSerializer();
        } catch (XmlPullParserException xppe) {
            throw new RuntimeException("Couldn't obtain xml serializer", xppe);
        }
        try {
            ser.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output",
                    true);
        } catch (Exception e) {
        }

        StringWriter out = new StringWriter();
        ser.setOutput(out);
        ser.startDocument("utf-16", Boolean.TRUE);
        ser.flush();
        out.getBuffer().setLength(0);

        ser.startTag(null, TOP_TAG);

        // filter the posted data so we are only working with items that end
        // in "_ALL".  This simplifies our work.
        postedData = EditedPageDataParser.filterParamMap(postedData,
                new TreeMap(), null, "_ALL", false, true);

        Map enumerations = EditedPageDataParser.filterParamMap(postedData,
                new TreeMap(), null, "Enum", true, true);
        for (Iterator i = enumerations.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String[] enumIDs = (String[]) e.getValue();
            if (enumIDs == null || enumIDs.length == 0)
                continue;

            String enumName = (String) e.getKey();
            ser.startTag(null, ENUM_TAG);
            ser.attribute(null, NAME_ATTR, enumName);

            for (int j = 0; j < enumIDs.length; j++) {
                ser.startTag(null, ITEM_TAG);

                String id = enumIDs[j];
                String itemSpace = enumName + id + "_";
                Map itemAttrs = EditedPageDataParser.filterParamMap(postedData,
                        new TreeMap(), itemSpace, null, true, true);
                writeParamList(ser, itemAttrs);

                ser.endTag(null, ITEM_TAG);
            }


            ser.endTag(null, ENUM_TAG);
        }

        writeParamList(ser, postedData);

        ser.endTag(null, TOP_TAG);
        ser.endDocument();

        return out.toString();
    }

    private void writeParamList(XmlSerializer ser, Map params)
            throws IOException {
        for (Iterator k = params.entrySet().iterator(); k.hasNext();) {
            Map.Entry attr = (Map.Entry) k.next();
            String attrName = (String) attr.getKey();
            String[] values = (String[]) attr.getValue();
            for (int v = 0; v < values.length; v++) {
                if (values[v] != null
                        && (values[v].length() > 0 || values.length > 1)) {
                    ser.startTag(null, PARAM_TAG);
                    ser.attribute(null, NAME_ATTR, attrName);
                    ser.text(values[v]);
                    ser.endTag(null, PARAM_TAG);
                }
            }
        }
    }

}
