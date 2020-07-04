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

package net.sourceforge.processdash.team.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.team.TeamDataConstants;

public class TeamSettingsRepublisher {

    private static final DashboardPermission PERMISSION = //
            new DashboardPermission("teamSettingsRepublisher");

    private static final Logger logger = Logger
            .getLogger(TeamSettingsRepublisher.class.getName());

    private static TeamSettingsRepublisher INSTANCE = null;

    public static void init(DashboardContext ctx) {
        PERMISSION.checkPermission();
        if (INSTANCE == null)
            INSTANCE = new TeamSettingsRepublisher(ctx);
    }

    public static TeamSettingsRepublisher getInstance() {
        return INSTANCE;
    }



    private DashboardContext ctx;

    /** @since 2.5.6 */
    private boolean team;

    private volatile long needDate, publishDate;

    private BackgroundRepublisher worker;

    private TeamSettingsRepublisher(DashboardContext ctx) {
        this.ctx = ctx;
        this.team = Settings.isTeamMode();
        this.needDate = team ? System.currentTimeMillis() : -1;
        this.worker = new BackgroundRepublisher();
        this.worker.start();
    }


    /**
     * Request a republish operation. The operation will start as soon as
     * possible and run in a background thread. If the operation fails, it will
     * be automatically retried (at intervals) until it succeeds.
     */
    public void requestRepublish() {
        this.needDate = System.currentTimeMillis();
        synchronized (worker) {
            worker.notifyAll();
        }
    }


    /**
     * Republish all team settings files.
     * 
     * @param force
     *            if false, settings files will only be written if they are
     *            out-of-date. If true, all settings files will be rewritten
     *            regardless of their status
     * @return a list of team project paths whose files could not be
     *         successfully written. If the return list is empty, the republish
     *         operation completed successfully.
     */
    public synchronized List<String> republish(boolean force) {
        List<String> errors = new ArrayList<String>();
        long publishDate = System.currentTimeMillis();

        republish(ctx.getHierarchy(), PropertyKey.ROOT, force, errors);

        if (errors.isEmpty()) {
            this.publishDate = publishDate;
            long elapsed = System.currentTimeMillis() - publishDate;
            logger.fine("Republished settings.xml for all team projects; took "
                    + elapsed + " ms");
        }

        return errors;
    }

    /**
     * Recurse over the hierarchy and publish each team project.
     */
    private void republish(DashHierarchy hier, PropertyKey key, boolean force,
            List<String> errors) {
        String templateID = hier.pget(key).getID();
        if (team ? isTeamProjectRoot(templateID)
                 : isPersonalProjectRoot(templateID, key)) {
            String projectPath = key.path();
            try {
                republishProject(projectPath, force);
            } catch (IOException ioe) {
                if (force)
                    ioe.printStackTrace();
                logger.log(Level.FINE, "Could not publish settings.xml " //
                        + "file for " + projectPath, ioe);
                errors.add(projectPath);
            }

        } else {
            for (int i = hier.getNumChildren(key); i-- > 0;)
                republish(hier, hier.getChildKey(key, i), force, errors);
        }
    }

    static boolean isTeamProjectRoot(String templateID) {
        if (templateID == null)
            return false;
        else
            return (templateID.endsWith("/TeamRoot")
                    || templateID.endsWith("/MasterRoot"));
    }

    /** @since 2.5.6 */
    private boolean isPersonalProjectRoot(String templateID, PropertyKey key) {
        if (templateID != null && templateID.endsWith("/Indiv2Root")) {
            String dataName = DataRepository.createDataName(key.path(),
                TeamDataConstants.PERSONAL_PROJECT_FLAG);
            SimpleData sd = ctx.getData().getSimpleValue(dataName);
            return (sd != null && sd.test());
        }
        return false;
    }

    /**
     * Republish the settings file for a single project.
     */
    public void republishProject(String path, boolean force)
            throws IOException {
        String dir = getStr(path, TeamDataConstants.TEAM_DATA_DIRECTORY);
        String url = getStr(path, TeamDataConstants.TEAM_DATA_DIRECTORY_URL);

        TeamSettingsFile tsf = new TeamSettingsFile(dir, url);
        tsf.read();
        if (!tsf.isReadOnly() && tsf.isDatasetMatch()
                && (tsf.maybeUpdateProjectNameFromPath(path) || force
                        || tsf.needsRefresh())) {
            tsf.write();
            logger.fine("Republished settings.xml file for " + path);
        }
    }

    private String getStr(String path, String name) {
        String dataName = DataRepository.createDataName(path, name);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }



    /**
     * Background thread for performing asynchronous republish operations.
     */
    private class BackgroundRepublisher extends Thread {

        BackgroundRepublisher() {
            super("TeamSettingsRepublisher");
            setDaemon(true);
        }

        @Override
        public void run() {
            // wait a moment after initial startup
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
            }

            while (true) {
                // make a note of the date when a republish was last requested
                long currentNeedDate = needDate;

                // if a republish is needed, perform one
                if (needsRepublish())
                    republish(false);

                if (currentNeedDate != needDate) {
                    // if a new request came in while we were republishing,
                    // perform an immediate republish

                } else {
                    // if no new request has arrived, wait before publishing
                    // again. If our publishing attempt failed, wait one minute
                    // before trying again. If the publish succeeded and the
                    // data is up to date, wait until we receive a new request
                    synchronized (this) {
                        try {
                            this.wait(needsRepublish() ? 60000 : 0);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }

        private boolean needsRepublish() {
            return needDate > publishDate;
        }

    }

}
