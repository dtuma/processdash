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

import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XMLUtils {

    private static ResourcePool builderPool;
    private static DocumentBuilderFactory factory = null;

    static {
        try {
            factory = DocumentBuilderFactory.newInstance();
        } catch (FactoryConfigurationError fce) {
            System.err.println("Unable to create a DocumentBuilderFactory: " +
                               fce);
        }

        builderPool = new ResourcePool("XMLUtils.builderPool") {
                protected Object createNewResource() {
                    try {
                        return factory.newDocumentBuilder();
                    } catch (Exception e) {
                        return null;
                    }
                }
            };
    }

    public static Document parse(java.io.InputStream in)
        throws SAXException, IOException
    {
        DocumentBuilder builder = null;
        Document result = null;
        try {
            builder = (DocumentBuilder) builderPool.get();
            result = builder.parse(in);
        } finally {
            if (builder != null) builderPool.release(builder);
        }
        return result;
    }

    public static Document parse(String document)
        throws SAXException, IOException
    {
        return parse(new ByteArrayInputStream(document.getBytes()));
    }

    public static String escapeAttribute(String value) {
        return StringUtils.findAndReplace
            (HTMLUtils.escapeEntities(value), "'", "&apos;");
    }

    public static boolean hasValue(String val) {
        return (val != null && val.length() > 0);
    }

    public static String exceptionMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) return null;

        if (e instanceof SAXParseException) {
            int line = ((SAXParseException) e).getLineNumber();
            int column = ((SAXParseException) e).getColumnNumber();
            if (line != -1 && column != -1)
                message += " (on line " +line+ ", column " +column+")";
            else if (line != -1)
                message += " (on line " +line+ ")";
        }

        return message;
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
