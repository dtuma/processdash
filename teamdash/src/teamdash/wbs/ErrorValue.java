// Copyright (C) 2002-2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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


package teamdash.wbs;

/** Simple class to describe a value with an associated error
 */
public class ErrorValue extends WrappedValue {

    public static final int ERROR = 0;
    public static final int WARNING = 1;
    public static final int INFO = 2;

    /** The error message */
    public String error;
    /** The severity of the error */
    public int severity;

    /** Create an ErrorValue with a severity of {@link #ERROR} */
    public ErrorValue(Object value, String errorMsg) {
        this(value, errorMsg, ERROR);
    }

    /** Create an ErrorValue */
    public ErrorValue(Object value, String errorMsg, int severity) {
        this.value = value;
        this.error = errorMsg;
        this.severity = severity;
    }

}
