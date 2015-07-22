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

import static net.sourceforge.processdash.ui.lib.BoxUtils.GLUE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.BoxUtils;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.NullSafeObjectUtils;

import teamdash.hist.BlameCaretPos;
import teamdash.hist.BlameData;
import teamdash.hist.BlameDataFactory;
import teamdash.hist.BlameModelData;
import teamdash.hist.BlameNodeData;
import teamdash.hist.BlameValueList;
import teamdash.hist.ProjectHistory;
import teamdash.hist.ProjectHistoryFactory;
import teamdash.merge.ModelType;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.IconFactory;
import teamdash.wbs.WBSEditor;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.WBSNodeColumn;

public class BlameHistoryDialog extends JDialog implements
        PropertyChangeListener {

    private WBSEditor wbsEditor;

    private String dataLocation;

    private DataTableModel wbsDataModel;

    private JDateChooser dateChooser;

    private BlameBreadcrumb breadcrumb;

    private BlameValueTableModel blameChanges;

    private BlameValueTable blameChangeTable;

    private ClearAction clearAction;

    private RejectAction rejectAction;

    private NavigateAction nextAction, previousAction;

    private DataProblemsTextArea dataProblems;

    private ProjectHistory projectHistory;

    private Date historyDate;

    private BlameData blameData;

    private BlameCalculator currentCalculation;


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
        BoxUtils dateBox = BoxUtils.hbox(resources.getString("Date_Prompt"), 5,
            dateChooser, 1);

        breadcrumb = new BlameBreadcrumb();

        blameChanges = new BlameValueTableModel();
        blameChangeTable = new BlameValueTable(blameChanges);
        JScrollPane sp = new JScrollPane(blameChangeTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setAlignmentX(0f);
        sp.setPreferredSize(new Dimension(376, 200));

        dataProblems = new DataProblemsTextArea(wbsDataModel);
        BoxUtils contentBox = BoxUtils.vbox(7, breadcrumb, sp, dataProblems);

        clearAction = new ClearAction();
        rejectAction = new RejectAction();
        nextAction = new NavigateAction(true);
        previousAction = new NavigateAction(false);
        BoxUtils buttonBox = BoxUtils.hbox(GLUE, new JButton(previousAction),
            5, new JButton(rejectAction), 5, new JButton(clearAction), 5,
            new JButton(nextAction), GLUE, new JButton(new CloseAction()), 1);

        JPanel content = new JPanel(new BorderLayout());
        content.add(dateBox, BorderLayout.NORTH);
        content.add(contentBox, BorderLayout.CENTER);
        content.add(buttonBox, BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        showReadyMessage();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().add(content);
        pack();
        setVisible(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible)
            wbsEditor.setBlameData(blameData);
    }

    @Override
    public void dispose() {
        super.dispose();

        wbsEditor.setBlameData(null);

        if (currentCalculation != null) {
            currentCalculation.cancel(true);
            clearBlameData();
            dateChooser.setDate(null);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ("date".equals(propertyName))
            setHistoryDate((Date) evt.getNewValue());
        else if ("caretPos".equals(propertyName))
            setCaretPos((BlameCaretPos) evt.getNewValue());
    }

    private void setHistoryDate(Date historyDate) {
        if (historyDate != null)
            historyDate = DateUtils.truncDate(historyDate);
        if (NullSafeObjectUtils.EQ(historyDate, this.historyDate))
            return;

        clearBlameData();

        if (historyDate != null) {
            showMessage(resources.getString("Message.Wait"));
            new BlameCalculator(historyDate);
        }
    }

    protected void clearBlameData() {
        installBlameData(null, null);
    }

    private void installBlameData(BlameData newBlameData, Date newHistoryDate) {
        if (blameData != null)
            blameData.removePropertyChangeListener("caretPos", this);

        this.blameData = newBlameData;
        this.historyDate = newHistoryDate;

        if (blameData != null)
            blameData.addPropertyChangeListener("caretPos", this);

        if (isVisible())
            wbsEditor.setBlameData(blameData);

        setCaretPos(null);
        enableNavigationActions();
        showReadyMessage();
    }

    protected void showReadyMessage() {
        String resourceKey;
        if (blameData == null)
            resourceKey = "Message.Date";
        else if (blameData.isEmpty())
            resourceKey = "Message.Empty";
        else
            resourceKey = "Message.Ready";
        showMessage(resources.getString(resourceKey));
    }

    private void showMessage(String text) {
        breadcrumb.clear();
        blameChanges.showMessage(text);
        blameChangeTable.autoResizeColumns();
    }

    private void enableNavigationActions() {
        boolean enabled = blameData != null && !blameData.isEmpty();
        if (!enabled) {
            clearAction.setEnabled(false);
            rejectAction.setEnabled(false);
        }
        nextAction.setEnabled(enabled);
        previousAction.setEnabled(enabled);
    }

    private void setCaretPos(BlameCaretPos caretPos) {
        boolean enableClearButton = setBlameComments(caretPos);
        clearAction.setEnabled(enableClearButton);
        blameChangeTable.autoResizeColumns();
    }

    private boolean setBlameComments(BlameCaretPos caretPos) {
        breadcrumb.clear();
        blameChanges.clearRows();
        dataProblems.setCurrentNode(null);
        rejectAction.setEnabled(false);
        if (caretPos == null || blameData == null)
            return false;

        if (!caretPos.isSingleCell()) {
            int numChanges = blameData.countAnnotations(caretPos);
            String message = resources.format("Message.Multiple_FMT",
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

        WBSNode node = wbsDataModel.getWBSModel().getNodeMap().get(nodeID);
        String nodePath = (node == null ? null : node.getFullName());

        String columnID = caretPos.getSingleColumn();
        if (WBSNodeColumn.COLUMN_ID.equals(columnID)) {
            breadcrumb.setPath(nodePath, null);
            blameChanges.setBlameNodeStructure(nodeData);
            dataProblems.setCurrentNode(nodeID);
            rejectAction.setEnabled(true);
            return nodeData.hasStructuralChange();
        }

        if (nodeData.getAttributes() == null)
            return false;

        for (BlameValueList val : nodeData.getAttributes().values()) {
            if (val.columnMatches(columnID)) {
                int col = wbsDataModel.findColumn(columnID);
                String columnName = wbsDataModel.getColumnName(col);
                breadcrumb.setPath(nodePath, columnName);
                blameChanges.setBlameValueList(val);
                dataProblems.setCurrentNode(nodeID);
                rejectAction.setEnabled(true);
                return true;
            }
        }

        return false;
    }


    private class BlameCalculator extends SwingWorker<BlameData, Object> {

        private Date calcDate;

        private BlameCalculator oldCalcToCancel;

        BlameCalculator(Date calcDate) {
            this.calcDate = calcDate;

            oldCalcToCancel = currentCalculation;
            if (oldCalcToCancel != null)
                oldCalcToCancel.cancel(true);

            currentCalculation = this;
            historyDate = calcDate;

            execute();
        }

        @Override
        protected BlameData doInBackground() throws Exception {
            if (oldCalcToCancel != null) {
                try {
                    oldCalcToCancel.get();
                } catch (Exception e) {
                }
                oldCalcToCancel = null;
            }

            if (projectHistory == null)
                projectHistory = ProjectHistoryFactory
                        .getProjectHistory(dataLocation);
            else
                projectHistory.refresh();

            BlameData blameData = BlameDataFactory.getBlameData(projectHistory,
                calcDate, wbsDataModel, this);
            return blameData;
        }

        @Override
        protected void done() {
            if (currentCalculation == this && !isCancelled()) {
                try {
                    installBlameData(get(), calcDate);
                } catch (InterruptedException ie) {
                    // shouldn't happen
                } catch (ExecutionException ee) {
                    clearBlameData();
                    showMessage(ee.getCause().getMessage());
                    ee.printStackTrace();
                }
                currentCalculation = null;
            }
        }

    }


    private class ClearAction extends AbstractAction {

        public ClearAction() {
            putValue(SHORT_DESCRIPTION, resources.getString("Button.Accept"));
            putValue(LARGE_ICON_KEY, IconFactory.getAcceptChangeIcon());
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            blameData.clearAnnotations(blameData.getCaretPos());
            dataProblems.setVisible(false);
            if (blameData.isEmpty()) {
                showReadyMessage();
                enableNavigationActions();
            } else {
                SwingUtilities.invokeLater(nextAction);
            }
        }

    }

    private class RejectAction extends AbstractAction {
        public RejectAction() {
            putValue(SHORT_DESCRIPTION, resources.getString("Button.Reject"));
            putValue(LARGE_ICON_KEY, IconFactory.getRejectChangeIcon());
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dataProblems.setVisible(true);
            dataProblems.requestFocusInWindow();
        }
    }

    private class NavigateAction extends AbstractAction implements Runnable {

        private boolean searchForward;

        public NavigateAction(boolean searchForward) {
            this.searchForward = searchForward;
            putValue(SHORT_DESCRIPTION, resources.getString( //
                    (searchForward ? "Button.Next" : "Button.Previous")));
            putValue(LARGE_ICON_KEY,
                IconFactory.getHorizontalArrowIcon(searchForward));
            setEnabled(false);
        }

        public void run() {
            actionPerformed(null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (blameData == null)
                return;

            BlameCaretPos currentCaret = blameData.getCaretPos();
            BlameCaretPos newCaret = wbsEditor.findNextAnnotation(currentCaret,
                searchForward);
            if (newCaret == null) {
                blameData.clear();
                showReadyMessage();
                enableNavigationActions();
                Toolkit.getDefaultToolkit().beep();
            } else {
                wbsEditor.showHyperlinkedItem(newCaret.getAsHref());
            }
        }
    }

    private class CloseAction extends AbstractAction {

        public CloseAction() {
            super(resources.getString("Button.Close"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }

    }

}
