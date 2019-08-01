// Copyright (C) 2011-2019 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.quicklauncher.PdbkConstants;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.XorInputStream;


public class RestoreIndivData extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {
        generatePostToken();
        out.print("Location: restoreIndivData.shtm\r\n\r\n");
    }

    protected String getDefaultPostTokenDataNameSuffix() {
        return "RestoreIndivData";
    }

    @Override
    protected void doPost() throws IOException {
        parseMultipartFormData();

        if (checkPostToken() == false) {
            doGet();
            return;
        }

        String message;
        try {
            File pdashFile = getPdashFile();
            RestoreIndivDataWorker worker = new RestoreIndivDataWorker(
                    getDashboardContext(), getPrefix(), pdashFile);
            worker.run();

            message = "Restored data at " + new Date();

        } catch (RestoreIndivDataException re) {
            if (re.getCause() != null)
                re.getCause().printStackTrace();
            message = HTMLUtils.escapeEntities(re.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            message = "An unexpected error was encountered. Open the "
                    + "<a href='/control/showConsole.class'>debug log</a> "
                    + "for more details.";
        }

        writeHeader();
        out.write("<html><body><h1>");
        out.write(HTMLUtils.escapeEntities(getPrefix()));
        out.write("</h1><h2>Restore Team Project Data</h2>");
        out.write(message);
        out.write("</body></html>");
    }

    private File getPdashFile() throws IOException {
        // If they posted a filename, use the named file
        if (getParameter("filenameOK") != null) {
            String filename = getParameter("filename");
            if (!StringUtils.hasValue(filename))
                throw new RestoreIndivDataException(
                        "You did not enter a filename.");
            if (!filename.toLowerCase().endsWith(PDASH_SUFFIX))
                throw new RestoreIndivDataException("The file '" + filename
                        + "' is not a personal data export file.");
            File f = new File(filename);
            if (!f.exists())
                throw new RestoreIndivDataException(
                        "The file '" + filename + "' does not exist.");
            return f;

        } else {
            // if they uploaded data, save it to a temp file
            InputStream in = getPdashData();
            File f = TempFileFactory.get().createTempFile("restore", ".tmp");
            FileUtils.copyFile(in, f);
            in.close();
            f.deleteOnExit();
            return f;
        }
    }

    private InputStream getPdashData() throws IOException {
        String filename = getParameter("file");
        if (!StringUtils.hasValue(filename))
            throw new RestoreIndivDataException("No file was uploaded.");
        byte[] data = (byte[]) parameters.get("file_CONTENTS");
        InputStream in = new ByteArrayInputStream(data);

        filename = filename.toLowerCase();
        if (filename.endsWith(".pdash"))
            return in;
        else if (filename.endsWith(".pdbk"))
            in = new XorInputStream(in, PdbkConstants.PDASH_BACKUP_XOR_BITS);
        else if (!filename.endsWith(".zip"))
            throw new RestoreIndivDataException("The file you uploaded "
                    + "was not a recognized file type.");

        String projectID = getStringData(TeamDataConstants.PROJECT_ID);
        String initials = getStringData(TeamDataConstants.INDIV_INITIALS);
        String entryName = "/" + projectID + "/" + initials + PDASH_SUFFIX;

        try {
            ZipInputStream zipIn = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null) {
                if (e.getName().toLowerCase().endsWith(entryName))
                    return zipIn;
            }
        } catch (IOException ioe) {
            throw new RestoreIndivDataException("The file you uploaded " //
                    + "is not a valid Team Dashboard backup file.", ioe);
        }

        throw new RestoreIndivDataException("The file you uploaded "
                + "does not contain data for this project for any team member "
                + "with the initials '" + initials + "'.");
    }

    private String getStringData(String name) {
        return getDataContext().getSimpleValue(name).format().toLowerCase();
    }

    private static final String PDASH_SUFFIX = "-data.pdash";

}
