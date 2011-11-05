// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.engine;

public class FilenameComparable {

    private String fullPath;

    private String directory;

    private String file;

    private String suffix;

    public FilenameComparable(String fullPath) {
        this.fullPath = fullPath;

        int slashPos = fullPath.lastIndexOf('/');
        int backslashPos = fullPath.lastIndexOf('\\');
        int dirEnd = Math.max(slashPos, backslashPos);
        if (dirEnd == -1)
            this.directory = "";
        else
            this.directory = fullPath.substring(0, dirEnd);

        int dotPos = fullPath.lastIndexOf('.');
        if (dotPos > dirEnd) {
            file = fullPath.substring(dirEnd+1, dotPos);
            suffix = fullPath.substring(dotPos).toLowerCase();
        } else {
            file = fullPath.substring(dirEnd+1);
            suffix = "";
        }
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getDirectory() {
        return directory;
    }

    public String getFile() {
        return file;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return fullPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FilenameComparable) {
            FilenameComparable that = (FilenameComparable) obj;
            return this.fullPath.equals(that.fullPath);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fullPath.hashCode();
    }

}
