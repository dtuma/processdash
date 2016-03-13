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

package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.Border;

import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

public class DefectPhaseItemRenderer extends DefaultListCellRenderer {

    private JPanel panel;

    private JSeparator separator;

    private JLabel label;

    private Border border;

    private String moreItemText;

    private boolean hasMultipleProcesses;

    int lastScannedSize = -1;


    public DefectPhaseItemRenderer() {
        panel = new JPanel(new BorderLayout());
        panel.add(separator = new JSeparator(), BorderLayout.NORTH);
        panel.add(label = new JLabel(), BorderLayout.CENTER);
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.PLAIN, font.getSize2D() * 0.8f));
        border = BorderFactory.createEmptyBorder(1, 15, 1, 1);
        moreItemText = DefectDialog.resources.getString("More_Options.List_Item");
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        checkForMultipleWorkflows(list);
        Object display = (value == MorePhaseOptionsHandler.MORE_OPTIONS
                ? moreItemText : value);
        super.getListCellRendererComponent(list, display, index, isSelected,
            cellHasFocus);
        if (index == -1 || hasMultipleProcesses == false)
            return this;

        DefectPhase phase = (DefectPhase) value;

        boolean isFirstPhase = isFirstPhase(list, phase, index);
        separator.setVisible(index > 0 && isFirstPhase);

        label.setText(phase.processName);
        label.setVisible(isFirstPhase);

        if (StringUtils.hasValue(phase.processName))
            setBorder(border);
        panel.add(this, BorderLayout.SOUTH);

        return panel;
    }

    private void checkForMultipleWorkflows(JList list) {
        int numItems = list.getModel().getSize();
        if (numItems == lastScannedSize)
            return;

        hasMultipleProcesses = false;
        String processNameFound = null;
        for (int i = numItems; i-- > 0;) {
            DefectPhase p = (DefectPhase) list.getModel().getElementAt(i);
            String oneName = p.processName;
            if (oneName != null) {
                if (processNameFound == null)
                    processNameFound = oneName;
                else if (!processNameFound.equals(oneName))
                    hasMultipleProcesses = true;
            }
        }

        lastScannedSize = numItems;
    }

    private boolean isFirstPhase(JList list, DefectPhase phase, int index) {
        if (index < 1)
            return true;

        DefectPhase prev = (DefectPhase) list.getModel().getElementAt(index - 1);
        return !NullSafeObjectUtils.EQ(phase.processName, prev.processName);
    }

}