// Copyright (C) 2020-2021 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sourceforge.processdash.util.RobustFileOutputStream;

public class DaemonMetadataFile extends DaemonMetadata {

    private File metadataDir;

    private File syncRequestFile;

    private File daemonStateFile;


    public DaemonMetadataFile(String systemID, File wbsDir) {
        super(systemID);
        metadataDir = new File(wbsDir, "sync");
        syncRequestFile = new File(metadataDir,
                systemID + "-sync-requested.txt");
        daemonStateFile = new File(metadataDir,
                systemID + "-sync-daemon-state.txt");
    }


    public boolean isSyncRequestSupported() {
        return true;
    }

    public void setSyncRequestPending(boolean pending) throws IOException {
        if (pending) {
            metadataDir.mkdir();
            FileOutputStream out = new FileOutputStream(syncRequestFile);
            out.write(tstamp(0).getBytes("UTF-8"));
            out.close();
        } else {
            syncRequestFile.delete();
        }
    }

    public boolean isSyncRequestPending() throws IOException {
        return syncRequestFile.isFile();
    }


    public void load() throws IOException {
        if (daemonStateFile.isFile()) {
            FileInputStream in = new FileInputStream(daemonStateFile);
            stateProps.clear();
            stateProps.load(in);
            in.close();
        }
    }

    public void save() throws IOException {
        RobustFileOutputStream out = new RobustFileOutputStream(
                daemonStateFile);
        stateProps.store(out, null);
        out.close();
        log.finer("Daemon state: " + stateProps.toString());
    }

}
