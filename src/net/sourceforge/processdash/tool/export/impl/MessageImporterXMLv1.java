// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import net.sourceforge.processdash.BackgroundTaskManager;
import net.sourceforge.processdash.msg.MessageDispatcher;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MessageImporterXMLv1 implements
        ArchiveMetricsFileImporter.Handler, ArchiveMetricsXmlConstants {

    public boolean canHandle(String type, String version) {
        return FILE_TYPE_MESSAGE.equals(type) && "1".equals(version);
    }

    public void handle(ArchiveMetricsFileImporter caller, InputStream in,
            String type, String version) throws Exception {

        // read the XML document from the input stream. XMLUtils.parse()
        // closes the input stream after reading the document, which is not
        // acceptable for our purposes; so we have to read and cache the
        // file ourselves first.
        InputStream xmlIn = new ByteArrayInputStream(FileUtils.slurpContents(
            in, false));
        Document doc = XMLUtils.parse(xmlIn);

        // Now, find all of the messages in the document
        NodeList messages = doc.getElementsByTagName(MESSAGE_TAG);
        if (messages != null) {
            for (int i = 0; i < messages.getLength(); i++) {
                Element msg = (Element) messages.item(i);
                MessageEvent msgEvent = new MessageEvent(msg);

                // Register a task to dispatch each message later on the
                // background thread.  (Message handling logic is defined
                // by third parties, and we have no guarantee that it will
                // finish in a timely manner.  We can't risk hanging the
                // import operation indefinitely.)
                BackgroundTaskManager.getInstance().addTask(
                    new MessageDispatchTask(msgEvent));
            }
        }

        NodeList nl = doc.getElementsByTagName(DELETE_TAG);
        if (nl != null && nl.getLength() > 0)
            caller.deleteArchiveFileOnCompletion();
    }

    private static class MessageDispatchTask implements Runnable {
        private MessageEvent event;
        private MessageDispatchTask(MessageEvent event) {
            this.event = event;
        }
        public void run() {
            MessageDispatcher.getInstance().dispatch(event, false);
        }
    }

    private static final String MESSAGE_TAG = "message";

    private static final String DELETE_TAG = "deleteEnclosingArchive";

}
