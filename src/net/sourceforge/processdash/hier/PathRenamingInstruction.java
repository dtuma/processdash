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

package net.sourceforge.processdash.hier;

import java.util.Iterator;
import java.util.List;


public class PathRenamingInstruction {

    private String oldPath;

    private String newPath;

    public PathRenamingInstruction(String oldPath, String newPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public String getOldPath() {
        return oldPath;
    }

    public static String renamePath(List renamingInstructions, String path) {
        if (path == null)
            return null;

        for (Iterator iter = renamingInstructions.iterator(); iter.hasNext();) {
            PathRenamingInstruction instr = (PathRenamingInstruction) iter
                    .next();
            if (Filter.pathMatches(path, instr.getOldPath(), true)) {
                path = instr.getNewPath()
                        + path.substring(instr.getOldPath().length());
            }
        }

        return path;
    }

}
