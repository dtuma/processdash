// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class XMLUtils {

    private static DocumentBuilderFactory factory = null;
    private static DocumentBuilder builder = null;

    static {
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (FactoryConfigurationError fce) {
            System.err.println("Unable to create a DocumentBuilderFactory: " +
                               fce);
        } catch (ParserConfigurationException e) {
            System.err.println("Unable to create a DocumentBuilder: " + e);
        }
    }

    public static Document parse(java.io.InputStream in)
        throws SAXException, IOException
    {
        Document result = null;
        synchronized (builder) {
            result = builder.parse(in);
        }
        return result;
    }

    public static Document parse(String document)
        throws SAXException, IOException
    {
        return parse(new ByteArrayInputStream(document.getBytes()));
    }

    public static String escapeAttribute(String value) {
        if (value == null)
            return "";

        StringBuffer result = new StringBuffer(value.length());
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case '<': result.append("&lt;"); break;
                case '>': result.append("&gt;"); break;
                case '&': result.append("&amp;"); break;
                case '"': result.append("&quot;"); break;
                case '\'': result.append("&apos;"); break;
                default:
                    if (chars[i] < 32)
                        result.append("&#").append((int) chars[i]).append(";");
                    else
                        result.append(chars[i]);
            }
        }
        return result.toString();
    }

    public static boolean hasValue(String val) {
        return (val != null && val.length() > 0);
    }

    public static String saveDate(Date d) { return "@" + d.getTime(); }
    private static Date parseDate(String d) throws IllegalArgumentException {
        if (!d.startsWith("@")) throw new IllegalArgumentException();
        return new Date(Long.parseLong(d.substring(1)));
    }
    public static double getXMLNum(Element e, String attrName) {
        try {
            return Double.parseDouble(e.getAttribute(attrName));
        } catch (Exception exc) { return 0; }
    }
    public static int getXMLInt(Element e, String attrName) {
        try {
            return Integer.parseInt(e.getAttribute(attrName));
        } catch (Exception exc) { return -1; }
    }
    public static Date getXMLDate(Element e, String attrName) {
        String s = e.getAttribute(attrName);
        if (s == null || s.length() == 0) return null;
        try {
            return parseDate(s);
        } catch (Exception exc) { return null; }
    }
    public static String getTextContents(Element e) {
        if (!e.hasChildNodes()) return null;

        StringBuffer buf = new StringBuffer();
        NodeList list = e.getChildNodes();
        int len = list.getLength();
        String text;
        for (int i=0;   i<len;   i++) {
            text = ((Node) list.item(i)).getNodeValue();
            if (text != null) buf.append(text);
        }
        return buf.toString();
    }

    // this isn't working for me with GNU JAXP...they appear to have
    // compiled in a default TransformerFactory that doesn't exist.
    // How stupid is that?
    public static String getAsText(Node n) {
        try {
            Transformer t =
                TransformerFactory.newInstance().newTransformer();
            StringWriter out = new StringWriter();
            t.transform(new javax.xml.transform.dom.DOMSource(n),
                        new javax.xml.transform.stream.StreamResult(out));
            return out.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
