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

package net.sourceforge.processdash.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import net.sourceforge.processdash.i18n.Resources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



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

    public static StringMapper ESCAPE_ATTRIBUTE = new StringMapper() {
        public String getString(String str) {
            return escapeAttribute(str);
        }
    };

    public static String escapeAttribute(String value) {
        return StringUtils.findAndReplace
            (HTMLUtils.escapeEntities(value), "'", "&apos;");
    }

    public static boolean hasValue(String val) {
        return (val != null && val.length() > 0);
    }

    public static String exceptionMessage(Exception e) {
        String message = e.getLocalizedMessage();
        if (message == null) message = e.getMessage();
        if (message == null) return null;

        int line = -1, col = -1;
        if (e instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) e;
            line = spe.getLineNumber();
            col = spe.getColumnNumber();
        }
        if (line == -1)
            // no line number information.  Just return the message.
            return message;
        else {
            // format a message containing line#/col# information.
            Resources r = Resources.getDashBundle("Templates");
            String fmtKey = "XML_Exception_Line_FMT";
            if (col != -1)
                fmtKey = "XML_Exception_Line_Column_FMT";
            return r.format
                (fmtKey, message, new Integer(line), new Integer(col));
        }
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
