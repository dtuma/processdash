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

package net.sourceforge.processdash.ui.lib.binding;

import net.sourceforge.processdash.util.StringUtils;

public class ErrorValue implements ErrorData {

    private String error;

    private int severity;

    public ErrorValue(String error, int severity) {
        this.error = error;
        this.severity = severity;
    }

    public String getError() {
        return error;
    }

    public int getSeverity() {
        return severity;
    }

    public static boolean isRealError(Object obj) {
        if (obj instanceof ErrorData) {
            ErrorData errorData = (ErrorData) obj;
            return errorData.getSeverity() != NO_ERROR
                    && StringUtils.hasValue(errorData.getError());
        }
        return false;
    }
}
