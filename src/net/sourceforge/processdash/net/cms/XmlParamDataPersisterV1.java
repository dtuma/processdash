// Copyright (C) 2006-2010 Tuma Solutions, LLC
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class XmlParamDataPersisterV1 implements ParamDataPersister {

    private boolean lenientPostedDataMode;

    private boolean generateWhitespace;

    public XmlParamDataPersisterV1() {
        this(false, true);
    }

    public XmlParamDataPersisterV1(boolean lenientPostedDataMode,
            boolean whitespace) {
        this.lenientPostedDataMode = lenientPostedDataMode;
        this.generateWhitespace = whitespace;
    }

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
            if (generateWhitespace)
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
        if (lenientPostedDataMode)
            addMissingALLParams(postedData);
        postedData = EditedPageDataParser.filterParamMap(postedData,
                new TreeMap(), null, "_ALL", false, true);

        writeParamList(ser, postedData);

        ser.endTag(null, TOP_TAG);
        ser.endDocument();

        return out.toString();
    }

    /**
     * When query parameters are parsed by TinyCGIBase, we can assume that
     * each parameter will have a matching "_ALL" array (even if it only
     * contains one element).  But parameters that come from another source
     * will not follow that pattern.  On request, this method will fill in
     * the missing "_ALL" parameters for the posted data.
     */
    private void addMissingALLParams(Map postedData) {
        Set<String> plainParamNames = new HashSet<String>();
        Set<String> allParamNames = new HashSet<String>();
        for (Iterator i = postedData.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (name.endsWith("_ALL"))
                allParamNames.add(name.substring(0, name.length()-4));
            else
                plainParamNames.add(name);
        }
        plainParamNames.removeAll(allParamNames);
        for (String name : plainParamNames) {
            Object value = postedData.get(name);
            if (value instanceof String) {
                String str = (String) value;
                String[] allValue = new String[] { str };
                postedData.put(name + "_ALL", allValue);
            }
        }
    }

    private void writeParamList(XmlSerializer ser, Map postedData)
            throws IOException {
        // first, find all the enumerations in the data.  Sort them
        // lexographically so top-level enum names will appear before
        // embedded enum names.
        Set enumerationNames = EditedPageDataParser.filterParamMap(postedData,
            new TreeMap(), null, "Enum", false, true).keySet();

        // extract the enumerations from the map and write the associated
        // list of items
        for (Iterator i = enumerationNames.iterator(); i.hasNext();) {
            String enumName = (String) i.next();
            String enumKey = enumName + "Enum";
            String[] enumIDs = (String[]) postedData.remove(enumKey);
            if (enumIDs == null || enumIDs.length == 0)
                continue;

            ser.startTag(null, ENUM_TAG);
            ser.attribute(null, NAME_ATTR, enumName);

            for (int j = 0; j < enumIDs.length; j++) {
                ser.startTag(null, ITEM_TAG);

                // extract the data elements associated with each item in the
                // enumeration.  Note that if an enumerated item contains an
                // second-level embedded enumeration, this will remove it from
                // the posted data map, and the outer "for" loop will skip over
                // it at some point in the future.
                String id = enumIDs[j];
                String itemSpace = enumName + id + "_";
                Map itemAttrs = EditedPageDataParser.filterParamMap(postedData,
                        new TreeMap(), itemSpace, null, true, true);
                // now recursively call writeParamList to write the data for
                // this item, which may include parameters and other embedded
                // enumerations
                writeParamList(ser, itemAttrs);

                ser.endTag(null, ITEM_TAG);
            }

            ser.endTag(null, ENUM_TAG);
        }

        writePlainParamList(ser, postedData);
    }

    /**
     * Write a list of parameters that are known NOT to include enumerations.
     */
    private void writePlainParamList(XmlSerializer ser, Map params)
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
