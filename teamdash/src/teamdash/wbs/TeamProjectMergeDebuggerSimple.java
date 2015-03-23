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

package teamdash.wbs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class TeamProjectMergeDebuggerSimple extends TeamProjectMergeDebugger {

    private Map<String, Object> deferredMergeData;

    private Throwable deferredMergeException;

    public TeamProjectMergeDebuggerSimple() {
        super(true);
        deferredMergeData = new HashMap();
    }

    @Override
    public void mergeStarting() {
        deferredMergeData.clear();
        deferredMergeException = null;
        super.mergeStarting();
    }

    @Override
    public void mergeDataNotify(String type, File srcDir) {
        deferredMergeData.put(type, srcDir);
    }

    @Override
    public void mergeDataNotify(String type, TeamProject teamProject) {
        deferredMergeData.put(type, teamProject);
    }

    @Override
    public void mergeException(Throwable t) {
        this.deferredMergeException = t;
    }


    @Override
    public boolean supportsZipOfAllMerges() {
        return false;
    }

    @Override
    public File makeZipOfAllMerges() {
        return null;
    }

    @Override
    public File makeZipOfCurrentMerge() {
        try {
            createDebugDir();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

        // tell our superclass about the merged data we received earlier
        for (Map.Entry<String, Object> e : deferredMergeData.entrySet()) {
            String type = e.getKey();
            Object data = e.getValue();
            if (data instanceof File) {
                File srcDir = (File) data;
                super.mergeDataNotify(type, srcDir);

            } else if (data instanceof TeamProject) {
                TeamProject teamProject = (TeamProject) data;
                super.mergeDataNotify(type, teamProject);
            }
        }
        deferredMergeData.clear();

        if (deferredMergeException != null) {
            super.mergeException(deferredMergeException);
            deferredMergeException = null;
        }

        return super.makeZipOfCurrentMerge();
    }

}
