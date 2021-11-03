// Copyright (C) 2016-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sourceforge.processdash.util.lock.LockFailureException;

public class BridgedImportSubdirectory extends LocalImportDirectory {

    private BridgedWorkingDirectory base;

    private String baseUrl;

    private String subdir;

    private SyncFilter filter;

    public BridgedImportSubdirectory(BridgedWorkingDirectory base, String subdir) {
        super(new File(base.getDirectory(), subdir));
        if (base.hasAcquiredWriteLock())
            this.base = base;
        this.baseUrl = base.getDescription();
        this.subdir = subdir;
        this.filter = new SubdirFilter();
    }

    @Override
    public void validate() throws IOException {
        update();
        super.validate();
    }

    @Override
    public void update() {
        try {
            if (base != null && base.hasAcquiredWriteLock()
                    && !ImportDirectoryFactory.getInstance().isCaching()) {
                base.client.syncUp(filter);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void writeUnlockedFile(String filename, InputStream source)
            throws IOException, LockFailureException {
        ResourceBridgeClient.uploadSingleFile(new URL(baseUrl),
            subdir + "/" + filename, source);
        update();
    }

    @Override
    public void deleteUnlockedFile(String filename)
            throws IOException, LockFailureException {
        ResourceBridgeClient.deleteSingleFile(new URL(baseUrl),
            subdir + "/" + filename);
        update();
    }

    @Override
    public String getRemoteLocation() {
        if (base == null)
            return null;
        else
            return base.getDescription() + "#" + subdir;
    }

    private class SubdirFilter implements SyncFilter {

        private String prefix = subdir + "/";

        public boolean shouldSync(String name, long localTS, long remoteTS) {
            return name.startsWith(prefix);
        }
    }

}
