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

import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;


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
        StringTokenizer tok = new StringTokenizer(value, "<>&'\"", true);
        StringBuffer result = new StringBuffer();
        String token;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if      ("<".equals(token))  result.append("&lt;");
            else if (">".equals(token))  result.append("&gt;");
            else if ("&".equals(token))  result.append("&amp;");
            else if ("'".equals(token))  result.append("&apos;");
            else if ("\"".equals(token)) result.append("&quot;");
            else                         result.append(token);
        }
        return result.toString();
    }

    public static boolean hasValue(String val) {
        return (val != null && val.length() > 0);
    }

}
