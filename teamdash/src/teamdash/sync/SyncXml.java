// Copyright (C) 2017-2020 Tuma Solutions, LLC
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
import java.util.Set;

import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.util.XMLUtils;

public class SyncXml implements SyncXmlConstants {

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
        String extSystemID = xml.getAttribute(SYSTEM_ID_ATTR);
        String extSystemName = xml.getAttribute(SYSTEM_NAME_ATTR);
        if (!XMLUtils.hasValue(extSystemName))
            extSystemName = extSystemID;

        // create an object to manage the list of XML node data
        XmlNodeSet nodeSet = new XmlNodeSet(xml,
                args.length > 2 ? args[2] : null);

        // perform the synchronization operation
        ExtSyncCoordinator coord = new ExtSyncCoordinator(dataTarget,
                extSystemName, extSystemID, null);
        coord.run(nodeSet);

        // dispose of resources
        dataTarget.dispose();

        // display elapsed time
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        ExtSynchronizer.log.fine("Synchronization took " + elapsed + " ms.");
    }


    private static class XmlNodeSet implements ExtNodeSet {

        private Element xml;
        private String outFile;

        private XmlNodeSet(Element xml, String outFile) {
            this.xml = xml;
            this.outFile = outFile;
        }

        @Override
        public List<ExtNode> getExtNodes(Set<String> ignored) {
            return getExtNodeChildren(xml);
        }

        @Override
        public void applyWbsChanges(List<ExtChange> changes,
                SyncMetadata metadata) throws IOException {
            writeChanges(changes, outFile);
        }
    }

    private static List<ExtNode> getExtNodeChildren(Element xml) {
        List<ExtNode> extNodes = new ArrayList<ExtNode>();
        for (Element n : XMLUtils.getChildElements(xml)) {
            if (NODE_TAG.equals(n.getTagName()))
                extNodes.add(new XmlNode(n));
        }
        return extNodes;
    }

    public static void writeChanges(List<ExtChange> changes, String outFile)
            throws IOException {
        OutputStream out = (outFile == null ? System.out
                : new FileOutputStream(outFile));

        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, ENCODING);
        xml.startDocument(ENCODING, null);
        xml.startTag(null, CHANGES_TAG);

        for (ExtChange c : changes) {
            xml.startTag(null, CHANGE_TAG);
            xml.attribute(null, CHANGE_NODE_ID_ATTR, c.extNode.getID());
            xml.attribute(null, NODE_NAME_ATTR, c.extNode.getName());

            for (Entry<String, Object> e : c.attrValues.entrySet()) {
                xml.startTag(null, ATTR_CHANGE_TAG);
                xml.attribute(null, ATTR_CHANGE_NAME_ATTR, e.getKey());
                xml.attribute(null, ATTR_CHANGE_VALUE_ATTR,
                    e.getValue().toString());
                xml.endTag(null, ATTR_CHANGE_TAG);
            }

            xml.endTag(null, CHANGE_TAG);
        }

        xml.endTag(null, CHANGES_TAG);
        xml.endDocument();

        if (outFile != null) {
            out.flush();
            out.close();
        }
    }


    public static void writeExtNodes(OutputStream out, String extSystemID,
            String extSystemName, List<ExtNode> nodes) throws IOException {
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, ENCODING);
        xml.startDocument(ENCODING, null);
        xml.startTag(null, EXT_NODES_TAG);
        writeAttr(xml, SYSTEM_ID_ATTR, extSystemID);
        writeAttr(xml, SYSTEM_NAME_ATTR, extSystemName);

        writeExtNodeList(xml, nodes);

        xml.endTag(null, EXT_NODES_TAG);
        xml.endDocument();
    }

    private static void writeExtNodeList(XmlSerializer xml, List<ExtNode> nodes)
            throws IOException {
        if (nodes != null) {
            for (ExtNode n : nodes) {
                xml.startTag(null, NODE_TAG);
                writeAttr(xml, NODE_ID_ATTR, n.getID());
                writeAttr(xml, NODE_NAME_ATTR, n.getName());
                writeAttr(xml, OWNER_ATTR, n.getOwner());
                writeAttr(xml, URL_ATTR, n.getUrl());
                writeAttr(xml, EST_HOURS_ATTR, n.getEstimatedHours());
                writeAttr(xml, REM_HOURS_ATTR, n.getRemainingHours());
                writeAttr(xml, ACT_HOURS_ATTR, n.getActualHours());

                writeExtNodeList(xml, n.getChildren());

                xml.endTag(null, NODE_TAG);
            }
        }
    }

    private static void writeAttr(XmlSerializer xml, String attrName,
            Object attrValue) throws IOException {
        if (attrValue != null)
            xml.attribute(null, attrName, attrValue.toString());
    }


    private static class XmlNode implements ExtNode {

        private Element xml;

        public XmlNode(Element xml) {
            this.xml = xml;
        }

        @Override
        public String getID() {
            return xml.getAttribute(NODE_ID_ATTR);
        }

        @Override
        public String getKey() {
            return xml.getAttribute(NODE_KEY_ATTR);
        }

        @Override
        public String getName() {
            return xml.getAttribute(NODE_NAME_ATTR);
        }

        @Override
        public String getType() {
            return xml.getAttribute(TYPE_ATTR);
        }

        @Override
        public String getTypeID() {
            return xml.getAttribute(TYPE_ID_ATTR);
        }

        @Override
        public List<ExtNode> getChildren() {
            return getExtNodeChildren(xml);
        }

        @Override
        public String getOwner() {
            return xml.getAttribute(OWNER_ATTR);
        }

        @Override
        public String getUrl() {
            return xml.getAttribute(URL_ATTR);
        }

        @Override
        public Double getEstimatedHours() {
            return getDoubleAttr(EST_HOURS_ATTR, null);
        }

        @Override
        public Double getRemainingHours() {
            return getDoubleAttr(REM_HOURS_ATTR, null);
        }

        @Override
        public Double getActualHours() {
            return getDoubleAttr(ACT_HOURS_ATTR, ZERO);
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
