// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;


public class TimeLogSelection implements Transferable {

    public static final DataFlavor FLAVOR = new Flavor();


    List<TimeLogEntry> entries;

    String text;

    public TimeLogSelection(List<TimeLogEntry> entries, String text) {
        this.entries = entries;
        this.text = text;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (flavor.equals(FLAVOR))
            return entries;
        else if (flavor.equals(DataFlavor.stringFlavor))
            return text;
        else
            throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { FLAVOR, DataFlavor.stringFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavor.equals(FLAVOR) || flavor.equals(DataFlavor.stringFlavor));
    }


    private static class Flavor extends DataFlavor {
        public Flavor() {
            super(DataFlavor.javaJVMLocalObjectMimeType + "; class="
                    + TimeLogSelection.class.getName(), "Time Log Data");
        }
    };

}
