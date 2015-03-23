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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.SwingWorker;
import net.sourceforge.processdash.util.FileUtils;

public class WBSSaveAsAction extends AbstractAction {

    private WBSEditor wbsEditor;

    private WBSOpenFileAction openAction;

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Save_As");

    public WBSSaveAsAction(WBSEditor wbsEditor, WBSOpenFileAction openAction) {
        super(resources.getString("Menu"));
        putValue(MNEMONIC_KEY, new Integer(resources.getString("Mnemonic")
            .charAt(0)));
        this.wbsEditor = wbsEditor;
        this.openAction = openAction;
    }

    public void actionPerformed(ActionEvent e) {
        run();
    }

    private void run() {
        // Start an asynchronous thread to perform the "Save As" work.
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();

        // Prompt the user for the location where the file should be saved.
        File destFile = getDestinationFile();

        // tell the worker about the user's selection. This line will block if
        // necessary until the work is completed and the file has been written.
        workerThread.userSetSelectedDestFile(destFile);

        // If the user didn't cancel, display a message with the results of
        // the operation
        if (destFile != null)
            displayResults(destFile, workerThread);
    }


    private File getDestinationFile() {
        // prompt the user to select a file to save to
        JFileChooser fileChooser = makeFileChooser();
        int userChoice = fileChooser.showSaveDialog(wbsEditor.frame);
        openAction.lastDirectory = fileChooser.getCurrentDirectory();
        if (userChoice != JFileChooser.APPROVE_OPTION)
            return null;

        // if they selected a file that did not end with the "zip" suffix,
        // append it
        File f = fileChooser.getSelectedFile();
        if (f != null && !f.getName().toLowerCase().endsWith(".zip"))
            f = new File(f.getParentFile(), f.getName() + ".zip");

        if (f.isFile()) {
            String title = resources.getString("Overwrite.Title");
            String message = resources.format("Overwrite.Message_FMT",
                    f.getPath());
            userChoice = JOptionPane.showConfirmDialog(wbsEditor.frame,
                message, title, JOptionPane.OK_CANCEL_OPTION);
            if (userChoice != JOptionPane.OK_OPTION)
                f = null;
        }

        return f;
    }

    JFileChooser makeFileChooser() {
        JFileChooser fc = openAction.makeFileChooser();
        fc.setDialogTitle(resources.getString("Dialog_Title"));
        return fc;
    }


    private void displayResults(File destFile, WorkerThread workerThread) {
        if (workerThread.exception == null)
            displaySuccessMessage(destFile);
        else
            displayErrorDialog(workerThread.exception);
    }

    private void displaySuccessMessage(File destFile) {
        String title = resources.getString("Success.Title");
        Object[] message = new Object[] {
                resources.formatStrings("Success.Saved_Message_FMT",
                        destFile.getPath()),
                " ",
                resources.formatStrings("Success.Still_Old_"
                        + wbsEditor.workingDirResKey() + "_FMT",
                    wbsEditor.workingDirectory.getDescription()),
                " ",
                resources.getString("Success.Open_New_Window_Prompt")
        };
        int userChoice = JOptionPane.showConfirmDialog(wbsEditor.frame,
            message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (userChoice == JOptionPane.YES_OPTION)
            openAction.openFile(destFile);
    }

    private void displayErrorDialog(IOException exceptionEncountered) {
        String title = resources.getString("Error.Title");
        String[] message = resources.getStrings("Error.Message");
        JOptionPane.showMessageDialog(wbsEditor.frame, message, title,
            JOptionPane.ERROR_MESSAGE);
    }



    private class WorkerThread extends SwingWorker {

        boolean waitingForWorker = true;

        boolean waitingForUser = true;

        boolean finished = false;

        File destFile = null;

        JDialog progressDialog;

        IOException exception;


        @Override
        public Object construct() {
            try {
                WBSSaveAsWorker worker = new WBSSaveAsWorker(wbsEditor);
                File tmpZipFile = worker.run();
                synchronized (this) {
                    this.waitingForWorker = false;
                }

                File destFile = getUserSelectedDestFile();
                if (destFile != null)
                    FileUtils.copyFile(tmpZipFile, destFile);

            } catch (IOException e) {
                e.printStackTrace();
                this.exception = e;
            }

            if (progressDialog != null) {
                // sleep for one half-second, to accomplish two things:
                // 1. Ensure that the dialog is visible for at least
                //    that length of time (a brief flash is unsettling)
                // 2. Avoid a race condition where dispose() in the
                //    finished method is called after the dialog is created
                //    in userSetSelectedDestFile, but before it is shown in
                //    that same method
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }

            synchronized (this) {
                this.finished = true;
                notifyAll();
            }
            return null;
        }

        @Override
        public synchronized void finished() {
            if (progressDialog != null)
                progressDialog.dispose();
        }

        private synchronized File getUserSelectedDestFile() {
            while (waitingForUser)
                doWait();
            return destFile;
        }

        private void userSetSelectedDestFile(File f) {
            synchronized (this) {
                if (f != null && waitingForWorker)
                    progressDialog = makeProgressDialog();
                this.destFile = f;
                this.waitingForUser = false;
                notifyAll();
            }
            if (progressDialog != null) {
                // we are still waiting on the worker. display a progress
                // dialog in the meantime.  (This will block.)
                progressDialog.setVisible(true);
            } else if (f != null)
                // we are not waiting on the worker, so the only remaining
                // step is to copy the temporary ZIP to the final location.
                // that should finish very quickly; wait until it does.
                waitUntilFinished();
        }

        private JDialog makeProgressDialog() {
            JDialog d = new JDialog(wbsEditor.frame, true);
            d.setTitle(resources.getString("Progress.Title"));
            d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

            JLabel prompt = new JLabel(resources.getString("Progress.Prompt"));
            prompt.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 10));
            panel.add(prompt, BorderLayout.NORTH);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.CENTER);

            panel.add(Box.createHorizontalStrut(200), BorderLayout.SOUTH);
            d.getContentPane().add(panel);
            d.pack();
            d.setLocationRelativeTo(wbsEditor.frame);
            return d;
        }

        private synchronized void waitUntilFinished() {
            while (!finished)
                doWait();
        }

        private void doWait() {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

    }

}
