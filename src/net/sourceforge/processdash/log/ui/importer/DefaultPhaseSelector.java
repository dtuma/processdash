// Copyright (C) 2007-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import org.w3c.dom.Element;

import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.log.defects.DefectPhaseList;
import net.sourceforge.processdash.log.ui.DefectDialog;
import net.sourceforge.processdash.log.ui.DefectPhaseItemRenderer;
import net.sourceforge.processdash.log.ui.MorePhaseOptionsHandler;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

public class DefaultPhaseSelector extends JComboBox {

    private static final String ID_PREFIX = DefaultPhaseSelector.class
            .getName();

    public static final String PHASE_LIST_ID = ID_PREFIX + ".Phase_List";

    public static final String INJ_PHASE_ID = ID_PREFIX + ".Injection_Phase";

    public static final String REM_PHASE_ID = ID_PREFIX + ".Removal_Phase";

    private BoundMap map;

    private String propertyName;

    public DefaultPhaseSelector(BoundMap map_, Element xml) {
        this.map = map_;

        // decide which property we should store our value to
        String tagName = xml.getTagName().toLowerCase();
        boolean isInjection = tagName.contains("inj");
        this.propertyName = (isInjection ? INJ_PHASE_ID : REM_PHASE_ID);

        // add the list of allowed phases to the combo box
        addItem(Defect.UNSPECIFIED_PHASE);
        DefectPhaseList phases = (DefectPhaseList) map.get(PHASE_LIST_ID);
        for (DefectPhase phase : phases)
            addItem(phase);

        // read the current phase from the map, and set the selection
        DefectPhase currentValue = (DefectPhase) map.get(propertyName);
        DefectDialog.phaseComboSelect(this, currentValue);

        // install a renderer that is appropriate for defect phases
        setRenderer(new DefectPhaseItemRenderer());

        // if additional workflows are available, add a "More..." option
        DefectPhaseList workflows = (DefectPhaseList) map
                .get(DefectPhaseMapper.WORKFLOW_PHASE_LIST_ID);
        DefectPhaseList process = (DefectPhaseList) map
                .get(DefectPhaseMapper.PROCESS_PHASE_LIST_ID);
        if (workflows != null && workflows.workflowInfo != null
                && !workflows.workflowInfo.isEmpty())
            new MorePhaseOptionsHandler(this, workflows, process, isInjection);

        // when the user changes the selection, copy their choice to the map
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefectPhase selectedItem = (DefectPhase) getSelectedItem();
                if (selectedItem != MorePhaseOptionsHandler.MORE_OPTIONS)
                    map.put(propertyName, selectedItem);
            }
        });
    }

    @Override
    public void setSelectedItem(Object selectedItem) {
        super.setSelectedItem(selectedItem);
        if (selectedItem != MorePhaseOptionsHandler.MORE_OPTIONS)
            map.put(propertyName, selectedItem);
    }

}
