// Copyright (C) 2006-2009 Tuma Solutions, LLC
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/** Implementation of a
 * {@link net.sourceforge.processdash.net.cms.ContentSerializer} that uses
 * a simple XML format.
 */
public class XmlSnippetContentSerializer implements ContentSerializer {

    public PageContentTO parse(InputStream content) throws IOException {
        PageContentTO result = new PageContentTO();

        Document doc;
        try {
            doc = XMLUtils.parse(content);
        } catch (SAXException e) {
            IOException ioe = new IOException("Could not read content");
            ioe.initCause(e);
            throw ioe;
        }

        Element e = (Element) doc.getElementsByTagName(PAGE_TITLE_TAG).item(0);
        result.setPageTitle(XMLUtils.getTextContents(e));

        NodeList metadataTag = doc.getElementsByTagName(METADATA_TAG);
        if (metadataTag != null && metadataTag.getLength() > 0) {
            NodeList metadata = ((Element) metadataTag.item(0))
                    .getElementsByTagName(PARAM_TAG);
            for (int i = 0; i < metadata.getLength();  i++) {
                e = (Element) metadata.item(i);
                String name = e.getAttribute(NAME_ATTR);
                String value = XMLUtils.getTextContents(e);
                result.setMetadataValue(name, value);
            }
        }

        NodeList snippetDescriptions = doc.getElementsByTagName(SNIPPET_TAG);
        List contentSnippets = new ArrayList(snippetDescriptions.getLength());

        for (int i = 0; i < snippetDescriptions.getLength(); i++) {
            e = (Element) snippetDescriptions.item(i);
            SnippetInstanceTO snip = new SnippetInstanceTO();
            snip.setSnippetID(e.getAttribute(TYPE_ATTR));
            snip.setSnippetVersion(e.getAttribute(VERSION_ATTR));
            snip.setInstanceID(e.getAttribute(INSTANCE_ID_ATTR));
            snip.setPersistedText(XMLUtils.getTextContents(e));
            snip.setPersisterID(e.getAttribute(PERSISTER_ATTR));
            snip.setPageRegion(getPageRegion(e));
            contentSnippets.add(snip);
        }
        result.setSnippets(contentSnippets);

        return result;
    }

    private int getPageRegion(Node node) {
        while (node != null) {
            node = node.getParentNode();
            if (node instanceof Element) {
                Element elem = (Element) node;
                if (PAGE_HEADING_TAG.equals(elem.getTagName()))
                    return PageContentTO.REGION_HEADER;
                if (PAGE_FOOTER_TAG.equals(elem.getTagName()))
                    return PageContentTO.REGION_FOOTER;
            }
        }
        return PageContentTO.REGION_CONTENT;
    }

    public void format(PageContentTO page, OutputStream out) throws IOException {
        XmlSerializer ser = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            ser = factory.newSerializer();
        } catch (XmlPullParserException xppe) {
            throw new RuntimeException("Couldn't obtain xml serializer", xppe);
        }

        ser.setOutput(out, ENCODING);
        ser.startDocument(ENCODING, null);
        ser.ignorableWhitespace(NEWLINE);
        ser.startTag(null, DOC_ROOT_ELEM);
        ser.ignorableWhitespace(NEWLINE + NEWLINE);

        writePageMetadata(page, ser);

        Iterator headerSnippets = page.getHeaderSnippets();
        writeWrappedSnippets(ser, PAGE_HEADING_TAG, headerSnippets);

        Iterator contentSnippets = page.getContentSnippets();
        writeSnippets(ser, contentSnippets);

        Iterator footerSnippets = page.getFooterSnippets();
        writeWrappedSnippets(ser, PAGE_FOOTER_TAG, footerSnippets);

        ser.endTag(null, DOC_ROOT_ELEM);
        ser.ignorableWhitespace(NEWLINE);
        ser.endDocument();

        out.close();
    }

    private void writePageMetadata(PageContentTO page, XmlSerializer ser)
            throws IOException {
        ser.startTag(null, PAGE_TITLE_TAG);
        if (page.getPageTitle() != null)
            ser.text(page.getPageTitle());
        ser.endTag(null, PAGE_TITLE_TAG);
        ser.ignorableWhitespace(NEWLINE + NEWLINE);

        if (!page.getMetadata().isEmpty()) {
            ser.startTag(null, METADATA_TAG);
            ser.ignorableWhitespace(NEWLINE);
            for (Map.Entry<String, String> e : page.getMetadata().entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                if (name != null && value != null) {
                    ser.ignorableWhitespace("  ");
                    ser.startTag(null, PARAM_TAG);
                    ser.attribute(null, NAME_ATTR, name);
                    ser.text(value);
                    ser.endTag(null, PARAM_TAG);
                    ser.ignorableWhitespace(NEWLINE);
                }
            }
            ser.endTag(null, METADATA_TAG);
            ser.ignorableWhitespace(NEWLINE + NEWLINE);
        }
    }

    private void writeWrappedSnippets(XmlSerializer ser, String wrappingTag,
            Iterator snippets) throws IOException {
        if (snippets.hasNext()) {
            ser.startTag(null, wrappingTag);
            ser.ignorableWhitespace(NEWLINE + NEWLINE);
            writeSnippets(ser, snippets);
            ser.endTag(null, wrappingTag);
            ser.ignorableWhitespace(NEWLINE + NEWLINE + NEWLINE);
        }
    }

    private void writeSnippets(XmlSerializer ser, Iterator snippets)
            throws IOException {
        while (snippets.hasNext()) {
            SnippetInstanceTO snip = (SnippetInstanceTO) snippets.next();
            ser.startTag(null, SNIPPET_TAG);
            ser.attribute(null, INSTANCE_ID_ATTR, snip.getInstanceID());
            ser.attribute(null, TYPE_ATTR, snip.getSnippetID());
            ser.attribute(null, VERSION_ATTR, snip.getSnippetVersion());
            if (XMLUtils.hasValue(snip.getPersisterID()))
                ser.attribute(null, PERSISTER_ATTR, snip.getPersisterID());
            if (XMLUtils.hasValue(snip.getPersistedText()))
                ser.cdsect(snip.getPersistedText());
            ser.endTag(null, SNIPPET_TAG);
            ser.ignorableWhitespace(NEWLINE + NEWLINE);
        }
    }



    private static final String ENCODING = "UTF-8";

    private static final String NEWLINE = "\r\n";

    private static final String DOC_ROOT_ELEM = "pdashCmsPage";

    private static final String PAGE_TITLE_TAG = "pageTitle";

    private static final String METADATA_TAG = "pageMetadata";

    private static final String PARAM_TAG = "param";

    private static final String NAME_ATTR = "name";

    private static final String PAGE_HEADING_TAG = "pageHeading";

    private static final String PAGE_FOOTER_TAG = "pageFooter";

    private static final String SNIPPET_TAG = "snippet";

    private static final String TYPE_ATTR = "type";

    private static final String INSTANCE_ID_ATTR = "instanceId";

    private static final String VERSION_ATTR = "version";

    private static final String PERSISTER_ATTR = "persister";

}
