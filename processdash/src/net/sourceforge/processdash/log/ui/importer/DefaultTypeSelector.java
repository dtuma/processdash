// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui.importer;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.OptionList;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

import org.w3c.dom.Element;

public class DefaultTypeSelector extends JPanel {

    public static final String TYPE_ID = DefaultTypeSelector.class.getName()
            + ".Default_Type";

    private DefectImportForm form;

    private JComboBox comboBox;

    public DefaultTypeSelector(BoundMap map, Element xml) {
        this.form = (DefectImportForm) map;

        DefectTypeStandard dts = DefectTypeStandard.get(form.getSelectedPath(),
                form.getDashContext().getData());
        OptionList ol = new OptionList(dts);

        ol.options.insertElementAt(Defect.UNSPECIFIED, 0);

        if (ol.translations != null)
            ol.translations.put(Defect.UNSPECIFIED, Translator
                    .translate(Defect.UNSPECIFIED));

        if (ol.comments != null)
            ol.comments.put(Defect.UNSPECIFIED, DefectImportForm.resources
                    .getString("Unspecified_Type_Comment"));

        comboBox = ol.getAsComboBox();
        comboBox.setSelectedIndex(0);
        comboBox.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "updateMapFromValue"));
        updateMapFromValue();

        setLayout(new BorderLayout());
        add(comboBox);
    }

    public void updateMapFromValue() {
        Object value = comboBox.getSelectedItem();
        form.put(TYPE_ID, value);
    }

}
