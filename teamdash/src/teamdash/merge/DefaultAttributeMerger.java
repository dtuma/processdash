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

import teamdash.merge.ContentMerger.ErrorReporter;
import teamdash.merge.MergeWarning.Severity;

public class DefaultAttributeMerger<ID, Type> implements
        AttributeMerger<ID, Type> {

    private Severity severity;

    private String warningKey;

    private boolean chooseIncoming;

    public DefaultAttributeMerger(Severity severity) {
        this(severity, "");
    }

    public DefaultAttributeMerger(Severity severity, String warningKey) {
        this(severity, warningKey, false);
    }

    public DefaultAttributeMerger(Severity severity, String warningKey,
            boolean chooseIncoming) {
        this.severity = severity;
        this.warningKey = warningKey;
        this.chooseIncoming = chooseIncoming;
    }

    public Type mergeAttribute(ID nodeID, String attrName, Type base,
            Type main, Type incoming, ErrorReporter<ID> err) {
        if (severity != null && warningKey != null) {

            String warningKey = this.warningKey;
            if (warningKey.length() == 0)
                warningKey = "Attribute." + attrName;

            err.addMergeWarning(new AttributeMergeWarning<ID>(severity,
                    warningKey, nodeID, attrName, base, main, incoming));
        }

        return selectResult(main, incoming);
    }

    protected Type selectResult(Type main, Type incoming) {
        return (chooseIncoming ? incoming : main);
    }

    public static final DefaultAttributeMerger SILENTLY_PREFER_MAIN =
            new DefaultAttributeMerger(null, null, false);
    public static final DefaultAttributeMerger SILENTLY_PREFER_INCOMING =
            new DefaultAttributeMerger(null, null, true);

}
