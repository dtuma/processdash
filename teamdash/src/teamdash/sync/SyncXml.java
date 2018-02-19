// Copyright (C) 2017 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.util.XMLUtils;

public class SyncXml {

    public static void main(String[] args) throws Exception {
        // retrieve the command-line arguments for the application
        String location = args[0];
        String xmlFile = args[1];
        Authenticator.setDefault(new SystemPropertyHttpAuth());
        long start = System.currentTimeMillis();

        // create a data target for synchronization
        TeamProjectDataTarget dataTarget = TeamProjectDataTargetFactory
                .getBatchProcessTarget(location);

        // read the given XML file
        Element xml = XMLUtils.parse(new FileInputStream(xmlFile))
                .getDocumentElement();
        String extSystemID = xml.getAttribute("extSystemID");
        String extSystemName = xml.getAttribute("extSystemName");
        if (!XMLUtils.hasValue(extSystemName))
            extSystemName = extSystemID;

        // parse the node descriptions found in the XML file
        List<ExtNode> extNodes = new ArrayList<ExtNode>();
        for (Element n : XMLUtils.getChildElements(xml))
            extNodes.add(new XmlNode(n));

        // perform the synchronization operation
        ExtSyncCoordinator coord = new ExtSyncCoordinator(dataTarget,
                extSystemName, extSystemID);
        List<ExtChange> changes = coord.run(extNodes);

        // dispose of resources
        dataTarget.dispose();

        // write external changes
        writeChanges(changes, args.length > 2 ? args[2] : null);

        // display elapsed time
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        ExtSynchronizer.log.fine("Synchronization took " + elapsed + " ms.");
    }

    public static void writeChanges(List<ExtChange> changes, String outFile)
            throws IOException {
        OutputStream out = (outFile == null ? System.out
                : new FileOutputStream(outFile));

        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, "UTF-8");
        xml.startDocument("UTF-8", null);
        xml.startTag(null, "extChanges");

        for (ExtChange c : changes) {
            xml.startTag(null, "change");
            xml.attribute(null, "nodeId", c.extNode.getID());
            xml.attribute(null, "name", c.extNode.getName());

            for (Entry<String, Object> e : c.attrValues.entrySet()) {
                xml.startTag(null, "attr");
                xml.attribute(null, "name", e.getKey());
                xml.attribute(null, "value", e.getValue().toString());
                xml.endTag(null, "attr");
            }

            xml.endTag(null, "change");
        }

        xml.endTag(null, "extChanges");
        xml.endDocument();
    }


    private static class XmlNode implements ExtNode {

        private Element xml;

        public XmlNode(Element xml) {
            this.xml = xml;
        }

        @Override
        public String getID() {
            return xml.getAttribute("id");
        }

        @Override
        public String getName() {
            return xml.getAttribute("name");
        }

        @Override
        public String getOwner() {
            return xml.getAttribute("owner");
        }

        @Override
        public String getUrl() {
            return xml.getAttribute("url");
        }

        @Override
        public Double getEstimatedHours() {
            return getDoubleAttr("estHours", null);
        }

        @Override
        public Double getRemainingHours() {
            return getDoubleAttr("remHours", null);
        }

        @Override
        public Double getActualHours() {
            return getDoubleAttr("actHours", ZERO);
        }

        private Double getDoubleAttr(String attrName, Double defaultValue) {
            if (xml.hasAttribute(attrName))
                return XMLUtils.getXMLNum(xml, attrName);
            else
                return defaultValue;
        }

        private static final Double ZERO = Double.valueOf(0.0);
    }


    private static class SystemPropertyHttpAuth extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                    System.getProperty("teamdash.wbs.username"),
                    System.getProperty("teamdash.wbs.password").toCharArray());
        }
    }

}
