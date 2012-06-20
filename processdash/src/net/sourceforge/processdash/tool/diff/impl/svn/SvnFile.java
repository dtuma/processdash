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

package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Stack;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.diff.engine.AccountingType;
import net.sourceforge.processdash.tool.diff.engine.FileToAnalyzeSubtitled;

public class SvnFile implements FileToAnalyzeSubtitled {

    private String filename;

    private Stack<SvnFileVersion> versions;

    public SvnFile(String filename) {
        this.filename = filename;
        this.versions = new Stack<SvnFileVersion>();
    }


    /// The next methods implement the FileToAnalyzeSubtitled API

    public String getFilename() {
        return filename;
    }

    public String getSubtitle() {
        AccountingType localType = getLocalModType();
        if (localType != null)
            return resources.getString("Report.Locally_" + localType);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i + 1 < versions.size(); i += 2) {
            String revA = versions.get(i).getRevision();
            String revB = versions.get(i+1).getRevision();
            result.append(", -r ").append(revA).append(":").append(revB);
        }
        if (result.length() > 2)
            return result.substring(2);
        else
            return null;
    }

    public List getVersions() {
        return versions;
    }

    public InputStream getContents(Object version) throws IOException {
        return ((SvnFileVersion) version).getContents();
    }



    /// Methods for modifying the revision history of this object

    protected void addVersion(SvnFileVersion v, boolean trackChanges) {
        if (isDeleted() && v != SvnEmptyFile.DELETED)
            throw new IllegalStateException(
                    "Cannot append version to deleted file");

        int numVer = versions.size();
        boolean lastVersionIsTracking = ((numVer & 1) == 0);
        if (trackChanges == lastVersionIsTracking)
            versions.pop();

        versions.add(v);
    }

    protected void undeleteAndRename(String newFilename) {
        if (isDeleted()) {
            versions.pop();
            this.filename = newFilename;
        } else {
            throw new IllegalStateException(
                    "Cannot undelete a file which has not been deleted");
        }
    }


    // Methods indicating the state of the object

    protected boolean isDeleted() {
        return (!versions.isEmpty() && versions.peek() == SvnEmptyFile.DELETED);
    }

    protected AccountingType getLocalModType() {
        if (versions.size() == 2) {
            if (versions.get(0) == SvnEmptyFile.ADDED
                    && versions.get(1) instanceof SvnWorkingFile)
                return AccountingType.Added;
            if (versions.get(0) instanceof SvnBaseFile
                    && versions.get(1) instanceof SvnWorkingFile)
                return AccountingType.Modified;
            if (versions.get(0) instanceof SvnBaseFile
                    && versions.get(1) == SvnEmptyFile.DELETED)
                return AccountingType.Deleted;
        }
        return null;
    }

    static Resources resources = Resources.getDashBundle("LOCDiff");

}
