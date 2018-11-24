// Copyright (C) 2007-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.TemplateFilterLocator;
import net.sourceforge.processdash.tool.diff.engine.DefaultLanguageFilterSelector;
import net.sourceforge.processdash.tool.diff.engine.LanguageFilterSelector;
import net.sourceforge.processdash.ui.Browser;

public class OpenLOCDiffAction extends AbstractAction {

    private static final Resources resources = Resources
            .getDashBundle("LOCDiff.Dialog");

    public OpenLOCDiffAction() {
        super(resources.getString("Menu_Text"));
    }

    public void actionPerformed(ActionEvent e) {
        showDialog();
    }

    public static void showDialog() {
        List panels = TemplateLOCDiffPanelLocator.getPanels();
        List filters = TemplateFilterLocator.getFilters();
        LanguageFilterSelector lfs = new DefaultLanguageFilterSelector(filters);
        new LOCDiffDialog(panels, lfs, SHOW_REPORT);
    }

    private static final ActionListener SHOW_REPORT = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            String filename = e.getActionCommand();
            File reportFile = new File(filename);
            long reportId = LOCDiffReport.storeReport(reportFile);
            String uri = "/dash/pspdiff.class?report=" + reportId;
            Browser.launch(uri);
        }

    };

}
