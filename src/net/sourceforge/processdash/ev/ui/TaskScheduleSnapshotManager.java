// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.ev.EVMetadata;
import net.sourceforge.processdash.ev.EVSnapshot.Metadata;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class TaskScheduleSnapshotManager extends JPanel {

    private DataContext data;

    private EVTaskList taskList;

    private String activeSnapshotId;

    private boolean dirty;

    private DefaultListModel snapshotModel;

    private JList snapshotList;

    private static final Resources resources = Resources
            .getDashBundle("EV.Manage_Baselines");

    protected TaskScheduleSnapshotManager(DataContext data,
            EVTaskList taskList, List<Metadata> snapshots,
            String activeSnapshotId) {
        this.data = data;
        this.taskList = taskList;
        this.activeSnapshotId = activeSnapshotId;
        this.dirty = false;
        createGui(snapshots);
    }

    public boolean isDirty() {
        return dirty;
    }

    private void createGui(List<Metadata> snapshots) {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(layout);

        int activePos = -1;
        snapshotModel = new DefaultListModel();
        for (Metadata snap : snapshots) {
            if (snap.getId().equals(activeSnapshotId))
                activePos = snapshotModel.getSize();
            snapshotModel.addElement(snap);
        }

        snapshotList = new JList(snapshotModel);
        snapshotList.setCellRenderer(new CellRenderer());
        if (activePos != -1)
            snapshotList.setSelectedIndex(activePos);
        c.gridx = c.gridy = 0;
        c.gridheight = 3;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 10);
        add(new JScrollPane(snapshotList), c);


        c.gridx = c.gridheight = 1;
        c.weighty = c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);
        add(new JButton(new EditAction()), c);

        c.weighty = c.gridy = 1;
        add(new JButton(new DeleteAction()), c);

        c.weighty = 0;
        c.gridy = 2;
        add(new JButton(new SelectAction()), c);
    }

    private void setSelectedBaseline(String snapshotId) {
        if (snapshotId == null || !snapshotId.equals(activeSnapshotId)) {
            taskList.setMetadata(EVMetadata.Baseline.SNAPSHOT_ID, snapshotId);
            dirty = true;
        }
    }

    private class CellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            // use the name of the snapshot as its display value
            Metadata snap = (Metadata) value;
            Component result = super.getListCellRendererComponent(list,
                snap.getName(), index, isSelected, cellHasFocus);

            // place a checkmark icon next to the active snapshot
            if (snap.getId().equals(activeSnapshotId))
                setIcon(DashboardIconFactory.getCheckIcon());
            else
                setIcon(null);

            // display snapshot comments as a tooltip
            String comment = snap.getComment();
            String tooltip = null;
            if (StringUtils.hasValue(comment))
                tooltip = "<html><div style='width:200px'>"
                        + HTMLUtils.escapeEntities(comment) + "</div></html>";
            setToolTipText(tooltip);

            return result;
        }

    }

    private abstract class SnapshotAction extends AbstractAction implements
            ListSelectionListener {

        private boolean multiSelectOK;

        public SnapshotAction(String name, String tipKey, boolean multiSelectOK) {
            super(name);
            putValue(SHORT_DESCRIPTION, resources.getString(tipKey));
            this.multiSelectOK = multiSelectOK;
            snapshotList.addListSelectionListener(this);
            valueChanged(null);
        }

        public void valueChanged(ListSelectionEvent e) {
            int selCount = snapshotList.getSelectedIndices().length;
            if (multiSelectOK)
                setEnabled(selCount > 0);
            else
                setEnabled(selCount == 1);
        }

    }

    private class EditAction extends SnapshotAction {
        public EditAction() {
            super(resources.getDlgString("Edit"), "Edit.Tooltip", false);
        }

        public void actionPerformed(ActionEvent e) {
            // find the snapshot that the user wants to edit
            int selIdx = snapshotList.getSelectedIndex();
            if (selIdx == -1)
                return;
            Metadata snap = (Metadata) snapshotModel.get(selIdx);

            // prompt the user for the desired name/description
            String[] userValues = showSnapEditDialog(
                TaskScheduleSnapshotManager.this,
                resources.getString("Edit.Title"), //
                snap.getName(), snap.getComment());
            if (userValues == null)
                return;

            // retrieve the edited values and save the changes
            String newName = userValues[0];
            if (newName.length() > 0)
                snap.setName(newName);
            snap.setComment(userValues[1]);
            snap.save(data);

            // alert the list model so it can redraw the given item
            snapshotModel.set(selIdx, snap);
        }
    }

    private class DeleteAction extends SnapshotAction {

        public DeleteAction() {
            super(resources.getDlgString("Delete"), "Delete.Tooltip", true);
        }

        public void actionPerformed(ActionEvent e) {
            if (JOptionPane.showConfirmDialog(TaskScheduleSnapshotManager.this,
                resources.getString("Delete.Confirm_Prompt"),
                resources.getString("Delete.Confirm_Title"),
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;

            int[] sel = snapshotList.getSelectedIndices();
            for (int i = sel.length; i-- > 0;) {
                int pos = sel[i];
                Metadata snap = (Metadata) snapshotModel.elementAt(pos);
                if (snap.getId().equals(activeSnapshotId))
                    setSelectedBaseline(null);
                snap.delete(data);
                snapshotModel.remove(pos);
            }
        }
    }

    private class SelectAction extends SnapshotAction {
        public SelectAction() {
            super(resources.getString("Select.Button"), "Select.Tooltip", false);
        }

        public void actionPerformed(ActionEvent e) {
            Metadata snap = (Metadata) snapshotList.getSelectedValue();
            if (snap != null) {
                setSelectedBaseline(snap.getId());
                SwingUtilities.getWindowAncestor(
                    TaskScheduleSnapshotManager.this).dispose();
            }
        }
    }

    public static String[] showSnapEditDialog(Component parent, String title,
            String name, String comment) {
        // create a set of fields allowing them to edit the name/comment
        JTextField snapshotName = new JTextField(name);
        snapshotName.selectAll();
        JTextArea snapshotComment = new JTextArea(comment, 3, 25);
        snapshotComment.setFont(UIManager.getFont("Table.font"));
        snapshotComment.setWrapStyleWord(true);
        snapshotComment.setLineWrap(true);
        Object message = new Object[] {
                resources.getString("Save_Baseline.Save_Dialog.Name_Prompt"),
                snapshotName,
                resources.getString("Save_Baseline.Save_Dialog.Comment_Prompt"),
                new JScrollPane(snapshotComment),
                new JOptionPaneTweaker.GrabFocus(snapshotName) };

        // display a dialog inviting the user to edit the values
        if (JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;

        return new String[] { snapshotName.getText().trim(),
                snapshotComment.getText().trim() };
    }

}
