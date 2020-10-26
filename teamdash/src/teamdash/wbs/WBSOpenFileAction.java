// Copyright (C) 2012-2020 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.CompressedWorkingDirectory;
import net.sourceforge.processdash.tool.quicklauncher.TeamToolsVersionManager;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.RuntimeUtils;

public class WBSOpenFileAction extends AbstractAction {

    private WBSEditor wbsEditor;

    private JFrame parentFrame;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.File_Open");

    public WBSOpenFileAction(WBSEditor wbsEditor, JFrame frame) {
        super(resources.getString("Menu"));
        putValue(SMALL_ICON, IconFactory.getOpenIcon());
        putValue(MNEMONIC_KEY, new Integer(resources.getString("Mnemonic")
                .charAt(0)));
        this.wbsEditor = wbsEditor;
        this.parentFrame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        File f = promptForFile();
        if (f != null && !wbsEditor.isCurrentlyShowingFile(f))
            openFile(f.getPath());
    }

    private File promptForFile() {
        return promptForExistingFile(resources.getString("Dialog_Title"));
    }

    File promptForExistingFile(String dialogTitle) {
        JFileChooser fc = makeFileChooser();
        fc.setDialogTitle(dialogTitle);
        while (true) {
            int userChoice = fc.showOpenDialog(parentFrame);
            lastDirectory = fc.getCurrentDirectory();
            if (userChoice != JFileChooser.APPROVE_OPTION)
                return null;

            File f = fc.getSelectedFile();
            if (f == null)
                return null;

            // if the file doesn't exist, but appending ".zip" solves the
            // problem, make it so.
            if (!f.exists() && !f.getName().toLowerCase().endsWith(".zip")) {
                File f2 = new File(f.getParentFile(), f.getName() + ".zip");
                if (f2.isFile())
                    f = f2;
            }

            String errKey = getErrorKey(f);
            if (errKey == null) {
                return f;
            } else {
                showErrorDialog(errKey, f.getPath());
            }
        }
    }

    private String getErrorKey(File f) {
        if (!f.isFile())
            return "No_Such_File";
        if (!f.canRead())
            return "Cannot_Read_File";
        if (!CompressedWorkingDirectory.isZipFileContaining(f,
            WBSFilenameConstants.WBS_FILENAME))
            return "Not_WBS_File";
        return null;
    }

    public void openFile(String path) {
        // find the team tools JAR file that should be used to open this file
        File teamToolsJar = TeamToolsVersionManager
                .getBestTeamToolsJarFor(new File(path), null);
        if (teamToolsJar == null)
            teamToolsJar = RuntimeUtils.getClasspathFile(WBSEditor.class);

        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(RuntimeUtils.getJreExecutable());
        cmdLine.add(RuntimeUtils.getJvmHeapArg());
        cmdLine.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // set a reasonable application menu name/icon on Mac OS X
        if (MacGUIUtils.isMacOSX()) {
            cmdLine.add("-Xdock:name=" + resources.getString("Window.App_Name"));
            File icon = new File(teamToolsJar.getParentFile(), "wbs-editor.icns");
            if (icon.isFile())
                cmdLine.add("-Xdock:icon=" + icon.getAbsolutePath());
        }

        cmdLine.add("-cp");
        cmdLine.add(teamToolsJar.getPath());
        cmdLine.add(WBSEditor.class.getName());

        cmdLine.add(path);

        String[] exec = cmdLine.toArray(new String[cmdLine.size()]);
        try {
            Process proc = Runtime.getRuntime().exec(exec);
            new ProcessWatcher(path, proc).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            showErrorDialog("Unexpected_Error", path);
        }
    }

    private void showErrorDialog(String resKey, String path) {
        String title = resources.getString("Error." + resKey + "_Title");
        String[] message = resources.formatStrings("Error." + resKey + "_FMT",
            path);
        JOptionPane.showMessageDialog(parentFrame, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    JFileChooser makeFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new ExampleFileFilter("zip", resources
                .getString("File_Type_Description")));
        if (lastDirectory != null)
            fc.setCurrentDirectory(lastDirectory);
        return fc;
    }

    File lastDirectory;


    private class ProcessWatcher extends Thread {

        String path;

        Process proc;

        long startTime;

        public ProcessWatcher(String path, Process proc) {
            this.path = path;
            this.proc = proc;
            this.startTime = System.currentTimeMillis();
            setDaemon(true);
        }

        public void run() {
            // wait for the subprocess to write at least a byte of output;
            // then let the WBS Editor know we opened the file
            waitForOutput();
            wbsEditor.fileWasOpened();

            // When we launch a subprocess, we must consume the data it writes
            // to stdout and stderr - otherwise it will hang.
            int exitCode = RuntimeUtils.doWaitFor(proc);

            // When we launch a Java process, the <i>process</i> might launch
            // successfully (meaning we don't get an IOException), but the
            // process itself might exit almost immediately with a nonzero exit
            // status (for example, due to a classpath problem). If that occurs,
            // display an error message to the user.
            if (exitCode != 0) {
                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;
                long elapsedMin = elapsed / 60000;
                if (elapsedMin < 2)
                    showErrorDialog("Unexpected_Error", path);
            }
        }

        private void waitForOutput() {
            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            while (true) {
                try {
                    // if any output is available, return immediately
                    if (in.available() > 0 || err.available() > 0)
                        return;

                    // otherwise, wait a fraction of a second
                    Thread.sleep(100);

                    // try getting the subprocess exit value. If this returns
                    // without exception, the process is done and we can exit.
                    proc.exitValue();
                    return;

                } catch (Exception e) {
                }
            }
        }

    }

}
