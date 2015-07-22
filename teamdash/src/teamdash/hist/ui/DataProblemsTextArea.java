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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.UndoList;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WBSTabPanel;
import teamdash.wbs.columns.ErrorNotesColumn;

public class DataProblemsTextArea extends JTextArea implements FocusListener {

    private WBSTabPanel tabPanel;

    private DataTableModel wbsDataModel;

    private int dataProblemsColumn;

    private String hint;

    private Color hintColor;

    private Font hintFont;

    private Integer currentNodeID;

    public DataProblemsTextArea(WBSTabPanel tabPanel,
            DataTableModel wbsDataModel) {
        super(2, 20);
        this.tabPanel = tabPanel;
        this.wbsDataModel = wbsDataModel;
        this.dataProblemsColumn = wbsDataModel
                .findColumn(ErrorNotesColumn.COLUMN_ID);
        this.hint = BlameHistoryDialog.resources.getString("Data_Problem");
        this.hintColor = Color.gray;
        this.hintFont = getFont().deriveFont(Font.ITALIC);

        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(BorderFactory.createEtchedBorder());
        setAlignmentX(0f);
        setVisible(false);

        addFocusListener(this);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (getText().trim().length() == 0) {
            ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(hintColor);
            g.setFont(hintFont);
            Insets ins = getInsets();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, ins.left, ins.top + fm.getAscent());
        }
    }

    @Override
    public void focusGained(FocusEvent e) {}

    @Override
    public void focusLost(FocusEvent e) {
        maybeSaveDataNotes();
    }

    public void setCurrentNode(Integer nodeID) {
        this.currentNodeID = nodeID;

        String text = null;
        WBSNode wbsNode = getNode(nodeID);
        if (wbsNode != null)
            text = ErrorNotesColumn.getTextAt(wbsNode);
        setText(text);

        setVisible(text != null && text.trim().length() > 0);
    }

    private void maybeSaveDataNotes() {
        WBSNode wbsNode = getNode(currentNodeID);
        if (wbsNode == null)
            return;

        String currentText = ErrorNotesColumn.getTextAt(wbsNode);
        String newText = getText().trim();
        if (newText.equals(nvl(currentText)))
            return;

        wbsDataModel.setValueAt(newText, wbsNode, dataProblemsColumn);
        UndoList.madeChange(tabPanel, "Editing value in 'Data Problems' column");
        int row = wbsDataModel.getWBSModel().getRowForNode(wbsNode);
        if (row != -1) {
            wbsDataModel.getWBSModel().fireTableCellUpdated(row, 0);
            wbsDataModel.fireTableCellUpdated(row, dataProblemsColumn);
        }
    }

    private WBSNode getNode(Integer nodeID) {
        if (nodeID == null)
            return null;
        else
            return wbsDataModel.getWBSModel().getNodeMap().get(nodeID);
    }

    private String nvl(String a) {
        return (a == null ? "" : a.trim());
    }

}
