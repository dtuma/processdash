// Copyright (C) 2023 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowDataModel;


public class WorkflowScriptCellEditor extends DefaultCellEditor {

    private JTable parentTable;

    private String currentValue;

    private WorkflowScriptListEditor editor;

    private String workflowName, taskName;


    public WorkflowScriptCellEditor() {
        super(new JCheckBox());

        JButton button = new JButton();
        button.setBackground(Color.white);
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editor.setValue(currentValue);
                if (editor.showDialog(parentTable, workflowName, taskName)) {
                    currentValue = editor.getValue();
                    fireEditingStopped();
                } else {
                    fireEditingCanceled();
                }

                if (parentTable != null)
                    parentTable.requestFocusInWindow();
            }
        });

        editorComponent = button;
        setClickCountToStart(2);
    }


    public Object getCellEditorValue() {
        return currentValue;
    }


    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        parentTable = table;
        currentValue = (String) value;
        ((JButton) editorComponent).setText(currentValue);

        if (editor == null)
            editor = new WorkflowScriptListEditor();

        WorkflowDataModel dataModel = (WorkflowDataModel) table.getModel();
        WBSModel model = dataModel.getWBSModel();
        WBSNode node = model.getNodeForRow(row);
        if (node != null) {
            workflowName = model.getBaseParent(node).getName();
            taskName = (node.getIndentLevel() > 1 ? node.getName() : null);
        }

        return editorComponent;
    }

}
