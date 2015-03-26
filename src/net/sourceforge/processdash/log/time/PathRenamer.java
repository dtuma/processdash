// Copyright (C) 2005-2006 Tuma Solutions, LLC
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

import java.util.List;

import net.sourceforge.processdash.hier.PathRenamingInstruction;
import net.sourceforge.processdash.log.ChangeFlagged;

public class PathRenamer {

    public static boolean isRenamingOperation(TimeLogEntry tle) {
        if (tle instanceof ChangeFlaggedTimeLogEntry) {
            ChangeFlaggedTimeLogEntry mod = (ChangeFlaggedTimeLogEntry) tle;
            return mod.getChangeFlag() == ChangeFlagged.BATCH_MODIFICATION
                    && mod.getID() == 0 && mod.getPath() != null
                    && mod.getPath().indexOf('\n') != -1
                    && mod.getStartTime() == null && mod.getElapsedTime() == 0
                    && mod.getInterruptTime() == 0 && mod.getComment() == null;
        } else
            return false;
    }

    public static ChangeFlaggedTimeLogEntry toTimeLogEntry(
            PathRenamingInstruction instr) {
        return getRenameModification(instr.getOldPath(), instr.getNewPath());
    }

    public static ChangeFlaggedTimeLogEntry getRenameModification(
            String oldPath, String newPath) {
        String path = oldPath + "\n" + newPath;
        return new TimeLogEntryVO(0, path, null, 0, 0, null,
                ChangeFlagged.BATCH_MODIFICATION);
    }

    public static PathRenamingInstruction toInstruction(TimeLogEntry tle) {
        if (!isRenamingOperation(tle))
            throw new IllegalArgumentException(
                    "Time log entry does not describe a batch modification.");

        return toInstruction(tle.getPath());
    }

    public static PathRenamingInstruction toInstruction(String path) {
        int pos = path.indexOf('\n');
        String oldPath = path.substring(0, pos);
        String newPath = path.substring(pos + 1);
        return new PathRenamingInstruction(oldPath, newPath);
    }


    public static ChangeFlaggedTimeLogEntry getRenameModification(
            TimeLogEntry tle, List renamingOperations) {
        if (renamingOperations == null || renamingOperations.isEmpty()
                || tle == null)
            return null;

        String oldPath = tle.getPath();
        String newPath = PathRenamingInstruction.renamePath(renamingOperations,
                oldPath);

        if (oldPath == newPath)
            return null;
        else {
            ChangeFlaggedTimeLogEntry mod = new TimeLogEntryVO(tle.getID(),
                    newPath, null, 0, 0, null, ChangeFlagged.MODIFIED);
            return mod;
        }
    }
}
