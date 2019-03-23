// Copyright (C) 2019 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.ui.lib.TransferHandlerUtils;
import net.sourceforge.processdash.util.FileUtils;

public class DashboardDropTransferHandler extends TransferHandler {

    private WorkingDirectory workingDir;


    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Drag_and_Drop");


    public DashboardDropTransferHandler(WorkingDirectory workingDir) {
        this.workingDir = workingDir;
    }


    @Override
    public boolean canImport(TransferSupport support) {
        return TransferHandlerUtils.hasFileListFlavor(support.getDataFlavors());
    }


    @Override
    public boolean importData(TransferSupport support) {
        boolean importedFile = false;

        // retrieve a list of any files/URIs that were dropped
        List<File> files = TransferHandlerUtils
                .getTransferredFileList(support.getTransferable());
        List<URI> uris = TransferHandlerUtils
                .getTransferredURIList(support.getTransferable());

        // if any PDASH files were dropped, process them
        List<File> pdashFiles = getFileList(files, PDASH_SUFFIX);
        if (pdashFiles != null && !pdashFiles.isEmpty() && getUserApproval(
            support.getComponent(), pdashFiles, "PDASH_File")) {
            for (File f : pdashFiles) {
                if (importPdashFile(f))
                    importedFile = true;
            }
        }

        // if any PDASH URLs were dropped, process them
        List<URI> pdashUris = getUriList(uris, "http", PDASH_SUFFIX, //
            "/setup/join.shtm$", "/setup/join.pdash");
        if (pdashUris != null && !pdashUris.isEmpty() && getUserApproval(
            support.getComponent(), pdashUris, "PDASH_URL")) {
            for (URI u : pdashUris) {
                if (importPdashUri(u))
                    importedFile = true;
            }
        }

        // tell the DataImporter to check for newly imported files
        if (importedFile)
            DataImporter.refreshPrefix("/");

        return importedFile;
    }


    private boolean getUserApproval(Component dropTarget, List items,
            String resKey) {
        // make a list of the string representations of the items
        String[] itemList = new String[items.size()];
        for (int i = itemList.length; i-- > 0;)
            itemList[i] = "        " + items.get(i).toString();

        // build the title and contents for a dialog box
        String title = resources.getString(resKey + ".Window_Title");
        Object message = new Object[] { resources.getString(resKey + ".Header"),
                itemList, " ", resources.getString(resKey + ".Footer") };

        // prompt the user and return whether they clicked "Yes"
        int userChoice = JOptionPane.showConfirmDialog(dropTarget, message,
            title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return userChoice == JOptionPane.YES_OPTION;
    }


    private boolean importPdashFile(File f) {
        if (!isImportableFile(f, PDASH_SUFFIX))
            return false;
        else
            return importPdashUri(f.toURI());
    }


    private boolean importPdashUri(URI u) {
        if (workingDir instanceof BridgedWorkingDirectory) {
            return importPdashUriToBridgedDir(u);
        } else {
            return importPdashUriToLocalDir(u);
        }
    }

    private boolean importPdashUriToBridgedDir(URI u) {
        try {
            // identify the URL destination we should copy to
            URL baseUrl = new URL(workingDir.getDescription());
            String destName = "import/" + getUriFileName(u);

            // upload the file to the import subdirectory
            InputStream in = u.toURL().openStream();
            ResourceBridgeClient.uploadSingleFile(baseUrl, destName, in);
            FileUtils.safelyClose(in);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean importPdashUriToLocalDir(URI u) {
        try {
            // make sure the "import" directory exists
            File baseDir = workingDir.getDirectory();
            File importDir = new File(baseDir, "import");
            if (!importDir.isDirectory())
                importDir.mkdir();

            // determine the name we should use for the incoming file
            File dest = new File(importDir, getUriFileName(u));

            // copy the file into the import directory
            InputStream in = u.toURL().openStream();
            FileUtils.copyFile(in, dest);
            FileUtils.safelyClose(in);

            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }


    public String getUriFileName(URI u) {
        // Find the last part of the URI path, after the final slash
        String filename = u.getPath();
        int slashPos = Math.max(filename.lastIndexOf('/'),
            filename.lastIndexOf('\\'));
        filename = filename.substring(slashPos + 1);
        return filename;
    }


    private List<File> getFileList(List<File> files, String suffix) {
        if (files == null)
            return null;

        List<File> result = new ArrayList();
        for (File f : files) {
            if (isImportableFile(f, suffix))
                result.add(f);
        }

        return result;
    }


    private List<URI> getUriList(List<URI> uris, String prefix, String suffix,
            String... replacements) {
        if (uris == null)
            return null;

        List<URI> result = new ArrayList<URI>();
        for (URI u : uris) {
            try {
                String s = u.toString();
                for (int i = 0; i < replacements.length; i += 2)
                    s = s.replaceAll(replacements[i], replacements[i + 1]);
                if (s.startsWith(prefix) && s.endsWith(suffix))
                    result.add(new URI(s));
            } catch (Exception e) {
            }
        }

        return result;

    }


    private boolean isImportableFile(File f, String suffix) {
        return f != null && f.isFile() && f.canRead()
                && f.getName().toLowerCase().endsWith(suffix);
    }


    private static final String PDASH_SUFFIX = ".pdash";

}
