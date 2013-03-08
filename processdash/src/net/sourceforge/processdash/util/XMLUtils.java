// Copyright (C) 2001-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sourceforge.processdash.i18n.Resources;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;



public class XMLUtils {

    private static ResourcePool builderPool;
    private static DocumentBuilderFactory factory = null;
    private static ResourcePool saxParserPool;
    private static SAXParserFactory saxFactory = null;
    private static XmlPullParserFactory xmlPullParserFactory = null;

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

        try {
            xmlPullParserFactory = XmlPullParserFactory.newInstance();
        } catch (Exception e) {
            System.err.println("Unable to create an XmlPullParserFactory: " +
                    e);
        }
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
            if (builder != null) {
                builder.reset();
                builderPool.release(builder);
            }
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

    /// methods for XML Pull parsing / serialization

    public static XmlSerializer getXmlSerializer(boolean whitespace)
            throws IOException {
        XmlSerializer result;
        try {
            result = xmlPullParserFactory.newSerializer();
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't obtain xml serializer");
            ioe.initCause(e);
            throw ioe;
        }

        if (whitespace)
            try {
                result.setFeature(
                        "http://xmlpull.org/v1/doc/features.html#indent-output",
                        true);
            } catch (Exception e) {
                // pretty whitespace output is a preference, but rarely a
                // requirement.  If the current XmlPull implementation doesn't
                // support it, just ignore the limitation and move on.
            }

        return result;
    }

    public static StringMapper ESCAPE_ATTRIBUTE = new StringMapper() {
        public String getString(String str) {
            return escapeAttribute(str);
        }
    };

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
    public static String getAttribute(Element e, String attrName, String def) {
        String result = e.getAttribute(attrName);
        if (!hasValue(result))
            result = def;
        return result;
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
    public static List<Element> getChildElements(Node node) {
        NodeList childNodes = node.getChildNodes();
        List result = new ArrayList(childNodes.getLength());
        for (int i= 0;  i < childNodes.getLength();  i++) {
            Node oneChild = childNodes.item(i);
            if (oneChild instanceof Element)
                result.add(oneChild);
        }
        return result;
    }

    /** Return the attributes from the specified element as a Map */
    public static Map<String, String> getAttributesAsMap(Element e) {
        Map<String, String> result = new HashMap<String, String>();
        NamedNodeMap attrs = e.getAttributes();
        if (attrs != null) {
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                Node n = attrs.item(i);
                if (n instanceof Attr) {
                    Attr a = (Attr) n;
                    result.put(a.getName(), a.getValue());
                }
            }
        }
        return result;
    }

    private static ThreadLocal<XPath> XPATH_POOL = new ThreadLocal<XPath>() {
        @Override
        protected XPath initialValue() {
            return XPathFactory.newInstance().newXPath();
        }
    };

    /**
     * @return an XPath object that can be used by the current thread
     * @since 1.14.1
     */
    public static XPath xPath() {
        return XPATH_POOL.get();
    }

    /**
     * @return the string obtained from evaluating an XPath expression
     * @since 1.14.1
     */
    public static String xPathStr(String expr, Object context) {
        try {
            return xPath().evaluate(expr, context);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(expr, e);
        }
    }

    /**
     * @return a list of nodes obtained from evaluating an XPath expression
     * @since 1.14.1
     */
    public static List<Node> xPathNodes(String expr, Object context) {
        return xPathObjects(expr, context);
    }

    /**
     * @return a list of elements obtained from evaluating an XPath expression
     * @since 1.14.1
     */
    public static List<Element> xPathElems(String expr, Object context) {
        return xPathObjects(expr, context);
    }

    static <T> List<T> xPathObjects(String expr, Object context) {
        try {
            NodeList nodes = (NodeList) xPath().evaluate(expr, context,
                XPathConstants.NODESET);
            List<T> result = new ArrayList<T>(nodes.getLength());
            for (int i = 0;  i < nodes.getLength();  i++)
                result.add((T) nodes.item(i));
            return result;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * @return find a list of elements obtained from evaluating an XPath
     *         expression, and return the text contents of those elements.
     * @since 1.14.1
     */
    public static List<String> xPathElemsText(String expr, Object context) {
        List<Element> elems = XMLUtils.xPathElems(expr, context);
        List<String> result = new ArrayList<String>(elems.size());
        for (Element elem : elems)
            result.add(XMLUtils.getTextContents(elem));
        return result;
    }

}
