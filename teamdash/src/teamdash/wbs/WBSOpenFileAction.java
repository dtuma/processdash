// Copyright (C) 2012 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.CompressedWorkingDirectory;
import net.sourceforge.processdash.ui.lib.ExampleFileFilter;
import net.sourceforge.processdash.util.RuntimeUtils;

public class WBSOpenFileAction extends AbstractAction {

    private JFrame parentFrame;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.File_Open");

    public WBSOpenFileAction(JFrame frame) {
        super(resources.getString("Menu"));
        putValue(MNEMONIC_KEY, new Integer(resources.getString("Mnemonic")
                .charAt(0)));
        this.parentFrame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        File f = promptForFile();
        if (f != null)
            openFile(f);
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
                showErrorDialog(errKey, f);
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

    void openFile(File f) {
        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(RuntimeUtils.getJreExecutable());
        cmdLine.add(RuntimeUtils.getJvmHeapArg());
        cmdLine.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // provide the classpath for the new JVM.  Note this assumes that the
        // WBS Editor code is packaged up into the TeamTools.jar file, so only
        // one classpath entry is needed.  As such, this "Open" action will
        // not work when the WBS Editor is run in a development environment.
        cmdLine.add("-cp");
        cmdLine.add(RuntimeUtils.getClasspathFile(WBSEditor.class).getPath());
        cmdLine.add(WBSEditor.class.getName());

        cmdLine.add(f.getPath());

        String[] exec = cmdLine.toArray(new String[cmdLine.size()]);
        try {
            Process proc = Runtime.getRuntime().exec(exec);
            new ProcessWatcher(f, proc).start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            showErrorDialog("Unexpected_Error", f);
        }
    }

    private void showErrorDialog(String resKey, File f) {
        String title = resources.getString("Error." + resKey + "_Title");
        String[] message = resources.formatStrings("Error." + resKey + "_FMT",
            f.getPath());
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

        File f;

        Process proc;

        long startTime;

        public ProcessWatcher(File f, Process proc) {
            this.f = f;
            this.proc = proc;
            this.startTime = System.currentTimeMillis();
            setDaemon(true);
        }

        public void run() {
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
                    showErrorDialog("Unexpected_Error", f);
            }
        }

    }

}
