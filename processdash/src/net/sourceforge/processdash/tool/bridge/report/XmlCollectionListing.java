// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.tool.bridge.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;


/**
 * A report which generates an XML document with the names, modification times,
 * and checksums of the resources in a collection.
 */
public class XmlCollectionListing implements CollectionReport {

    public static final XmlCollectionListing INSTANCE = new XmlCollectionListing();


    public String getContentType() {
        return "text/xml";
    }

    public void runReport(ResourceCollection c, List<String> resources,
            OutputStream out) throws IOException {
        XmlSerializer ser = XMLUtils.getXmlSerializer(true);

        ser.setOutput(out, ENCODING);
        ser.startDocument(ENCODING, null);
        ser.startTag(null, DOCUMENT_TAG);

        for (String resourceName : resources) {
            long lastMod = c.getLastModified(resourceName);
            if (lastMod < 1)
                continue;

            Long checksum = c.getChecksum(resourceName);
            if (checksum == null)
                continue;

            ser.startTag(null, RESOURCE_TAG);
            ser.attribute(null, NAME_ATTR, resourceName);
            ser.attribute(null, MOD_TIME_ATTR, Long.toString(lastMod));
            ser.attribute(null, CHECKSUM_ATTR, checksum.toString());
            ser.endTag(null, RESOURCE_TAG);
        }

        ser.endTag(null, DOCUMENT_TAG);
        ser.endDocument();
    }

    public static ResourceCollectionInfo parseListing(InputStream in)
            throws IOException {
        try {
            Element data = XMLUtils.parse(in).getDocumentElement();
            ResourceListing result = new ResourceListing();
            NodeList resourceElems = data.getElementsByTagName(RESOURCE_TAG);
            if (resourceElems != null) {
                for (int i = 0; i < resourceElems.getLength(); i++) {
                    Element res = (Element) resourceElems.item(i);
                    String name = res.getAttribute(NAME_ATTR);
                    long mod = Long.parseLong(res.getAttribute(MOD_TIME_ATTR));
                    long sum = Long.parseLong(res.getAttribute(CHECKSUM_ATTR));
                    result.addResource(name, mod, sum);
                }
            }
            return result;
        } catch (Exception e) {
            IOException ioe = new IOException(
                    "Unable to parse collection listing");
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static final String ENCODING = "UTF-8";

    private static final String DOCUMENT_TAG = "resourceList";

    private static final String RESOURCE_TAG = "resource";

    private static final String NAME_ATTR = "name";

    private static final String MOD_TIME_ATTR = "modTime";

    private static final String CHECKSUM_ATTR = "checksum";

}
