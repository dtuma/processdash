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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import net.sourceforge.processdash.i18n.Resources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



public class XMLUtils {

    private static ResourcePool builderPool;
    private static DocumentBuilderFactory factory = null;
    private static ResourcePool saxParserPool;
    private static SAXParserFactory saxFactory = null;

    static {
        try {
            factory = DocumentBuilderFactory.newInstance();
        } catch (FactoryConfigurationError fce) {
            System.err.println("Unable to create a DocumentBuilderFactory: " +
                               fce);
        }

        builderPool = new ResourcePool("XMLUtils.builderPool") {
                protected Object createNewResource() {
                    return createNewDocumentBuilder();
                }
            };

        try {
            saxFactory = SAXParserFactory.newInstance();
        } catch (FactoryConfigurationError fce) {
            System.err.println("Unable to create a SAXParserFactory: " +
                               fce);
        }

        saxParserPool = new ResourcePool("XMLUtils.builderPool") {
                protected Object createNewResource() {
                    return createNewSAXParser();
                }
            };
    }

    private static Object createNewDocumentBuilder() {
        try {
            synchronized (factory) {
                return factory.newDocumentBuilder();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Object createNewSAXParser() {
        try {
            synchronized (saxFactory) {
                return saxFactory.newSAXParser();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /// methods for DOM parsing

    public static Document parse(java.io.InputStream in)
        throws SAXException, IOException
    {
        return parse(new InputSource(in));
    }

    public static Document parse(java.io.Reader in)
        throws SAXException, IOException
    {
        return parse(new InputSource(in));
    }

    public static Document parse(String document)
        throws SAXException, IOException
    {
        return parse(new StringReader(document));
    }

    public static Document parse(InputSource inputSource)
        throws SAXException, IOException
    {
        DocumentBuilder builder = null;
        Document result = null;
        try {
            builder = (DocumentBuilder) builderPool.get();
            result = builder.parse(inputSource);
        } finally {
            if (builder != null) builderPool.release(builder);
        }
        return result;
    }

    /// methods for SAX parsing

    public static void parse(java.io.Reader in,
            org.xml.sax.helpers.DefaultHandler handler) throws SAXException,
            IOException {
        parse(new InputSource(in), handler);
    }

    public static void parse(java.io.InputStream in,
            org.xml.sax.helpers.DefaultHandler handler) throws SAXException,
            IOException {
        parse(new InputSource(in), handler);
    }

    private static void parse(InputSource inputSource,
            org.xml.sax.helpers.DefaultHandler handler) throws SAXException,
            IOException {
        SAXParser parser = null;
        try {
            parser = (SAXParser) saxParserPool.get();
            parser.parse(inputSource, handler);
        } finally {
            if (parser != null) saxParserPool.release(parser);
        }
    }

    public static StringMapper ESCAPE_ATTRIBUTE = new StringMapper() {
        public String getString(String str) {
            return escapeAttribute(str);
        }
    };

    public static String escapeAttribute(String value) {
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
    public static Date parseDate(String d) throws IllegalArgumentException {
        if (d == null || d.length() == 0) return null;
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

    /** Return a list of the elements that are direct children of the given
     * node.
     */
    public static List getChildElements(Node node) {
        List result = new LinkedList();
        NodeList childNodes = node.getChildNodes();
        for (int i= 0;  i < childNodes.getLength();  i++) {
                Node oneChild = childNodes.item(i);
                if (oneChild instanceof Element)
                        result.add(oneChild);
        }
        return result;
    }
}
