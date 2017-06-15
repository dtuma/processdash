// Copyright (C) 2012-2017 Tuma Solutions, LLC
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManagerWBS;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToComboBox;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.hist.BlameModelData;
import teamdash.wbs.columns.CustomColumn;
import teamdash.wbs.columns.CustomColumnListener;
import teamdash.wbs.columns.CustomTextColumn;
import teamdash.wbs.columns.MilestoneColumn;
import teamdash.wbs.columns.PlanTimeWatcher;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.icons.WBSImageIcon;

public class WBSFilterAction extends AbstractAction
        implements CustomColumnListener {

    private WBSJTable wbsTable;

    private WBSTabPanel tabPanel;

    private TeamActualTimeColumn taskTester;

    private PlanTimeWatcher planTimeWatcher;

    private BlameModelData wbsBlameData;

    private boolean isActive;


    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Filter");

    private static Preferences preferences = Preferences
            .userNodeForPackage(WBSFilterAction.class);

    private static final String INFO_MSG_PREF = "wbsFilterAction.showInfoMessage";


    public WBSFilterAction(WBSJTable wbsTable) {
        super(resources.getString("Menu"), IconFactory.getFilterOffIcon());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, //
            MacGUIUtils.getCtrlModifier() | KeyEvent.SHIFT_MASK));
        this.wbsTable = wbsTable;
    }

    void setWbsTabPanel(WBSTabPanel p) {
        this.tabPanel = p;

        DataTableModel data = p.wbsTable.dataModel;
        int col = data.findColumn(TeamActualTimeColumn.COLUMN_ID);
        this.taskTester = (TeamActualTimeColumn) data.getColumn(col);
        col = data.findColumn(PlanTimeWatcher.COLUMN_ID);
        this.planTimeWatcher = (PlanTimeWatcher) data.getColumn(col);
    }

    public boolean isActive() {
        return isActive;
    }

    public void actionPerformed(ActionEvent e) {
        showFilterFrame();
    }

    public void setBlameData(BlameModelData blameData) {
        // if blame data isn't changing (for example null -> null), do nothing
        if (this.wbsBlameData == blameData)
            return;

        // store the new blame data
        this.wbsBlameData = blameData;

        // if the GUI has been built, update GUI and filtering state
        if (blameFilter != null) {
            // show/hide the blame filter as applicable
            blameLabel.setVisible(blameData != null);
            blameFilter.setVisible(blameData != null);
            dialog.pack();
            // if the blame filter was currently in effect, reapply filters
            if (isActive && blameFilter.isSelected())
                applyAction.actionPerformed(null);
            // if we have no more blame data, turn off the blame filter.
            if (blameData == null)
                blameFilter.setSelected(false);
        }
    }


    private JDialog dialog;

    private JPanel filterPanel;

    private GridBagConstraints lc, vc;

    private TextField nameFilter;

    private GroupField groupFilter;

    private CompletingField labelFilter;

    private CompletingField milestoneFilter;

    private TextField notesFilter;

    private JLabel blameLabel;

    private ChangedItemsField blameFilter;

    private int customColumnFilterPos;

    private List<AbstractFilterField> customColumnFilters;

    private JCheckBox showCompletedTasks;

    private JCheckBox showRelatedTasks;

    private boolean needsShowInfoMessage;


    private void showFilterFrame() {
        if (dialog == null)
            buildGui();

        groupFilter.refreshValues();
        labelFilter.refreshValues();
        milestoneFilter.refreshValues();
        refreshCustomColumnValues();
        dialog.setVisible(true);
        dialog.toFront();
    }

    private void buildGui() {
        JPanel panel = filterPanel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        lc = new GridBagConstraints();
        lc.gridx = lc.gridy = 0;  lc.anchor = GridBagConstraints.WEST;
        lc.gridwidth = 2;  lc.weightx = 1;
        lc.insets = new Insets(0, 0, 0, 40);
        JLabel label = new JLabel(resources.getString("Prompt"));
        panel.add(label);  layout.setConstraints(label, lc);

        GridBagConstraints hc = new GridBagConstraints();
        hc.gridx = 2;  lc.anchor = GridBagConstraints.EAST;
        JButton helpButton = new JButton(new HelpAction());
        helpButton.setBorder(null);
        helpButton.setMargin(new Insets(1, 1, 1, 1));
        helpButton.setFocusable(false);
        helpButton.setContentAreaFilled(false);
        panel.add(helpButton);  layout.setConstraints(helpButton, hc);

        lc.gridwidth = lc.gridy = 1;  lc.anchor = GridBagConstraints.EAST;
        lc.weightx = 0;  lc.insets = new Insets(5, 0, 1, 0);
        label = new JLabel(resources.getString("Items.Name"));
        panel.add(label);  layout.setConstraints(label, lc);

        vc = new GridBagConstraints();
        vc.weightx = vc.gridx = vc.gridy = 1;  vc.gridwidth = 2;
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.insets = new Insets(5, 5, 0, 0);
        nameFilter = new TextField();
        panel.add(nameFilter);  layout.setConstraints(nameFilter, vc);

        lc.gridy++;  vc.gridy++;
        label = new JLabel(resources.getString("Items.Assigned_To"));
        panel.add(label);  layout.setConstraints(label, lc);
        groupFilter = new GroupField();
        panel.add(groupFilter); layout.setConstraints(groupFilter, vc);

        lc.gridy++;  vc.gridy++;
        label = new JLabel(resources.getString("Items.Labels"));
        panel.add(label);  layout.setConstraints(label, lc);
        labelFilter = new CompletingField(TaskLabelColumn.COLUMN_ID, "[ ,]+");
        labelFilter.testAtLeaves = true;
        panel.add(labelFilter);  layout.setConstraints(labelFilter, vc);

        lc.gridy++;  vc.gridy++;
        label = new JLabel(resources.getString("Items.Milestone"));
        panel.add(label);  layout.setConstraints(label, lc);
        milestoneFilter = new CompletingField(MilestoneColumn.COLUMN_ID);
        milestoneFilter.mask = WBSFilterFactory.ENTIRE_VALUE;
        milestoneFilter.nullItemText = resources.getString("Items.Milestone_None");
        milestoneFilter.testAtLeaves = true;
        panel.add(milestoneFilter);  layout.setConstraints(milestoneFilter, vc);

        lc.gridy++;  vc.gridy++;
        label = new JLabel(resources.getString("Items.Notes"));
        panel.add(label);  layout.setConstraints(label, lc);
        notesFilter = new TextField();
        panel.add(notesFilter);  layout.setConstraints(notesFilter, vc);

        lc.gridy++;  vc.gridy++;
        blameLabel = new JLabel(resources.getString("Items.Changes"));
        blameLabel.setVisible(wbsBlameData != null);
        panel.add(blameLabel);  layout.setConstraints(blameLabel, lc);
        blameFilter = new ChangedItemsField();
        blameFilter.setVisible(wbsBlameData != null);
        panel.add(blameFilter);  layout.setConstraints(blameFilter, vc);

        customColumnFilterPos = lc.gridy + 1;
        buildCustomColumns();

        lc.gridy = vc.gridy = 9000;
        showCompletedTasks = new JCheckBox(resources.getString("Show_Completed"), true);
        panel.add(showCompletedTasks);  layout.setConstraints(showCompletedTasks, vc);

        lc.gridy++;  vc.gridy++;
        showRelatedTasks = new JCheckBox(resources.getString("Show_Related"));
        showRelatedTasks.setToolTipText(resources.getString("Show_Related_Tooltip"));
        panel.add(showRelatedTasks);  layout.setConstraints(showRelatedTasks, vc);

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(new JButton(removeAction));
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(new JButton(applyAction));
        buttonBox.add(Box.createHorizontalGlue());

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = lc.gridy + 1;  bc.gridwidth = 3;
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.insets = new Insets(8, 0, 0, 0);
        panel.add(buttonBox);  layout.setConstraints(buttonBox, bc);

        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "removeFilter");
        panel.getActionMap().put("removeFilter", removeAction);

        dialog = new JDialog((Frame) getDialogParent(),
                resources.getString("Title"), false);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setVisible(true);
        dialog.setLocationRelativeTo(getDialogParent());

        needsShowInfoMessage = preferences.getBoolean(INFO_MSG_PREF, true);
    }

    private void buildCustomColumns() {
        customColumnFilters = new ArrayList<AbstractFilterField>();
        for (int i = 0; i < wbsTable.dataModel.getColumnCount(); i++) {
            DataColumn c = wbsTable.dataModel.getColumn(i);
            if (c instanceof CustomColumn)
                columnAdded(c.getColumnID(), c);
        }
        tabPanel.wbsTable.dataModel.getCustomColumnManager()
                .addCustomColumnListener(this);
    }

    private void refreshCustomColumnValues() {
        for (AbstractFilterField f : customColumnFilters) {
            if (f instanceof CompletingField)
                ((CompletingField) f).refreshValues();
        }
    }

    @Override
    public void columnChanged(String id, DataColumn oldColumn,
            DataColumn newColumn) {
        columnDeleted(id, oldColumn);
        columnAdded(id, newColumn);
    }

    @Override
    public void columnDeleted(String id, DataColumn oldColumn) {
        for (AbstractFilterField f : customColumnFilters) {
            if (id.equals(f.column.getColumnID())) {
                customColumnFilters.remove(f);
                filterPanel.remove(f.label);
                filterPanel.remove(f);
                dialog.pack();
                break;
            }
        }
    }

    @Override
    public void columnAdded(String id, DataColumn column) {
        // build an appropriate filter for this type of column
        AbstractFilterField f = null;
        if (column instanceof CustomTextColumn) {
            CustomTextColumn ctc = (CustomTextColumn) column;
            if (ctc.isAutocomplete()) {
                f = new CustomColumnField(id);
            } else {
                f = new TextField(id, "\\s*\\|+\\s*");
            }
            f.testAtLeaves = ctc.isInherits();
        } else {
            // unrecognized custom column type; build no filter
            return;
        }

        // add the filter to the user interface
        GridBagLayout layout = (GridBagLayout) filterPanel.getLayout();
        lc.gridy = vc.gridy = customColumnFilterPos++;
        f.label = new JLabel(column.getColumnName() + ":");
        filterPanel.add(f.label);   layout.setConstraints(f.label, lc);
        filterPanel.add(f);   layout.setConstraints(f, vc);
        if (dialog != null)
            dialog.pack();

        // add the filter to our custom column filter list
        customColumnFilters.add(f);
    }


    private WBSFilter[] createFilters() {
        List<WBSFilter> filters = new ArrayList<WBSFilter>();

        String[] name = nameFilter.getValues();
        if (name != null)
            filters.add(WBSFilterFactory.createNodeNameFilter(name));

        WBSFilter filt = groupFilter.getFilter();
        if (filt != null)
            filters.add(filt);

        filt = labelFilter.createDataColumnFilter();
        if (filt != null)
            filters.add(filt);

        filt = milestoneFilter.createDataColumnFilter();
        if (filt != null)
            filters.add(filt);

        String[] notes = notesFilter.getValues();
        if (notes != null)
            filters.add(WBSFilterFactory.createNoteFilter(notes));

        if (wbsBlameData != null && blameFilter.isSelected())
            filters.add(WBSFilterFactory.createBlameFilter(wbsBlameData));

        for (AbstractFilterField f : customColumnFilters) {
            filt = f.createDataColumnFilter();
            if (filt != null)
                filters.add(filt);
        }

        if (filters.isEmpty())
            return null;
        else
            return filters.toArray(new WBSFilter[filters.size()]);
    }


    private void setFilters(WBSFilter[] filters) {
        // hide the filter dialog, since the user is done with it
        dialog.setVisible(false);

        // stop any editing sessions that are in progress
        wbsTable.stopCellEditing();
        wbsTable.cancelCut();

        // make a note of the currently selected rows/nodes
        WBSModel wbsModel = (WBSModel) wbsTable.getModel();
        int[] selectedRows = wbsTable.getSelectedRows();
        List<WBSNode> selectedNodes = wbsModel.getNodesForRows(selectedRows,
            false);

        // reset the plan time watcher, so it does not fire false alarms after
        // noticing that other team members' times were changed by this filter
        if (planTimeWatcher != null)
            planTimeWatcher.reset();

        // apply the filter to the WBS
        boolean showRelated = showRelatedTasks.isSelected();
        boolean showCompleted = showCompletedTasks.isSelected();
        wbsModel.filterRows(showRelated, showCompleted, taskTester, filters);

        // update the "active" flag of this object, and alter the appearance
        this.isActive = (filters != null || showCompleted == false);
        putValue(Action.SMALL_ICON, isActive
                ? IconFactory.getFilterOnIcon()
                : IconFactory.getFilterOffIcon());
        String res = isActive ? "Active_Tooltip" : "Menu";
        putValue(Action.SHORT_DESCRIPTION, resources.getString(res));

        // Restore the table selection. As a side effect, this will allow the
        // wbsTable to recalculate enablement for editing actions.
        int[] rowsToSelect = wbsModel.getRowsForNodes(selectedNodes);
        wbsTable.selectRows(rowsToSelect, true);

        // possibly display a message to the user so they understand filtering
        if (isActive() && needsShowInfoMessage)
            showInfoMessage(true);
    }


    private void showInfoMessage(boolean applying) {
        String resKey = (applying ? "Info.Applied." : "Info.Help.");
        String title = resources.getString(resKey + "Title");
        Object[] message = new Object[] {
                resources.getStrings(resKey + "Header"), " ",
                resources.getStrings("Info.Message2") };

        JCheckBox doNotShow = new JCheckBox(
                resources.getString("Info.Do_Not_Show"));
        if (applying)
            message = new Object[] { message, " ", doNotShow };

        JOptionPane.showMessageDialog(getDialogParent(), message, title,
            JOptionPane.PLAIN_MESSAGE);

        needsShowInfoMessage = false;
        if (doNotShow.isSelected())
            preferences.putBoolean(INFO_MSG_PREF, false);
    }


    private DataTableModel getDataTableModel() {
        return (DataTableModel) tabPanel.dataTable.getModel();
    }

    private Component getDialogParent() {
        return SwingUtilities.getWindowAncestor(wbsTable);
    }


    private class ApplyAction extends AbstractAction {
        public ApplyAction() {
            super(resources.getString("Apply"));
        }

        public void actionPerformed(ActionEvent e) {
            setFilters(createFilters());
        }
    }

    private ApplyAction applyAction = new ApplyAction();


    private class RemoveAction extends AbstractAction {
        public RemoveAction() {
            super(resources.getString("Remove"));
        }

        public void actionPerformed(ActionEvent e) {
            UserGroupManagerWBS.getInstance().setFilter(null, true);
            showCompletedTasks.setSelected(true);
            setFilters(null);
        }
    }

    private RemoveAction removeAction = new RemoveAction();


    private class HelpAction extends AbstractAction {
        public HelpAction() {
            super(null, IconFactory.getHelpIcon());
            putValue(SHORT_DESCRIPTION, resources.getString("Help"));
        }

        public void actionPerformed(ActionEvent e) {
            showInfoMessage(false);
        }
    }


    /** An component which allows the user to edit a value for a filter
     */
    private abstract class AbstractFilterField extends JPanel implements
            ActionListener {

        /** The label for this field, if it is for a custom column */
        JLabel label;

        /** A button to clear this field */
        private JButton deleteButton;

        /** The data column that this field applies to; may be null */
        DataColumn column;

        /** A regexp for splitting apart tokens in this field value */
        private String split;

        /** True if filter evaluation should occur at leaves only */
        boolean testAtLeaves;

        /** The mask to use for text comparisons */
        int mask = WBSFilterFactory.IGNORE_CASE + WBSFilterFactory.WHOLE_WORDS;

        public AbstractFilterField(String columnID) {
            this(columnID, "\\s*\\|+\\s*");
        }

        public AbstractFilterField(String columnID, String split) {
            super(new BorderLayout());

            if (columnID != null) {
                DataTableModel dtm = getDataTableModel();
                int colPos = dtm.findColumn(columnID);
                if (colPos != -1)
                    column = dtm.getColumn(colPos);
            }
            this.split = split;

            JComponent valueField = makeComponent();

            deleteButton = new JButton(IconFactory.getFilterDeleteIcon());
            deleteButton.addActionListener(this);
            deleteButton.setMargin(new Insets(1, 1, 1, 1));
            deleteButton.setFocusable(false);
            Dimension d = valueField.getPreferredSize();
            d.width = d.height;
            deleteButton.setPreferredSize(d);
            deleteButton.setMaximumSize(d);

            add(valueField, BorderLayout.CENTER);
            add(deleteButton, BorderLayout.EAST);
        }

        public String[] getValues() {
            String value = getValue();
            return (value == null ? null : value.split(split));
        }

        public WBSFilter createDataColumnFilter() {
            String[] values = getValues();
            if (values == null)
                return null;

            WBSFilter f = WBSFilterFactory.createDataColumnFilter(column, mask,
                values);
            if (testAtLeaves)
                f = WBSFilterFactory.createAnd(WBSFilterFactory.IS_LEAF, f);
            return f;
        }

        protected abstract JComponent makeComponent();

        protected abstract String getValue();

        protected abstract void clearValue();

        public void actionPerformed(ActionEvent e) {
            clearValue();
        }
    }


    /** A component which uses a free-text field for editing a filter */
    private class TextField extends AbstractFilterField {

        private JTextField valueField;

        public TextField() {
            super(null);
        }

        public TextField(String columnID, String split) {
            super(columnID, split);
        }

        @Override
        protected JComponent makeComponent() {
            valueField = new JTextField();
            valueField.setAction(applyAction);
            return valueField;
        }

        @Override
        protected void clearValue() {
            valueField.setText("");
        }

        public String getValue() {
            String result = valueField.getText().trim();
            return (result.length() > 0 ? result : null);
        }
    }


    /** A component which uses an autocompleting combo box to edit a filter */
    private class CompletingField extends AbstractFilterField implements
            KeyListener {

        protected String nullItemText, nullItemSelection;

        protected JComboBox valueField;

        protected TableCellEditor cellEditor;

        public CompletingField(String columnID) {
            super(columnID);
        }

        public CompletingField(String columnID, String split) {
            super(columnID, split);
        }

        @Override
        protected JComponent makeComponent() {
            // create the combo box to hold our values.
            valueField = getComboBox();

            // the table-cell-editor component won't have a border.  Give it
            // the same border and preferred size as the plain-text editors
            JComponent e = (JComponent) valueField.getEditor()
                    .getEditorComponent();
            e.addKeyListener(this);
            JTextField tf = new JTextField();
            e.setBorder(tf.getBorder());
            valueField.setPreferredSize(tf.getPreferredSize());
            return valueField;
        }

        protected JComboBox getComboBox() {
            // look up the column in the data table.  Ask it to provide an
            // editing component for us to use.
            cellEditor = ((CustomEditedColumn) column).getCellEditor();
            return (JComboBox) cellEditor.getTableCellEditorComponent(
                tabPanel.dataTable, "", false, 0, -1);
        }

        /** Reload the list of auto-completed values for this editor */
        public void refreshValues() {
            cellEditor.getTableCellEditorComponent(tabPanel.dataTable,
                getValue(), false, 0, -1);
            maybeAddNullItem();
        }

        private void maybeAddNullItem() {
            if (nullItemText != null) {
                nullItemSelection = "\u00AB" + nullItemText + "\u00BB";
                valueField.insertItemAt(nullItemSelection, 0);
            }
        }

        @Override
        protected void clearValue() {
            if (valueField instanceof AssignedToComboBox) {
                ((AssignedToComboBox) valueField).setFullText("");
            } else {
                valueField.setSelectedItem("");
            }
        }

        public String getValue() {
            Object item;
            if (valueField instanceof AssignedToComboBox) {
                item = ((AssignedToComboBox) valueField).getFullText();
            } else {
                item = valueField.getSelectedItem();
            }
            String result = (item == null ? "" : item.toString().trim());
            return (result.length() > 0 ? result : null);
        }

        @Override
        public String[] getValues() {
            String[] result = super.getValues();
            if (result != null && nullItemSelection != null) {
                for (int i = result.length; i-- > 0;) {
                    if (nullItemSelection.equals(result[i]))
                        result[i] = null;
                }
            }
            return result;
        }

        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
                applyAction.actionPerformed(null);
        }

        public void keyReleased(KeyEvent e) {}

        public void keyTyped(KeyEvent e) {}

    }


    /** A component which supports selection of a team member group */
    private class GroupField extends CompletingField {

        public GroupField() {
            super(null);
            refreshValues();
        }

        @Override
        protected JComboBox getComboBox() {
            JComboBox cb = new JComboBox();
            cb.setRenderer(new GroupFieldRenderer());
            return cb;
        }

        @Override
        public void refreshValues() {
            UserGroupManagerWBS mgr = UserGroupManagerWBS.getInstance();
            String selectedID = mgr.getGlobalFilter().getId();
            mgr.reload();

            Vector choices = new Vector();
            choices.addAll(mgr.getGroups().values());
            Collections.sort(choices);

            valueField.removeAllItems();
            valueField.addItem(UserGroup.EVERYONE);
            valueField.setSelectedIndex(0);
            addUserFilter(mgr.getMe(), selectedID);
            for (Object filt : choices)
                addUserFilter((UserFilter) filt, selectedID);
            for (UserFilter filt : mgr.getAllKnownPeople())
                addUserFilter(filt, selectedID);
        }

        private void addUserFilter(UserFilter filt, String selectedID) {
            if (filt != null) {
                valueField.addItem(filt);
                if (filt.getId().equals(selectedID))
                    valueField.setSelectedItem(filt);
            }
        }

        @Override
        protected void clearValue() {
            valueField.setSelectedIndex(0);
        }

        public WBSFilter getFilter() {
            UserGroupManagerWBS.getInstance().setFilter(
                (UserFilter) valueField.getSelectedItem(),
                showRelatedTasks.isSelected());
            return taskTester.getUserWBSNodeFilter();
        }
    }


    private static class GroupFieldRenderer extends DefaultListCellRenderer {
        private Icon everyone, group, individual;
        GroupFieldRenderer() {
            everyone = makeIcon("everyone.png");
            group = makeIcon("group.png");
            individual = makeIcon("individual.png");
        }
        private Icon makeIcon(String name) {
            return new PaddedIcon(new WBSImageIcon(getFont().getSize() + 1, //
                    "/net/sourceforge/processdash/ui/icons/" + name),
                1, 1, 0, 0);
        }
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
            setIcon(getIconForValue(value));
            return this;
        }
        private Icon getIconForValue(Object value) {
            if (value instanceof UserGroupMember)
                return individual;
            else if (value instanceof UserGroup)
                return UserGroup.isEveryone((UserGroup) value)
                        ? everyone : group;
            else
                return null;
        }
    }


    /** A component which displays a predetermined list of choices */
    private abstract class CompletingChoicesField extends CompletingField {

        public CompletingChoicesField() {
            super(null);
        }

        protected JComboBox getComboBox() {
            JComboBox result = new JComboBox(getChoices());
            result.setSelectedIndex(0);
            AutoCompleteDecorator.decorate(result);
            return result;
        }

        protected abstract String[] getChoices();

        protected String[] getChoices(String resourcePrefix,
                String... choiceKeys) {
            String[] items = new String[choiceKeys.length + 1];
            items[0] = "";
            for (int i = 0; i < choiceKeys.length; i++)
                items[i + 1] = resources.getString(resourcePrefix
                        + choiceKeys[i]);
            return items;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                clearValue();
                e.consume();
            } else
                super.keyPressed(e);
        }

    }


    /** A component for editing filter values for a custom column */
    private class CustomColumnField extends CompletingField {

        private boolean multivalued;

        public CustomColumnField(String columnID) {
            super(columnID, "\\s*,\\s*");
            nullItemText = resources.getString("Items.Custom_None");
            multivalued = ((CustomTextColumn) column).isMultivalued();
            if (!multivalued)
                mask = WBSFilterFactory.IGNORE_CASE
                        + WBSFilterFactory.ENTIRE_VALUE;
        }

        @Override
        protected JComboBox getComboBox() {
            cellEditor = ((CustomTextColumn) column).getFilterEditor();
            return (JComboBox) cellEditor.getTableCellEditorComponent(
                tabPanel.dataTable, "", false, 0, -1);
        }

        @Override
        public void refreshValues() {
            super.refreshValues();
            if (!multivalued) {
                // single-valued columns can contain commas in the cell values,
                // but those commas would be misrecognized by the logic in this
                // class that allows multiple filter values. Replace any commas
                // in cell values with an alternative unicode-comma character.
                for (int i = valueField.getItemCount(); i-- > 1;) {
                    String item = (String) valueField.getItemAt(i);
                    if (item.indexOf(',') != -1) {
                        String repl = item.replace(',', COMMA_REPL);
                        valueField.insertItemAt(repl, i);
                        valueField.removeItemAt(i + 1);
                    }
                }
            }
        }

        @Override
        public String[] getValues() {
            String[] result = super.getValues();

            if (result == null) {
                // no filter? return unchanged

            } else if (result.length == 1 && (result[0] == null
                    || WBSFilterFactory.isRegexp(result[0]))) {
                // single "no value" or regexp filter? return unchanged

            } else if (!multivalued) {
                // single-valued column? Remove any comma replacements
                for (int i = result.length; i-- > 0;) {
                    if (result[i] != null)
                        result[i] = result[i].replace(COMMA_REPL, ',');
                }

            } else {
                // multivalued column: build a regexp to look for matches,
                // where we require each token to match a complete value within
                // a multivalued list
                boolean sawNullToken = false;
                StringBuilder pat = new StringBuilder();
                pat.append("~((^|,\\s*)(");
                for (String tok : result) {
                    if (tok == null)
                        sawNullToken = true;
                    else
                        pat.append("\\Q").append(tok).append("\\E|");
                }
                pat.setLength(pat.length() - 1);
                pat.append(")($|,))");
                if (sawNullToken)
                    pat.append("|^$");
                result = new String[] { pat.toString() };
            }

            return result;
        }

        private static final char COMMA_REPL = '\uFE50';

    }


    /** A component which allows selection of changed items only */
    private class ChangedItemsField extends CompletingChoicesField {

        @Override
        protected String[] getChoices() {
            return getChoices("Items.Changes.", "With");
        }

        public boolean isSelected() {
            return valueField.getSelectedIndex() == 1;
        }

        public void setSelected(boolean selected) {
            valueField.setSelectedIndex(selected ? 1 : 0);
        }

    }

}
