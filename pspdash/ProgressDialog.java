// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;


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

    private Vector tasks = new Vector();
    private JLabel messageLabel = null;
    private JProgressBar progressBar = null;
    private String completionMessage = null;

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
        if (message == null) message = "Please wait...";
        messageLabel = new JLabel(message);
        progressBar = new JProgressBar();
        getContentPane().add(messageLabel, BorderLayout.NORTH);
        getContentPane().add(progressBar,  BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(parent);
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

    /** Displays the dialog, and runs the tasks in the order they were
     *  added.
     */
    public void run() {
        progressBar.setMaximum(tasks.size() * 100);
        WorkThread w = new WorkThread();
        w.start();
        show();         // this will block until the work thread finishes
    }


    public void finished() {
        if (completionMessage == null)
            ProgressDialog.this.dispose();

        else {
            messageLabel.setText(completionMessage);

            getContentPane().remove(progressBar);
            JButton closeButton = new JButton("Close");
            getContentPane().add(closeButton, BorderLayout.SOUTH);
            closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ProgressDialog.this.dispose(); }} );

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            invalidate();
            pack();
        }
    }

    private class WorkThread extends Thread implements ChangeListener {
        private Runnable task;
        private int i;

        public void run() {
            for (i = 0;   i < tasks.size();   progressBar.setValue(++i*100))
                try {
                    task = (Runnable) tasks.get(i);
                    if (task instanceof Task) {
                        String msg = ((Task) task).getMessage();
                        progressBar.setString(msg);
                        progressBar.setStringPainted(msg != null);

                        ((Task) task).addChangeListener(this);
                    }
                    task.run();
                } catch (Throwable t) { }
            finished();
        }

        public void stateChanged(ChangeEvent e) {
            try {
                int percent = ((Task) task).getPercentComplete() % 100;
                progressBar.setValue(i*100 + percent);

                String msg = ((Task) task).getMessage();
                progressBar.setStringPainted(msg != null);
                progressBar.setString(msg);
            } catch (Exception ex) {}
        }
    }
}
