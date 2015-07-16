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
import javax.swing.JTextArea;

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
import teamdash.wbs.WBSEditor;
import teamdash.wbs.columns.WBSNodeColumn;

public class BlameHistoryDialog extends JDialog implements
        PropertyChangeListener {

    private WBSEditor wbsEditor;

    private String dataLocation;

    private JDateChooser dateChooser;

    private JTextArea blameChanges;

    private ClearAction clearAction;

    private NavigateAction nextAction, previousAction;

    private ProjectHistory projectHistory;

    private Date historyDate;

    private BlameData blameData;

    protected static final Resources resources = Resources
            .getDashBundle("WBSEditor.Blame");


    public BlameHistoryDialog(WBSEditor wbsEditor, JFrame frame,
            String dataLocation) {
        super(frame, resources.getString("Title"), false);
        this.wbsEditor = wbsEditor;
        this.dataLocation = dataLocation;

        dateChooser = new JDateChooser();
        dateChooser.getDateEditor().addPropertyChangeListener("date", this);

        blameChanges = new JTextArea(5, 20);
        blameChanges.setEditable(false);
        blameChanges.setLineWrap(true);
        blameChanges.setWrapStyleWord(true);

        clearAction = new ClearAction();
        nextAction = new NavigateAction(true);
        previousAction = new NavigateAction(false);
        BoxUtils buttonBox = BoxUtils.hbox(new JButton(previousAction), 5,
            new JButton(clearAction), 5, new JButton(nextAction));

        JPanel content = new JPanel(new BorderLayout());
        content.add(dateChooser, BorderLayout.NORTH);
        content.add(new JScrollPane(blameChanges), BorderLayout.CENTER);
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
                historyDate);
            blameData.addPropertyChangeListener("caretPos", this);

            this.historyDate = historyDate;

        } catch (IOException e) {
            blameChanges.setText("IO Error!" + e.getMessage());
            e.printStackTrace();
        } catch (ProjectHistoryException e) {
            blameChanges.setText("Project Error!" + e.getMessage());
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
        String blameComments = getBlameComments(caretPos);
        blameChanges.setText(blameComments);
        clearAction.setEnabled(caretPos != null && blameComments != null);
    }

    private String getBlameComments(BlameCaretPos caretPos) {
        if (caretPos == null || blameData == null)
            return null;

        if (!caretPos.isSingleCell())
            return "Selected " + blameData.countAnnotations(caretPos)
                    + " changes";

        ModelType modelType = caretPos.getModelType();
        BlameModelData modelData = blameData.get(modelType);
        if (modelData == null)
            return null;

        Integer nodeID = caretPos.getSingleNode();
        BlameNodeData nodeData = modelData.get(nodeID);
        if (nodeData == null)
            return null;

        String columnID = caretPos.getSingleColumn();
        if (WBSNodeColumn.COLUMN_ID.equals(columnID))
            return nodeData.toString();

        if (nodeData.getAttributes() == null)
            return null;

        for (BlameValueList val : nodeData.getAttributes().values()) {
            if (val.columnMatches(columnID))
                return val.getColumn().getColumnName() + ": " + val;
        }

        return null;
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
            blameChanges.setText(null);
            setEnabled(false);
            enableNavigationActions();
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
