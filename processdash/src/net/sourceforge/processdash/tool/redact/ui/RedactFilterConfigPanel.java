// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.redact.RedactFilterer;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.util.XMLUtils;

public class RedactFilterConfigPanel extends JPanel {

    private GuiPrefs guiPrefs;

    private Set<String> chosenFilters;


    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Redact");

    public RedactFilterConfigPanel() {
        this(RedactFilterer.class);
    }

    public RedactFilterConfigPanel(Object... guiPrefsPath) {
        guiPrefs = new GuiPrefs(guiPrefsPath);
        chosenFilters = new HashSet();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (FilterCheckbox fcb : makeFilterCheckboxes())
            add(fcb);
    }

    public Set<String> getChosenFilters() {
        guiPrefs.saveAll();
        return Collections.unmodifiableSet(chosenFilters);
    }

    private List<FilterCheckbox> makeFilterCheckboxes() {
        List<FilterCheckbox> options = new ArrayList();

        List<Element> xmlCfg = ExtensionManager
                .getXmlConfigurationElements("redact-filter-set");
        for (Element xml : xmlCfg) {
            try {
                options.add(new FilterCheckbox(xml, guiPrefs));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Collections.sort(options);
        return options;
    }

    private class FilterCheckbox extends JCheckBox implements ActionListener,
            Comparable<FilterCheckbox> {

        private String id;

        private int ordinal;

        private FilterCheckbox(Element xml, GuiPrefs guiPrefs) {
            this.id = xml.getAttribute("id");

            this.ordinal = XMLUtils.getXMLInt(xml, "ordinal");
            if (this.ordinal == -1)
                this.ordinal = 999;

            String resBase = xml.getAttribute("resources");
            if (XMLUtils.hasValue(resBase)) {
                Resources res = Resources.getDashBundle(resBase);
                setText(res.getString("Display"));
            } else {
                setText(resources.getString(id + ".Display"));
            }

            guiPrefs.load(id, this);
            addActionListener(this);
            updateSelection();
        }

        public void actionPerformed(ActionEvent e) {
            updateSelection();
        }

        private void updateSelection() {
            if (isSelected())
                chosenFilters.add(id);
            else
                chosenFilters.remove(id);
        }

        public int compareTo(FilterCheckbox that) {
            int result = this.ordinal - that.ordinal;
            if (result != 0)
                return result;
            else
                return this.id.compareTo(that.id);
        }

    }

}
