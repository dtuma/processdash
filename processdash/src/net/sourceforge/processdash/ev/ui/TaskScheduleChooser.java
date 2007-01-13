// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.help.PCSH;

public class TaskScheduleChooser
    implements ActionListener, ListSelectionListener
{

    protected static Map openWindows = new Hashtable();

    protected DashboardContext dash;
    protected JDialog dialog = null;
    protected JList list = null;
    protected JButton newButton, renameButton, deleteButton,
        reportButton, cancelButton, okayButton;
    protected boolean keepDialogOpen = Settings.getBool(KEEP_OPEN, false);

    static Resources resources = Resources.getDashBundle("EV.Chooser");
    private static final String KEEP_OPEN = "taskChooser.keepOpen";

    public TaskScheduleChooser(DashboardContext dash) {
        this(dash, EVTaskList.findTaskLists(dash.getData()));
    }
    public TaskScheduleChooser(DashboardContext dash, String[] templates) {
        if (templates == null || templates.length == 0)
            displayNewTemplateDialog(dash);
        else
            displayChooseTemplateDialog(dash, templates);
    }

    public boolean isDisplayable() {
        return (dialog != null && dialog.isDisplayable());
    }
    public void toFront() {
        if (dialog != null) { dialog.show(); dialog.toFront(); }
    }


    public void displayNewTemplateDialog(DashboardContext dash) {
        if (Settings.isReadOnly()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        this.dash = dash;

        if (dialog != null && !keepDialogOpen) dialog.dispose();

        String taskName = getTemplateName
            (dash, resources.getString("New_Schedule_Window.Title"),
             resources.getString("New_Schedule_Window.Prompt"),
             showRollupOption);

        open(dash, taskName);
    }
    private static boolean showRollupOption =
        Settings.getBool("ev.enableRollup",false);

    private static final String OK = resources.getString("OK");
    private static final String CANCEL = resources.getString("Cancel");
    protected String getTemplateName(Object parent,
                                     String title, String prompt,
                                     boolean showRollupOptions) {

        String taskName = "";
        Object message = prompt, button;
        Object options[];
        JComboBox rollupOption = null;
        if (showRollupOptions) {
            String[] rollupOptions = new String[2];
            rollupOptions[0] = resources.getString("Create_Schedule_Option");
            rollupOptions[1] = resources.getString("Create_Rollup_Option");
            rollupOption = new JComboBox(rollupOptions);
            options = new Object[] {
                rollupOption, new JLabel("      "), OK, CANCEL };
        } else
            options = new Object[] { OK, CANCEL };
        JTextField inputField = new JTextField();

        Component parentComponent = null;
        if (parent instanceof Component)
            parentComponent = (Component) parent;


        while (true) {
            JOptionPane optionPane = new JOptionPane
                (new Object[] { message, inputField },
                 JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                 null, options);
            optionPane.createDialog(parentComponent, title).show();

            button = optionPane.getValue();
            if (button == null || button == CANCEL ||
                button == JOptionPane.UNINITIALIZED_VALUE)
                return null;    // user cancel

            taskName = (String) inputField.getText();

            taskName = taskName.trim();
            String errorMessage = checkNewTemplateName
                (taskName, dash.getData());
            if (errorMessage == null)
                break;
            else
                message = new String[] { errorMessage, prompt };
        }
        if (rollupOption != null && rollupOption.getSelectedIndex() == 1)
            taskName = ROLLUP_PREFIX + taskName;
        return taskName;
    }
    public static final String ROLLUP_PREFIX = " ";

    public static String checkNewTemplateName(String taskName,
                                              DataRepository data) {
        if (taskName == null || taskName.trim().length() == 0)
            return resources.getString("Name_Missing_Message");

        if (taskName.indexOf('/') != -1)
            return resources.getString("Slash_Prohibited_Message");

        if (templateExists(taskName, data))
            return resources.format("Duplicate_Name_Message_FMT", taskName);

        return null;
    }

    private static boolean templateExists(String taskListName,
                                          DataRepository data) {
        String [] taskLists = EVTaskList.findTaskLists(data);
        for (int i = taskLists.length;   i-- > 0; )
            if (taskListName.equals(taskLists[i]))
                return true;
        return false;
    }

    public void displayChooseTemplateDialog(DashboardContext dash,
                                            String[] taskLists) {
        this.dash = dash;

        if (dash instanceof Frame)
            dialog = new JDialog((Frame) dash);
        else
            dialog = new JDialog();
        PCSH.enableHelpKey(dialog, "UsingTaskSchedule.chooser");
        dialog.setTitle(resources.getString("Choose_Window.Title"));

        Box promptBox = Box.createHorizontalBox();
        promptBox.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        promptBox.add(new JLabel(resources.getString("Choose_Window.Prompt")));
        promptBox.add(Box.createHorizontalGlue());
        promptBox.add(new MultiLabel());

        dialog.getContentPane().add(promptBox, BorderLayout.NORTH);

        list = new JList(taskLists);
        list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        if (e.isShiftDown() || e.getButton() == MouseEvent.BUTTON3)
                            showReportForTaskList(e);
                        else
                            openSelectedTaskList();
                    }
                }});
        list.getSelectionModel().addListSelectionListener(this);
        dialog.getContentPane().add(new JScrollPane(list),
                                    BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        newButton = new JButton(resources.getDlgString("New"));
        if (Settings.isReadWrite()) buttons.add(newButton);
        newButton.addActionListener(this);

        renameButton = new JButton(resources.getDlgString("Rename"));
        if (Settings.isReadWrite()) buttons.add(renameButton);
        renameButton.addActionListener(this);
        renameButton.setEnabled(false);

        deleteButton = new JButton(resources.getDlgString("Delete"));
        if (Settings.isReadWrite()) buttons.add(deleteButton);
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(false);

        buttons.add(reportButton = new JButton
            (resources.getString("Buttons.Report")));
        reportButton.addActionListener(this);
        reportButton.setEnabled(false);

        buttons.add(cancelButton = new JButton(resources.getString("Cancel")));
        cancelButton.addActionListener(this);

        buttons.add(okayButton = new JButton(resources.getString("Open")));
        okayButton.addActionListener(this);
        okayButton.setEnabled(false);

        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        dialog.pack();
        dialog.show();
        dialog.toFront();
    }

    public static void open(DashboardContext dash, String taskListName) {
        if (taskListName == null)
            return;

        boolean createRollup = false;
        if (taskListName.startsWith(ROLLUP_PREFIX)) {
            createRollup = true;
            taskListName = taskListName.substring(ROLLUP_PREFIX.length());
        }

        TaskScheduleDialog d =
            (TaskScheduleDialog) openWindows.get(taskListName);
        if (d != null)
            d.show();
        else
            openWindows.put(taskListName,
                            new TaskScheduleDialog(dash, taskListName,
                                                   createRollup));
    }

    static void close(String taskListName) {
        openWindows.remove(taskListName);
    }


    public void valueChanged(ListSelectionEvent e) {
        boolean itemSelected = !list.getSelectionModel().isSelectionEmpty();
        renameButton.setEnabled(itemSelected);
        deleteButton.setEnabled(itemSelected);
        reportButton.setEnabled(itemSelected);
        okayButton.setEnabled(itemSelected);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okayButton)
            openSelectedTaskList();
        else if (e.getSource() == newButton)
            displayNewTemplateDialog(dash);
        else if (e.getSource() == renameButton)
            renameSelectedTaskList();
        else if (e.getSource() == deleteButton)
            deleteSelectedTaskList();
        else if (e.getSource() == reportButton)
            showReportForTaskList(null);
        else if (e.getSource() == cancelButton)
            dialog.dispose();
    }

    protected void openSelectedTaskList() {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName != null)
            try {
                dialog.getContentPane().setCursor(
                        Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                open(dash, taskListName);
                if (dialog != null)
                    if (keepDialogOpen)
                        dialog.getContentPane().setCursor(null);
                    else
                        dialog.dispose();
            } catch (Exception e) {
                dialog.getContentPane().setCursor(null);
                JOptionPane.showMessageDialog(list,
                        resources.getStrings("Unexpected_Error.Message"),
                        resources.getString("Unexpected_Error.Title"),
                        JOptionPane.ERROR_MESSAGE);
            }
    }

    /** Will display the report for the currently selected task list.
     * 
     * If no task list is currently selected, the given mouse event (if
     * non-null) will be checked.  If it occurred over a task list name,
     * the report for that task list will be displayed.
     */
    protected void showReportForTaskList(MouseEvent e) {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName == null && e != null)
            taskListName = getItemAtPoint(list, e.getPoint());
        if (taskListName != null) {
            TaskScheduleDialog.showReport(taskListName);
            if (dialog != null && !keepDialogOpen) dialog.dispose();
        }
    }

    private String getItemAtPoint(JList list, Point point) {
        if (list == null || point == null) return null;
        int pos = list.locationToIndex(point);
        if (pos == -1) return null;
        Rectangle r = list.getCellBounds(pos, pos);
        if (r == null || !r.contains(point)) return null;
        return (String) list.getModel().getElementAt(pos);
    }
    protected void renameSelectedTaskList() {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName == null) return;

        String newName = getTemplateName
            (dialog, resources.getString("Rename_Window.Title"),
             resources.format("Rename_Window.Prompt_FMT", taskListName),
             false);

        if (newName != null) {
            EVTaskList taskList = EVTaskList.openExisting
                (taskListName, dash.getData(), dash.getHierarchy(),
                 dash.getCache(), false);
            if (taskList != null)
                taskList.save(newName);
            refreshList();
            dialog.toFront();
        }
    }

    protected void deleteSelectedTaskList() {
        String taskListName = (String) list.getSelectedValue();
        if (taskListName == null) return;

        String message = resources.format
            ("Delete_Window.Prompt_FMT", taskListName);
        if (JOptionPane.showConfirmDialog
            (dialog, message, resources.getString("Delete_Window.Title"),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.QUESTION_MESSAGE)
            == JOptionPane.YES_OPTION) {
            EVTaskList taskList = EVTaskList.openExisting
                (taskListName, dash.getData(), dash.getHierarchy(),
                 dash.getCache(), false);
            if (taskList != null)
                taskList.save(null);
            refreshList();
            dialog.toFront();
        }
    }

    protected void refreshList() {
        list.setListData(EVTaskList.findTaskLists(dash.getData()));
    }

    public static void closeAll() {
        Set s = new HashSet(openWindows.values());
        Iterator i = s.iterator();
        while (i.hasNext())
            ((TaskScheduleDialog) i.next()).confirmClose(false);
    }

    private class MultiLabel extends JLabel implements MouseListener {
        public MultiLabel() {
            super(new MultiIcon());
            setToolTipText(resources.getString("Keep_Window_Open_Option"));
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            keepDialogOpen = !keepDialogOpen;
            repaint();
            InternalSettings.set(KEEP_OPEN, keepDialogOpen ? "true" : "false");
        }

        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
    }

    private class MultiIcon implements Icon {

        private Color checkColor = Color.black;
        private Color boxColor = Color.gray;
        private Color frameColor = Color.black;
        private Color titleBarColor = Color.blue.darker();
        private Color windowColor = Color.white;

        public int getIconHeight() {
            return 11;
        }

        public int getIconWidth() {
            return 25;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(boxColor);
            g.drawRect(x+0, y+1, 8, 8);
            paintFrame(g, x+12, y);
            paintFrame(g, x+16, y+3);

            if (keepDialogOpen) {
                g.setColor(checkColor);
                for (int i=-1;  i < 4;  i++)
                    g.drawLine(x+3+i, y+6-Math.abs(i), x+3+i, y+7-Math.abs(i));
            }
        }
        private void paintFrame(Graphics g, int x, int y) {
            g.setColor(frameColor);
            g.drawRect(x, y, 8, 7);
            g.setColor(windowColor);
            g.fillRect(x+1, y+1, 7, 6);
            g.setColor(titleBarColor);
            g.drawLine(x+1, y+1, x+7, y+1);
        }

    }
}
