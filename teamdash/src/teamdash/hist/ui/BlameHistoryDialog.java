// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

import teamdash.hist.BlameCaretPos;
import teamdash.hist.BlameData;
import teamdash.hist.BlameDataFactory;
import teamdash.hist.BlameModelData;
import teamdash.hist.BlameNodeData;
import teamdash.hist.BlameValueList;
import teamdash.hist.ProjectHistory;
import teamdash.hist.ProjectHistoryException;
import teamdash.hist.ProjectHistoryFactory;
import teamdash.merge.ModelType;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSEditor;
import teamdash.wbs.columns.WBSNodeColumn;

public class BlameHistoryDialog extends JDialog implements
        PropertyChangeListener {

    private WBSEditor wbsEditor;

    private String dataLocation;

    private DataTableModel wbsDataModel;

    private JDateChooser dateChooser;

    private BlameValueTableModel blameChanges;

    private BlameValueTable blameChangeTable;

    private ClearAction clearAction;

    private RejectAction rejectAction;

    private NavigateAction nextAction, previousAction;

    private DataProblemsTextArea dataProblems;

    private ProjectHistory projectHistory;

    private Date historyDate;

    private BlameData blameData;

    protected static final Resources resources = Resources
            .getDashBundle("WBSEditor.Blame");


    public BlameHistoryDialog(WBSEditor wbsEditor, JFrame frame,
            String dataLocation, DataTableModel wbsDataModel) {
        super(frame, resources.getString("Title"), false);
        this.wbsEditor = wbsEditor;
        this.dataLocation = dataLocation;
        this.wbsDataModel = wbsDataModel;

        dateChooser = new JDateChooser();
        dateChooser.getDateEditor().addPropertyChangeListener("date", this);

        blameChanges = new BlameValueTableModel();
        blameChangeTable = new BlameValueTable(blameChanges);
        JScrollPane sp = new JScrollPane(blameChangeTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(375, 200));

        dataProblems = new DataProblemsTextArea(wbsDataModel);
        BoxUtils contentBox = BoxUtils.vbox(sp, dataProblems);

        clearAction = new ClearAction();
        rejectAction = new RejectAction();
        nextAction = new NavigateAction(true);
        previousAction = new NavigateAction(false);
        BoxUtils buttonBox = BoxUtils.hbox(new JButton(previousAction), 5,
            new JButton(rejectAction), 5, new JButton(clearAction), 5,
            new JButton(nextAction));

        JPanel content = new JPanel(new BorderLayout());
        content.add(dateChooser, BorderLayout.NORTH);
        content.add(contentBox, BorderLayout.CENTER);
        content.add(buttonBox, BorderLayout.SOUTH);

        getContentPane().add(content);
        pack();
        setVisible(true);

        addWindowListener(EventHandler.create(WindowListener.class, this,
            "windowEvent"));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ("date".equals(propertyName))
            setHistoryDate((Date) evt.getNewValue());
        else if ("caretPos".equals(propertyName))
            setCaretPos((BlameCaretPos) evt.getNewValue());
    }

    private void setHistoryDate(Date historyDate) {
        if (NullSafeObjectUtils.EQ(historyDate, this.historyDate))
            return;

        BlameCaretPos oldCaretPos = null;
        if (blameData != null) {
            oldCaretPos = blameData.getCaretPos();
            blameData.removePropertyChangeListener("caretPos", this);
            blameData = null;
            setCaretPos(null);
        }

        try {
            if (projectHistory == null)
                projectHistory = ProjectHistoryFactory
                        .getProjectHistory(dataLocation);

            blameData = BlameDataFactory.getBlameData(projectHistory,
                historyDate, wbsDataModel);
            blameData.addPropertyChangeListener("caretPos", this);

            this.historyDate = historyDate;

        } catch (IOException e) {
            blameChanges.showMessage("IO Error!" + e.getMessage());
            e.printStackTrace();
        } catch (ProjectHistoryException e) {
            blameChanges.showMessage("Project Error!" + e.getMessage());
            e.printStackTrace();
        }

        wbsEditor.setBlameData(blameData);
        if (blameData != null && oldCaretPos != null)
            blameData.setCaretPos(oldCaretPos);
        enableNavigationActions();
    }

    private void enableNavigationActions() {
        boolean enabled = blameData != null && !blameData.isEmpty();
        nextAction.setEnabled(enabled);
        previousAction.setEnabled(enabled);
    }

    private void setCaretPos(BlameCaretPos caretPos) {
        boolean enableClearButton = setBlameComments(caretPos);
        clearAction.setEnabled(enableClearButton);
        blameChangeTable.autoResizeColumns();
    }

    private boolean setBlameComments(BlameCaretPos caretPos) {
        blameChanges.clearRows();
        dataProblems.setCurrentNode(null);
        rejectAction.setEnabled(false);
        if (caretPos == null || blameData == null)
            return false;

        if (!caretPos.isSingleCell()) {
            int numChanges = blameData.countAnnotations(caretPos);
            String message = resources.format("Multiple_Selected_FMT",
                numChanges);
            blameChanges.showMessage(message);
            return numChanges > 0;
        }

        ModelType modelType = caretPos.getModelType();
        BlameModelData modelData = blameData.get(modelType);
        if (modelData == null)
            return false;

        Integer nodeID = caretPos.getSingleNode();
        BlameNodeData nodeData = modelData.get(nodeID);
        if (nodeData == null)
            return false;

        String columnID = caretPos.getSingleColumn();
        if (WBSNodeColumn.COLUMN_ID.equals(columnID)) {
            blameChanges.setBlameNodeStructure(nodeData);
            dataProblems.setCurrentNode(nodeID);
            rejectAction.setEnabled(true);
            return nodeData.hasStructuralChange();
        }

        if (nodeData.getAttributes() == null)
            return false;

        for (BlameValueList val : nodeData.getAttributes().values()) {
            if (val.columnMatches(columnID)) {
                blameChanges.setBlameValueList(val);
                dataProblems.setCurrentNode(nodeID);
                rejectAction.setEnabled(true);
                return true;
            }
        }

        return false;
    }

    public void windowEvent() {
        wbsEditor.setBlameData(isVisible() ? blameData : null);
    }

    private class ClearAction extends AbstractAction {

        public ClearAction() {
            super("Clear");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            blameData.clearAnnotations(blameData.getCaretPos());
            blameChanges.clearRows();
            setEnabled(false);
            rejectAction.setEnabled(false);
            dataProblems.setVisible(false);
            enableNavigationActions();
        }

    }

    private class RejectAction extends AbstractAction {
        public RejectAction() {
            super("Reject");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dataProblems.setVisible(true);
            dataProblems.requestFocusInWindow();
        }
    }

    private class NavigateAction extends AbstractAction {

        private boolean searchForward;

        public NavigateAction(boolean searchForward) {
            super(searchForward ? "Next" : "Previous");
            this.searchForward = searchForward;
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (blameData == null)
                return;

            BlameCaretPos currentCaret = blameData.getCaretPos();
            BlameCaretPos newCaret = wbsEditor.findNextAnnotation(currentCaret,
                searchForward);
            if (newCaret == null) {
                enableNavigationActions();
                Toolkit.getDefaultToolkit().beep();
            } else {
                wbsEditor.showHyperlinkedItem(newCaret.getAsHref());
            }
        }
    }

}
