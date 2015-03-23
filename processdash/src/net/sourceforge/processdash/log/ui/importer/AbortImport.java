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
