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

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.*;

public class TaskScheduleChooser
    implements ActionListener, ListSelectionListener
{

    protected static Map openWindows = new Hashtable();

    protected PSPDashboard dash;
    protected JDialog dialog = null;
    protected JList list;
    protected JButton newButton, cancelButton, okayButton;

    TaskScheduleChooser(PSPDashboard dash) {
        String[] templates = EVTaskList.findTaskLists(dash.data);

        if (templates == null || templates.length == 0)
            displayNewTemplateDialog(dash);
        else
            displayChooseTemplateDialog(dash, templates);
    }

    private static final String DEFAULT_MSG =
        "Choose a name for the new task & schedule template:";
    public void displayNewTemplateDialog(PSPDashboard dash) {
        if (dialog != null) dialog.dispose();

        String taskName = "";
        Object message = DEFAULT_MSG;
        while (true) {
            taskName = (String) JOptionPane.showInputDialog
                (null, message, "Create New Schedule",
                 JOptionPane.PLAIN_MESSAGE, null, null, taskName);


            if (taskName == null) return; // user cancelled input

            taskName = taskName.trim();

            if (taskName.length() == 0)
                message = new String[] {
                    "Please enter a name for the new task &",
                    "schedule template, or click cancel:" };

            else if (taskName.indexOf('/') != -1)
                message = new String[] {
                    "The template name cannot contain the '/' character.",
                    DEFAULT_MSG };

            else
                break;
        }

        open(dash, taskName);
    }

    public void displayChooseTemplateDialog(PSPDashboard dash,
                                            String[] taskLists) {
        this.dash = dash;

        dialog = new JDialog();
        dialog.setTitle("Open/Create Task & Schedule");
        dialog.getContentPane().add
            (new JLabel("Choose a task & schedule template:"),
             BorderLayout.NORTH);

        list = new JList(taskLists);
        list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        openSelectedTaskList(); }});
        list.getSelectionModel().addListSelectionListener(this);
        dialog.getContentPane().add(new JScrollPane(list),
                                    BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttons.add(newButton = new JButton("New..."));
        newButton.addActionListener(this);

        buttons.add(cancelButton = new JButton("Cancel"));
        cancelButton.addActionListener(this);

        buttons.add(okayButton = new JButton("Open"));
        okayButton.addActionListener(this);
        okayButton.setEnabled(false);

        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(dialog.DISPOSE_ON_CLOSE);

        dialog.pack();
        dialog.show();
    }

    protected void open(PSPDashboard dash, String taskListName) {
        if (taskListName == null)
            return;
        TaskScheduleDialog d =
            (TaskScheduleDialog) openWindows.get(taskListName);
        if (d != null)
            d.show();
        else
            openWindows.put(taskListName,
                            new TaskScheduleDialog(dash, taskListName));
    }

    static void close(String taskListName) {
        openWindows.remove(taskListName);
    }


    public void valueChanged(ListSelectionEvent e) {
        okayButton.setEnabled(!list.getSelectionModel().isSelectionEmpty());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okayButton)
            openSelectedTaskList();
        else if (e.getSource() == newButton)
            displayNewTemplateDialog(dash);
        else if (e.getSource() == cancelButton)
            dialog.dispose();
    }

    protected void openSelectedTaskList() {
        open(dash, (String) list.getSelectedValue());
        if (dialog != null) dialog.dispose();
    }

    public static void closeAll() {
        Set s = new HashSet(openWindows.values());
        Iterator i = s.iterator();
        while (i.hasNext())
            ((TaskScheduleDialog) i.next()).confirmClose(false);
    }
}
