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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.XMLUtils;



public class ExtSyncDaemonWBS {

    protected Logger log;

    protected Properties globalConfig;

    protected String systemName, systemID;

    protected ExtSystemConnection connection;

    protected String wbsLocation;


    public ExtSyncDaemonWBS(ExtSyncDaemon parent, String wbsLocation) {
        this.log = parent.log;
        this.globalConfig = parent.globalConfig;
        this.systemName = parent.systemName;
        this.systemID = parent.systemID;
        this.connection = parent.connection;
        this.wbsLocation = wbsLocation;
    }


    public void run() throws Exception {
        // create a data target for synchronization
        TeamProjectDataTarget dataTarget = TeamProjectDataTargetFactory
                .getBatchProcessTarget(wbsLocation);

        try {
            // run a loop to synchronize this WBS
            run(dataTarget);
        } finally {
            // dispose of resources when finished
            dataTarget.dispose();
        }
    }

    protected void run(TeamProjectDataTarget dataTarget) throws Exception {
        // retrieve project-specific sync configuration
        Element targetConfig = getConfigXml(dataTarget);
        if (targetConfig == null) {
            log.warning("No " + systemName + " sync spec found, exiting");
            return;
        }

        // create objects to perform the synchronization of this target
        ExtSyncCoordinator coord = new ExtSyncCoordinator(dataTarget,
                systemName, systemID, globalConfig);
        ExtNodeSet nodeSet = connection.getNodeSet(targetConfig,
            coord.syncData);

        // run the sync operation, potentially multiple times
        long heartbeat = System.currentTimeMillis() + DateUtils.HOUR;
        int errCount = 0;
        int ldd = coord.isActiveSleepSupported() ? (int) DateUtils.HOUR : 5000;
        int loopDelay = ExtSyncUtil.getParamAsMillis(globalConfig,
            "loop.syncInterval", ldd);
        int retryDelay = ExtSyncUtil.getParamAsMillis(globalConfig,
            "loop.retryDelay", 5000);

        do {
            long start = System.currentTimeMillis();

            if (start > heartbeat) {
                log.info("Sync daemon is alive");
                heartbeat = start + DateUtils.HOUR;
            }

            try {
                // perform the synchronization operation
                coord.run(nodeSet);

                // record successful completion of sync operation
                if (errCount > 0)
                    log.info("Connection reestablished, "
                            + "synchronization resuming");
                errCount = 0;

                // display elapsed time
                long end = System.currentTimeMillis();
                long elapsed = end - start;
                log.fine("Synchronization took " + elapsed + " ms.");

            } catch (SyncDataFile.ComodificationException sdfce) {
                // another sync daemon process has started; we should exit
                log.info("A new sync daemon has started; old daemon will exit");
                return;
            } catch (Exception e) {
                log.log(errCount == 0 ? Level.SEVERE : Level.FINE,
                    "Encountered problem while synchronizing", e);
                connection.disconnect();
                errCount++;
            }

            // delay before repeating the loop again
            long nextStart = start
                    + loopDelayMillis(loopDelay, errCount, retryDelay);
            long wait = nextStart - System.currentTimeMillis();
            if (loopDelay == 0) {
                log.info("Press enter to repeat.");
                System.console().readLine();
            } else if (wait > 0) {
                coord.sleep(wait);
            }

        } while (loopDelay >= 0);
    }

    private Element getConfigXml(TeamProjectDataTarget dataTarget)
            throws IOException {
        // find the configuration file in the WBS data directory
        dataTarget.update();
        File extConfig = new File(dataTarget.getDirectory(),
                ExtSyncUtil.EXT_SPEC_FILE);
        if (!extConfig.isFile())
            return null;

        // parse the configuration file as an XML document
        Element xmlDoc;
        try {
            BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(extConfig));
            xmlDoc = XMLUtils.parse(in).getDocumentElement();
        } catch (SAXException se) {
            throw new IOException(se);
        }

        // find the specification for this system in the document
        for (Element systemXml : XMLUtils.getChildElements(xmlDoc)) {
            String extSystemType = systemXml.getAttribute("type");
            if (extSystemType.equalsIgnoreCase(systemID))
                return systemXml;
        }

        // no configuration was found
        return null;
    }

    private int loopDelayMillis(int loopDelay, int errCount, int retryDelay) {
        if (loopDelay <= 0 || errCount == 0)
            return loopDelay;
        else if (errCount < 2)
            return retryDelay;
        else if (errCount < 4)
            return 5 * retryDelay;
        else
            return 10 * retryDelay;
    }

}
