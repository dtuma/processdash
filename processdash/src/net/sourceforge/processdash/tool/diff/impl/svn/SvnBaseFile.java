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

import java.util.logging.Level;

import net.sourceforge.processdash.util.XMLUtils;

public class SvnBaseFile extends SvnCatFile {

    private String relativePath;

    private String revision;


    public SvnBaseFile(String relativePath) {
        super(relativePath + "@BASE");
        this.relativePath = relativePath;
    }

    public String getRevision() {
        if (revision == null) {
            try {
                helper.waitTillReady();
            } catch (Exception e) {}
        }
        return revision;
    }


    @Override
    public void execute(SvnExecutor svn) throws Exception {
        super.execute(svn);

        try {
            this.revision = XMLUtils.xPathStr("/info/entry/@revision", //
                svn.execXml("info", "--xml", relativePath));
        } catch (Exception e) {
            SvnExecutor.logger.log(Level.SEVERE,
                "Could not retrieve base revision for " + relativePath, e);
        }
    }

}
