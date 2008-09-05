// Copyright (C) 2001-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui.lib;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/** A useful class which displays a dialog box with a progress bar,
 * then runs a sequence of tasks, updating the progress bar as each
 * task completes.
 *
 * The tasks in question can be any object which implements Runnable.
 * If, however, the task objects implement ProgressDialog.Task, they
 * can provide a string to be displayed in the progress bar, and they
 * can provide finer-grained progress completion data.
 */
public class ProgressDialog extends JDialog {

    public interface Task extends Runnable {
        String getMessage();
        int getPercentComplete();
        void addChangeListener(ChangeListener l);
    }

    public interface CancellableTask extends Task {}

    public class CancelledException extends Error {}

    private Vector tasks = new Vector();
    private JLabel messageLabel = null;
    private JButton closeButton = null;
    private Component closePanel = null;
    private JProgressBar progressBar = null;
    private String completionMessage = null;
    private boolean cancelled = false;
    private boolean running = false;
    private String closeText = "Close";

    public ProgressDialog(Frame parent, String title, String message) {
        super(parent, title, true);
        init(parent, message);
    }
    public ProgressDialog(Dialog parent, String title, String message) {
        super(parent, title, true);
        init(parent, message);
    }
    private void init(Component parent, String message) {
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel dialogContents = new JPanel(new BorderLayout(5, 5));
        dialogContents.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        getContentPane().add(dialogContents);

        if (message == null) message = "Please wait...";
        messageLabel = new JLabel(message);
        progressBar = new JProgressBar();
        dialogContents.add(messageLabel, BorderLayout.NORTH);
        dialogContents.add(progressBar,  BorderLayout.CENTER);

        closeButton = new JButton("Cancel");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (running)
                    cancelled = true;
                else
                    dispose();
            }});
        closePanel = borderComponent(closeButton);
        closePanel.setVisible(false);
        dialogContents.add(closePanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }
    public static ProgressDialog create(Object parent, String title,
            String message) {
        if (parent instanceof Frame)
            return new ProgressDialog((Frame) parent, title, message);
        else if (parent instanceof Dialog)
            return new ProgressDialog((Dialog) parent, title, message);
        else
            return null;
    }

    private Component borderComponent(Component comp) {
        Box horizBox = Box.createHorizontalBox();
        horizBox.add(Box.createHorizontalGlue());
        horizBox.add(comp);
        horizBox.add(Box.createHorizontalGlue());
        return horizBox;
    }

    /** Add a task for this dialog to perform.
     *
     * All tasks must be added <b>before</b> calling the
     * <code>run()</code> method.
     */
    public void addTask(Runnable r) { tasks.add(r); }

    /** Request that a message be displayed when the dialog completes.
     *
     * By default, the completion message is <code>null</code>, which
     * tells the progress dialog to automatically close upon
     * completion.  If a non-null completion message is set, when the
     * progress dialog finishes running it will not close automatically;
     * instead, it will change the label to display the completion
     * message, and replace the progress bar with a "Close" button.
     */
    public void setCompletionMessage(String msg) { completionMessage = msg; }


    /** State whether the tasks run by this dialog can be cancelled.
     * 
     * If this parameter is set to true, the progess dialog will display
     * a cancel button.
     */
    public void setCancellable(boolean canCancel) {
        closePanel.setVisible(canCancel);
        pack();
    }

    /** Set the text that should be used for the cancel button.
     */
    public void setCancelText(String text) {
        closeButton.setText(text);
    }

    /** Set the text that should be used for the close button.
     */
    public void setCloseText(String text) {
        this.closeText = text;
    }

    /** Displays the dialog, and runs the tasks in the order they were
     *  added.
     */
    public void run() {
        progressBar.setMaximum(tasks.size() * 100);
        WorkThread w = new WorkThread();
        w.start();
        setVisible(true);   // this will block until the work thread finishes
    }


    public void finished() {
        tasks.clear();

        if (completionMessage == null)
            ProgressDialog.this.dispose();

        else {
            messageLabel.setText(completionMessage);

            progressBar.setValue(progressBar.getMaximum());
            closeButton.setText(closeText);
            closePanel.setVisible(true);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            invalidate();
            Dimension d = getSize();
            Dimension p = getPreferredSize();
            d.height = Math.max(d.height, p.height);
            d.width = Math.max(d.width, p.width);
            setSize(d);
        }
    }

    private void invokeOnSwingThread(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception ie) {}
    }

    private void updateProgressBar(final int value, final String msg) {
        invokeOnSwingThread(new Runnable() {
            public void run() {
                progressBar.setValue(value);
                progressBar.setString(msg);
                progressBar.setStringPainted(msg != null);
            }
        });
    }

    private class WorkThread extends Thread implements ChangeListener {
        private Runnable task;
        private int i;

        public void run() {
            running = true;
            for (i = 0;   i < tasks.size();   ++i) {
                if (cancelled)
                    break;

                try {
                    task = (Runnable) tasks.get(i);
                    String msg = null;
                    if (task instanceof Task) {
                        msg = ((Task) task).getMessage();
                        ((Task) task).addChangeListener(this);
                    }
                    updateProgressBar(i * 100, msg);
                    task.run();
                } catch (Throwable t) { }
            }
            updateProgressBar(tasks.size() * 100, null);
            running = false;
            invokeOnSwingThread(new Runnable() {
                public void run() {
                    finished();
                }
            });
        }

        public void stateChanged(ChangeEvent e) {
            if (cancelled && (task instanceof CancellableTask))
                throw new CancelledException();

            try {
                int percent = ((Task) task).getPercentComplete() % 100;
                String msg = ((Task) task).getMessage();

                updateProgressBar(i*100 + percent, msg);
            } catch (Exception ex) {}
        }
    }
}
