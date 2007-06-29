// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui.importer;

import javax.swing.JOptionPane;

public class AbortImport extends Exception {

    public static void showErrorAndAbort(String resKey, Object fmtArg) throws AbortImport {
        showError(resKey, fmtArg);
        throw new AbortImport();
    }

    public static void showErrorAndAbort(String resKey) throws AbortImport {
        showError(resKey, null);
        throw new AbortImport();
    }

    public static void showError(String resKey, Object fmtArg) {
        Object message;
        if (fmtArg == null)
            message = DefectImportForm.resources.getStrings("Errors." + resKey
                    + ".Message");
        else
            // TODO: handle multiple format args?
            message = DefectImportForm.resources.formatStrings("Errors."
                    + resKey + ".Message_FMT", fmtArg);

        String title = DefectImportForm.resources.getString("Errors." + resKey
                + ".Title");

        JOptionPane.showMessageDialog(null, message, title,
                JOptionPane.ERROR_MESSAGE);
    }

}
