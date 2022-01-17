// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle.ui;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMigrator;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class BundleMigrationPage extends TinyCGIBase {

    private boolean isPersonal;

    private File directory;

    private FileBundleMode bundleMode;

    private void loadValues() throws IOException {
        DataContext data = getDataContext();

        // check whether this is a personal project
        SimpleData personalFlag = data
                .getSimpleValue(TeamDataConstants.PERSONAL_PROJECT_FLAG);
        isPersonal = personalFlag != null && personalFlag.test();

        // retrieve the team project data directory
        SimpleData teamDir = data
                .getSimpleValue(TeamDataConstants.TEAM_DATA_DIRECTORY);
        if (teamDir == null || !teamDir.test())
            throw new TinyCGIException(404, "Project not found");

        // resolve the directory and get the bundle mode
        directory = new File(ExternalResourceManager.getInstance()
                .remapFilename(teamDir.format()));
        bundleMode = FileBundleUtils.getBundleMode(directory);
    }



    @Override
    protected void doGet() throws IOException {
        if (parameters.containsKey("nameAsText"))
            writeModeNameAsText();
        else
            writeHtmlSummary();
    }

    private void writeModeNameAsText() throws IOException {
        out.write("Content-Type: text/plain\r\n\r\n");
        try {
            loadValues();
            out.print(bundleMode == null ? "not bundled" : bundleMode.getName());
        } catch (Exception e) {
            out.print("?????");
        }
    }

    private void writeHtmlSummary() throws IOException {
        loadValues();

        writeHtmlPageHeader("");

        out.println("<p>This project stores data in the directory:</p>");
        out.println("<pre>" + esc(directory.getPath()) + "</pre>");

        if (bundleMode == null) {
            out.println("<p>That directory is <b>not using</b> "
                    + "bundled file storage.</p>");
        } else {
            out.println("<p>That directory is using bundled file storage in <b>"
                    + bundleMode.getName() + "</b> mode.</p>");
        }

        if (hasPermission()) {
            out.println("<form action='bundleMode' method='POST'>");
            writePostTokenFormElement(true);
            out.println("<input type='submit' name='c' value='"
                    + (bundleMode == null ? "Bundle" : "Unbundle")
                    + " this directory'>");
            out.println("</form>");
        }

        out.println("</body></html>");
    }

    private boolean hasPermission() {
        return isPersonal || Settings.isTeamMode();
    }

    private void writeHtmlPageHeader(String bodyAttrs) {
        writeHeader();
        out.println("<html><head>");
        out.println("<title>Project Bundle Mode</title>");
        out.println("<style>pre, form { margin-left: 1cm }</style>");
        out.println("</head><body" + bodyAttrs + ">");
        out.println("<h1>" + esc(getPrefix()) + "</h1>");
        out.println("<h2>Project Bundle Mode</h2>");
    }


    @Override
    protected void doPost() throws IOException {
        loadValues();
        parseFormData();
        long waitUntil = System.currentTimeMillis() + 1500;

        try {
            if (!checkPostToken()) {
                // no post token was found - make no changes

            } else if (!hasPermission()) {
                // no permission to make changes

            } else if (!parameters.containsKey("run")) {
                writePleaseWait();
                return;

            } else if (bundleMode == null) {
                FileBundleMigrator.migrate(directory,
                    TeamDataDirStrategy.INSTANCE, FileBundleMode.Local);

            } else {
                FileBundleMigrator.unmigrate(directory,
                    TeamDataDirStrategy.INSTANCE);
            }

        } catch (LockFailureException lfe) {
            throw new TinyCGIException(500, "Cannot acquire lock");
        }

        long delay = waitUntil - System.currentTimeMillis();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
            }
        }

        out.write("Location: bundleMode\r\n\r\n");
    }


    private void writePleaseWait() {
        writeHtmlPageHeader(" onload=\"document.forms['run'].submit()\"");

        out.println("Migrating, please wait...");

        out.println("<br/><form name='run' action='bundleMode' method='POST'>");
        writePostTokenFormElement(false);
        out.println("<input type='hidden' name='run' value='run'>");
        out.println("</form>");

        out.println("</body></html>");
    }

    private String esc(String text) {
        return HTMLUtils.escapeEntities(text);
    }

}
