// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.util.FileUtils;

public class BridgedImportDirectory implements ImportDirectory {

    protected String remoteURL;

    protected File importDirectory;

    protected ResourceBridgeClient client;

    protected long lastUpdateTime;

    protected BridgedImportDirectory(String remoteURL,
            FileResourceCollectionStrategy strategy) throws IOException {
        this.remoteURL = remoteURL;
        this.importDirectory = new File(DirectoryPreferences
                .getMasterImportDirectory(), getImportId());
        this.importDirectory.mkdirs();

        FileResourceCollection localCollection = new FileResourceCollection(
                importDirectory);
        localCollection.setStrategy(strategy);
        this.client = new ResourceBridgeClient(localCollection, remoteURL);

        this.client.syncDown();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    protected String getImportId() {
        String url = remoteURL;
        if (url.startsWith("https"))
            url = "http" + url.substring(5);
        return FileUtils.makeSafeIdentifier(url);
    }


    public File getDirectory() {
        return importDirectory;
    }

    public String getRemoteURL() {
        return remoteURL;
    }

    public void update() throws IOException {
        // this method may get called overzealously by code in different layers
        // of the application.  If it is called more than once within a few
        // milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateAge = now - lastUpdateTime;
        if (lastUpdateAge > 1000 || lastUpdateAge < 0) {
            client.syncDown();
            lastUpdateTime = System.currentTimeMillis();
        }
    }

}
