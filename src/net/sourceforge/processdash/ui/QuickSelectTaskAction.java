// Copyright (C) 2007-2018 Tuma Solutions, LLC
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.ActiveTaskModel;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchicalCompletionStatusCalculator;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.TaskNavigationSelector.QuickSelectTaskProvider;
import net.sourceforge.processdash.ui.lib.JFilterableTreeComponent;
import net.sourceforge.processdash.ui.lib.JFilterableTreeTable;
import net.sourceforge.processdash.ui.lib.JOptionPaneActionHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;

public class QuickSelectTaskAction extends AbstractAction {

    QuickSelectTaskProvider taskProvider;
    ActiveTaskModel activeTaskModel;
    Component parentComponent;

    private static final String SETTING_PREFIX = "navigator.quickSelect.";
    private static final String COLLAPSED_PATHS = ".collapsedPaths";
    private static final String COMPONENT_SIZE = ".size";

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.NavSelector");

    public QuickSelectTaskAction() {
        super(resources.getString("Choose_Task.Menu_Name"),
                DashboardIconFactory.getSearchIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, //
            MacGUIUtils.getCtrlModifier()));
        RuntimeUtils.assertMethod(JTable.class, "setRowSorter");
    }

    @Override
    public void putValue(String key, Object newValue) {
        if (TaskNavigationSelector.TASK_PROVIDER_KEY.equals(key))
            taskProvider = (QuickSelectTaskProvider) newValue;
        else if (TaskNavigationSelector.ACTIVE_TASK_MODEL_KEY.equals(key))
            activeTaskModel = (ActiveTaskModel) newValue;
        else if (TaskNavigationSelector.PARENT_COMPONENT_KEY.equals(key))
            parentComponent = (Component) newValue;
        else
            super.putValue(key, newValue);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            selectTask();
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().beep();
            ex.printStackTrace();
        }
    }

    private void selectTask() {
        if (taskProvider == null || activeTaskModel == null)
            throw new IllegalStateException("Object not yet initialized");

        TreeTableModel tasks = taskProvider.getTaskSelectionChoices();
        final JFilterableTreeComponent selector = new JFilterableTreeComponent(
                tasks, resources.getString("Choose_Task.Find"), false);
        final Object nodeToSelect = taskProvider
                .getTreeNodeForPath(activeTaskModel.getPath());
        loadPrefs(selector);
        selector.setMatchEntirePath(true);
        TaskCompletionRenderer rend = null;
        if (parentComponent instanceof DashboardContext)
            rend = new TaskCompletionRenderer(selector,
                    (DashboardContext) parentComponent);
        new JOptionPaneActionHandler().install(selector);
        Object[] message = new Object[] {
                resources.getString("Choose_Task.Prompt"), selector,
                WindowTracker.dlg(),
                new JOptionPaneTweaker.MakeResizable(),
                new JOptionPaneTweaker.GrabFocus(selector.getFilterTextField()),
                new JOptionPaneTweaker(50) {
                    public void doTweak(JDialog dialog) {
                        if (nodeToSelect != null)
                            selector.setAnchorSelectedNode(nodeToSelect);
                    }}
        };
        int userChoice = JOptionPane.showConfirmDialog(parentComponent, message,
            resources.getString("Choose_Task.Title"),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        savePrefs(selector);
        if (rend != null)
            rend.dispose();
        if (userChoice != JOptionPane.OK_OPTION)
            return;

        Object newTask = selector.getSelectedLeaf();
        if (newTask == null)
            return;

        String newPath = taskProvider.getPathForTreeNode(newTask);
        if (StringUtils.hasValue(newPath))
            activeTaskModel.setPath(newPath);
    }

    private void loadPrefs(final JFilterableTreeComponent selector) {
        loadSize(selector);
        loadCollapsedPaths(selector);
    }

    private void savePrefs(final JFilterableTreeComponent selector) {
        saveSize(selector);
        saveCollapsedPaths(selector);
    }

    private void loadSize(JFilterableTreeComponent selector) {
        String setting = Settings.getVal(getSettingName(COMPONENT_SIZE));
        if (StringUtils.hasValue(setting)) {
            try {
                String[] parts = setting.split(",");
                Dimension d = new Dimension(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]));
                selector.setPreferredSize(d);
            } catch (Exception e) {
            }
        }
    }

    private void saveSize(JFilterableTreeComponent selector) {
        Dimension d = selector.getSize();
        String setting = d.width + "," + d.height;
        InternalSettings.set(getSettingName(COMPONENT_SIZE), setting);
    }

    private void loadCollapsedPaths(JFilterableTreeComponent selector) {
        String setting = Settings.getVal(getSettingName(COLLAPSED_PATHS));
        if (StringUtils.hasValue(setting)) {
            // make a list of the tree nodes that should be collapsed
            Set collapsedNodes = new HashSet();
            for (String path : setting.split("\t"))
                collapsedNodes.add(taskProvider.getTreeNodeForPath(path));

            // collapse those nodes in the tree
            JFilterableTreeTable treeTable = selector.getTreeTable();
            for (int row = 0; row < treeTable.getRowCount(); row++) {
                TreePath path = treeTable.getPathForRow(row);
                Object node = path.getLastPathComponent();
                if (collapsedNodes.contains(node))
                    treeTable.getTree().collapsePath(path);
            }
        }
    }

    private void saveCollapsedPaths(JFilterableTreeComponent selector) {
        JFilterableTreeTable treeTable = selector.getTreeTable();
        List<String> collapsedPaths = new ArrayList();
        for (int row = treeTable.getRowCount(); row-- > 0;) {
            TreePath path = treeTable.getPathForRow(row);
            Object node = path.getLastPathComponent();
            if (treeTable.getTree().getModel().isLeaf(node) == false
                    && treeTable.getTree().isExpanded(path) == false) {
                collapsedPaths.add(taskProvider.getPathForTreeNode(node));
            }
        }
        String setting = (collapsedPaths.isEmpty() ? null : StringUtils.join(
            collapsedPaths, "\t"));
        InternalSettings.set(getSettingName(COLLAPSED_PATHS), setting);
    }

    private String getSettingName(String suffix) {
        return SETTING_PREFIX + taskProvider.getType() + suffix;
    }

    private class TaskCompletionRenderer extends DefaultTreeCellRenderer {

        HierarchicalCompletionStatusCalculator calc;

        Font plainFont, completedFont;

        TaskCompletionRenderer(JFilterableTreeComponent selector,
                DashboardContext ctx) {
            this.calc = new HierarchicalCompletionStatusCalculator(
                    ctx.getData(), ctx.getHierarchy(), PropertyKey.ROOT);
            this.plainFont = selector.getTreeTable().getTree().getFont();
            this.completedFont = plainFont.deriveFont(Collections.singletonMap(
                TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON));

            selector.getTreeTable().getTree().setCellRenderer(this);
        }

        private void dispose() {
            calc.dispose();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            String path = taskProvider.getPathForTreeNode(value);
            setFont(calc.isCompleted(path) ? completedFont : plainFont);
            return super.getTreeCellRendererComponent(tree, value, sel,
                expanded, leaf, row, hasFocus);
        }
    }

}
