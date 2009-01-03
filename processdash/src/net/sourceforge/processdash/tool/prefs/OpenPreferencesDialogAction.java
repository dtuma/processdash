// Copyright (C) 2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.prefs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;

public class OpenPreferencesDialogAction extends AbstractAction {
    private static final Resources resources = Resources.getDashBundle("Tools.Prefs.Dialog");

    private static final String MENU_TEXT_RESOURCE = "Menu_Text";
    private static final String DIALOG_TITLE_RESOURCE = "Window_Title";

    // The Dashboard instance
    private ProcessDashboard parent;

    // The PreferencesDialog
    PreferencesDialog preferencesDialog = null;

    public OpenPreferencesDialogAction(ProcessDashboard parent) {
        super(resources.getString(MENU_TEXT_RESOURCE));
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        if (preferencesDialog == null) {
            preferencesDialog =
                new PreferencesDialog(parent, resources.getString(DIALOG_TITLE_RESOURCE));
        }
        else  {
            preferencesDialog.setVisible(true);
        }

    }

}
