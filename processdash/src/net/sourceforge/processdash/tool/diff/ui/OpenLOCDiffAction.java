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
import java.util.List;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.diff.TemplateFilterLocator;

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
        List filters = TemplateFilterLocator.getFilters();
        FileSystemLOCDiffDialog dialog = new FileSystemLOCDiffDialog(filters);
        dialog.setOutputCharset(WebServer.getOutputCharset());
        dialog.showDialog();
    }

}
