// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.hier.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.ui.lib.JHintTextField;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

public class AddTaskDialog {

    private ProcessDashboard dash;

    private PropertyKey activeTask;

    private PropertyKey previousSibling;

    private PropertyKey targetParent;

    private AddTaskHandler handler;

    private JLabel taskTypeIcon;

    private AddTaskTypeOption taskType;

    private JHintTextField taskName;

    private static final String EXT_DATA_NAME = "Add_Task_Handler";

    static final Resources resources = Resources
            .getDashBundle("HierarchyEditor.AddTask");


    public AddTaskDialog(ProcessDashboard dash) {
        // ensure the hierarchy editor is not open before proceeding
        if (dash.isHierarchyEditorOpen()) {
            JOptionPane.showMessageDialog(dash,
                resources.getStrings("Close_Hierarchy_Editor.Message"),
                resources.getString("Close_Hierarchy_Editor.Title"),
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.dash = dash;

        // determine the appropriate location to create the new task, and the
        // handler that will manage the operation
        String handlerId = identifyTargetLocationAndHandler();
        handler = getHandler(handlerId);
        if (handler == null)
            return;

        // create user interface components for customizing the new task
        String title = resources.getString("Window_Title");
        JLabel parentLabel = makeParentLabel();
        Component newTaskComponentRow = makeNewTaskComponentRow();
        Object message = new Object[] { parentLabel, newTaskComponentRow,
                new JOptionPaneTweaker.GrabFocus(taskName),
                new JOptionPaneTweaker.MakeResizable() };

        while (true) {
            // display a dialog prompting the user for information
            int userChoice = JOptionPane.showConfirmDialog(dash, message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            // if the user canceled, abort
            if (userChoice != JOptionPane.OK_OPTION)
                return;

            // validate the inputs. If any were bad, display an error message
            // and then redisplay the user interface. If the inputs were valid
            // and the task was created, return.
            if (validateInputsAndPerformTaskAddition())
                return;
        }
    }

    private String identifyTargetLocationAndHandler() {
        activeTask = previousSibling = dash.getCurrentPhase();
        targetParent = activeTask.getParent();

        StringBuffer path = new StringBuffer(activeTask.path());
        while (true) {
            SaveableData sd = dash.getData().getInheritableValue(path,
                EXT_DATA_NAME);
            String handler = (sd == null ? null : sd.getSimpleValue().format());
            if (!"none".equals(handler))
                return handler;

            previousSibling = dash.getHierarchy().findExistingKey(
                path.toString());
            targetParent = previousSibling.getParent();
            path.setLength(targetParent.path().length());
        }
    }

    private AddTaskHandler getHandler(String handlerId) {
        if (handlerId == null)
            return new AddTaskDefaultHandler();

        for (Element e : ExtensionManager
                .getXmlConfigurationElements("addTaskHandler")) {
            if (handlerId.equals(e.getAttribute("id"))) {
                try {
                    return (AddTaskHandler) ExtensionManager
                            .getExecutableExtension(e, "class", dash);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    private JLabel makeParentLabel() {
        String parentPath = StringUtils.findAndReplace(targetParent.path()
                .substring(1), "/", " / ");
        JLabel parentLabel = new JLabel(parentPath,
                DashboardIconFactory.getProjectIcon(), SwingConstants.LEFT);
        parentLabel.setToolTipText(resources.getString("Parent_Tooltip"));
        return parentLabel;
    }

    private Component makeNewTaskComponentRow() {
        BoxUtils result = BoxUtils.hbox();

        // create an icon to suggest the parent-child relationship
        JLabel childIcon = new JLabel(new ChildIcon());
        childIcon.setAlignmentY(0.5f);
        result.addItem(childIcon);

        // create components for selecting and displaying the task type
        result.addItem(createTaskTypeSelector());
        taskTypeIcon.setAlignmentY(0.5f);
        result.addItem(taskTypeIcon);
        result.addItem(4);

        // create a text field for entering the new task name
        taskName = new JHintTextField(resources.getString("Task_Name_Hint"));
        taskName.setDocument(new NodeNameDocument());
        taskName.setAlignmentY(0.5f);
        result.addItem(taskName);

        return result;
    }


    private Component createTaskTypeSelector() {
        List<AddTaskTypeOption> taskTypes = handler.getTaskTypes(
            targetParent.path(), activeTask.path());
        taskTypeIcon = new JLabel();

        if (taskTypes.size() == 1) {
            setSelectedTaskType(taskTypes.get(0));
            return null;

        } else {
            JMenu typeMenu = makeTaskTypeMenu(taskTypes);
            new MouseHandler(typeMenu, taskTypeIcon);

            JMenuBar menuBar = new JMenuBar();
            menuBar.setMinimumSize(new Dimension(0, 0));
            menuBar.setPreferredSize(new Dimension(0, 1));
            menuBar.setMaximumSize(new Dimension(0, 100));
            menuBar.add(typeMenu);

            if (this.taskType == null)
                setSelectedTaskType(taskTypes.get(0));

            return menuBar;
        }
    }

    private JMenu makeTaskTypeMenu(List<AddTaskTypeOption> taskTypes) {
        JMenu typeMenu = new JMenu();

        int maxItemsPerMenu = Settings.getInt("hierarchyMenu.maxItems", 20);
        JMenu destMenu = typeMenu;

        for (AddTaskTypeOption type : taskTypes) {
            if (destMenu.getItemCount() + 1 >= maxItemsPerMenu) {
                JMenu moreMenu = new JMenu(Resources.getGlobalBundle()
                        .getDlgString("More"));
                destMenu.insert(moreMenu, 0);
                destMenu.insertSeparator(1);
                destMenu = moreMenu;
            }

            destMenu.add(new TypeMenuOption(type));
        }

        return typeMenu;
    }

    private void setSelectedTaskType(AddTaskTypeOption type) {
        taskType = type;
        taskTypeIcon.setIcon(taskType.icon);
        taskTypeIcon.setToolTipText(taskType.displayName);
    }

    private boolean validateInputsAndPerformTaskAddition() {
        // make certain they entered a task name. If not, display an error.
        String name = taskName.getText().trim();
        if (name.length() == 0) {
            displayError(resources.getString("Errors.Name_Missing"));
            return false;
        }

        // See if another task already exists with that name.
        PropertyKey newNode = new PropertyKey(targetParent, name);
        DashHierarchy hier = dash.getHierarchy();
        if (hier.containsKey(newNode)) {
            displayError(resources.format("Errors.Duplicate_Name_FMT", name,
                targetParent.path()));
            return false;
        }

        // produce a list of child names in our desired order, to indicate the
        // place where the new node should be inserted.
        List childOrder = new ArrayList();
        int numChildren = hier.getNumChildren(targetParent);
        for (int i = 0; i < numChildren; i++) {
            PropertyKey child = hier.getChildKey(targetParent, i);
            childOrder.add(child.name());
            if (child.equals(previousSibling))
                childOrder.add(name);
        }
        if (childOrder.size() == numChildren)
            childOrder = null;

        try {
            // add the new task.
            HierarchyAlterer alt = DashController.getHierarchyAlterer();
            String newTaskPath = newNode.path();
            if (taskType.templateID == null) {
                alt.addNode(newTaskPath);
            } else {
                alt.addTemplate(newTaskPath, taskType.templateID);
            }

            // the new task will initially be the last child of this parent.
            // proactively mark it as the selected child.
            hier.setSelectedChild(targetParent, numChildren);

            // now reorder the children of the target parent, so the new child
            // is in the correct position
            if (childOrder != null)
                alt.reorderChildren(targetParent.path(), childOrder);

            // let the handler perform any follow-up work
            handler.finalizeAddedTask(newTaskPath, taskType);

            // set the new task as the active task.
            deferredSetActiveTask(newNode);
        } catch (HierarchyAlterationException e) {
            e.printStackTrace();
        }

        return true;
    }

    private void displayError(Object message) {
        String title = resources.getString("Errors.Name_Title");
        JOptionPane.showMessageDialog(dash, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private void deferredSetActiveTask(final PropertyKey newNode) {
        // our hierarchy alterations will trigger a "hierarchy changed" event,
        // but that event won't fire until 300 ms after the changes occurred.
        // wait a little longer, then make the new task the active one.
        Timer t = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dash.getActiveTaskModel().setNode(newNode);
            }
        });
        t.setRepeats(false);
        t.start();
    }


    /**
     * Icon to draw a line suggesting that one item is a child of another
     */
    private static class ChildIcon implements Icon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine(8, 0, 8, 8);
            g.drawLine(8, 8, 20, 8);
        }

        public int getIconWidth() {
            return 24;
        }

        public int getIconHeight() {
            return 16;
        }

    }


    /**
     * This logic is copied from logic in the WBS Editor project; specifically,
     * WBSNodeEditor and WBSClipSelection.
     */
    private static class NodeNameDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null)
                return;

            // normalize whitespace characters
            str = str.replace('\t', ' ');
            str = str.replace('\n', ' ');
            str = str.replace('\r', ' ');

            // replace common extended characters found in Office documents
            for (int i = 0; i < CHARACTER_REPLACEMENTS.length; i++) {
                String repl = CHARACTER_REPLACEMENTS[i];
                for (int c = 1; c < repl.length(); c++)
                    str = str.replace(repl.charAt(c), repl.charAt(0));
            }
            // disallow slash characters
            str = str.replace('/', ',');
            // perform round-trip through default platform encoding
            str = new String(str.getBytes());

            super.insertString(offs, str, a);
        }
    }

    private static final String[] CHARACTER_REPLACEMENTS = { //
        "\"\u201C\u201D", // opening and closing double quotes
        "'\u2018\u2019", // opening and closing single quotes
        "-\u2013\u2014", // Em-dash and En-dash
        " \u00A0\u2002\u2003" // nonbreaking space, em-space, en-space
    };

    private class TypeMenuOption extends AbstractAction {
        AddTaskTypeOption type;

        public TypeMenuOption(AddTaskTypeOption type) {
            super(type.displayName, type.icon);
            this.type = type;
            if (type.isDefault)
                actionPerformed(null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setSelectedTaskType(type);
        }
    }

    private class MouseHandler extends MouseAdapter implements Runnable {
        
        private JMenu menu;
        
        MouseHandler(JMenu menu, JComponent trigger) {
            this.menu = menu;
            trigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            trigger.addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            SwingUtilities.invokeLater(this);
        }

        public void run() {
            menu.doClick();
        }

    }

}
