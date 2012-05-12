// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.merge;

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;
import static net.sourceforge.processdash.util.NullSafeObjectUtils.HC;


public class MergeWarning<ID> {

    public enum Severity {
        INFO, CONFLICT
    };

    private Severity severity;

    private String key;

    private ID mainNodeID;

    private ID incomingNodeID;

    public MergeWarning(Severity severity, String key, ID mainNodeID,
            ID incomingNodeID) {
        this.severity = severity;
        this.key = key;
        this.mainNodeID = mainNodeID;
        this.incomingNodeID = incomingNodeID;
    }

    public MergeWarning(Severity severity, ID mainNodeID,
            Object mainChangeType, ID incomingNodeID, Object incomingChangeType) {
        this(severity, mainChangeType.toString() + "_"
                + incomingChangeType.toString(), mainNodeID, incomingNodeID);
    }

    public MergeWarning(Severity severity, ID targetNodeID,
            Object mainChangeType, Object incomingChangeType) {
        this(severity, targetNodeID, mainChangeType, targetNodeID,
                incomingChangeType);
    }


    public Severity getSeverity() {
        return severity;
    }

    public String getKey() {
        return key;
    }

    public ID getMainNodeID() {
        return mainNodeID;
    }

    public ID getIncomingNodeID() {
        return incomingNodeID;
    }

    public boolean matches(String s) {
        return key.equals(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != getClass())
            return false;

        MergeWarning that = (MergeWarning) obj;
        return EQ(this.severity, that.severity)
                && EQ(this.key, that.key)
                && EQ(this.mainNodeID, that.mainNodeID)
                && EQ(this.incomingNodeID, that.incomingNodeID);
    }

    @Override
    public int hashCode() {
        int result = HC(severity);
        result = (result << 3) ^ HC(key);
        result = (result << 3) ^ HC(mainNodeID);
        result = (result << 3) ^ HC(incomingNodeID);
        return result;
    }

}
