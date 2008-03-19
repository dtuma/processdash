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
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.util.StringUtils;

public class ImportDirectoryFactory {

    private static final Logger logger = Logger
            .getLogger(ImportDirectoryFactory.class.getName());

    private static final ImportDirectoryFactory INSTANCE = new ImportDirectoryFactory();

    public static ImportDirectoryFactory getInstance() {
        return INSTANCE;
    }



    private Map<String, ImportDirectory> cache;

    private ImportDirectoryFactory() {
        cache = Collections
                .synchronizedMap(new HashMap<String, ImportDirectory>());
    }

    public ImportDirectory get(ImportDirectoryInstruction instr) {
        return get(instr.getURL(), instr.getDirectory());
    }

    public synchronized ImportDirectory get(String... locations) {
        for (String location : locations) {
            if (!StringUtils.hasValue(location))
                continue;

            ImportDirectory cached = cache.get(normalize(location));
            if (cached != null)
                return cached;

            String remappedLocation = ExternalResourceManager.getInstance()
                    .remapFilename(location);
            if (!normalizedEquals(remappedLocation, location))
                return get(new File(remappedLocation), NO_REMOTE);

            if (location.startsWith("http")) {
                URL remoteURL = TeamServerSelector.testServerURL(location);
                if (remoteURL != null) {
                    try {
                        return get(remoteURL);
                    } catch (IOException e) {
                        logger.log(Level.WARNING,
                            "Encountered error when contacting server "
                                    + remoteURL, e);
                    }
                }

            } else {
                File dir = new File(location);
                return get(dir, CHECK_REMOTE);
            }
        }

        return null;
    }

    public ImportDirectory get(File dir) {
        return get(dir, CHECK_REMOTE);
    }

    private synchronized ImportDirectory get(File dir, boolean noRemote) {
        String path = normalize(dir.getPath());
        if (cache.containsKey(path))
            return cache.get(path);

        ImportDirectory result = null;
        URL u = (noRemote ? null : TeamServerSelector.getServerURL(dir));
        if (u != null) {
            try {
                result = get(u);
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Encountered error when contacting server " + u, e);
            }
        }
        if (result == null)
            result = new LocalImportDirectory(dir);

        cache.put(path, result);
        return result;
    }

    public synchronized ImportDirectory get(URL url) throws IOException {
        String urlStr = url.toString();
        if (cache.containsKey(urlStr))
            return cache.get(urlStr);

        ImportDirectory result = new BridgedImportDirectory(urlStr,
                TeamDataDirStrategy.INSTANCE);
        cache.put(urlStr, result);
        return result;
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    private static boolean normalizedEquals(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return normalize(a).equals(normalize(b));
    }

    private static final boolean NO_REMOTE = true;
    private static final boolean CHECK_REMOTE = false;
}
