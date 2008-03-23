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
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
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

    /**
     * Return an ImportDirectory object capable of serving data from a
     * particular resource collection.
     * 
     * @param locations
     *                a list of filenames or URLs that might point to the
     *                desired resource collection. The first location that can
     *                be successfully used will be returned. Note that URLs are
     *                only considered successful if we can contact the server in
     *                question, but if a filename is passed in, it will always
     *                result in a "successful" ImportDirectory object, even if
     *                the named directory does not exist.
     * @return a resource collection, or null if no ImportDirectory object could
     *         be successfully created from the list of locations
     */
    public ImportDirectory get(String... locations) {
        for (String location : locations) {
            // ignore null or empty locations
            if (!StringUtils.hasValue(location))
                continue;

            // check to see if we have already created an ImportDirectory
            // object for this location in the past.  If so, return it.
            ImportDirectory cached = cache.get(normalize(location));
            if (cached != null)
                return refresh(cached);

            // If the location represents a directory that is being mapped to
            // a specific location by the ExternalLocationMapper, then we
            // should unequivocally serve files out of that directory. (The
            // typical use case for this would be imported resources that
            // were extracted from a data backup by the Quick Launcher.)
            String remappedLocation = ExternalLocationMapper.getInstance()
                    .remapFilename(location);
            if (!normalizedEquals(remappedLocation, location))
                return get(new File(remappedLocation), NO_REMOTE);

            // If the location is a URL, try contacting that server and
            // retrieving the data.
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
            }

            // The location is a filename.  Create an ImportDirectory object
            // to serve data from the named directory.  Note that the
            // object we return could be a plain local directory, or could be
            // a bridged directory if we discover that a team server is
            // handling the named directory.
            else {
                File dir = new File(location);
                return get(dir, CHECK_REMOTE);
            }
        }

        return null;
    }

    public ImportDirectory get(File dir) {
        return get(dir, CHECK_REMOTE);
    }

    private ImportDirectory get(File dir, boolean noRemote) {
        String path = normalize(dir.getPath());
        ImportDirectory cached = cache.get(path);
        if (cached != null)
            return refresh(cached);

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
        if (result == null) {
            result = new LocalImportDirectory(dir);
            logger.fine("Using local import directory " + dir.getPath());
        }

        return putInCache(path, result);
    }

    public ImportDirectory get(URL url) throws IOException {
        String urlStr = url.toString();
        ImportDirectory cached = cache.get(urlStr);
        if (cached != null)
            return refresh(cached);

        ImportDirectory result = new BridgedImportDirectory(urlStr,
                TeamDataDirStrategy.INSTANCE);
        logger.fine("Using bridged import directory "
                + result.getDirectory().getPath());

        return putInCache(urlStr, result);
    }

    private ImportDirectory putInCache(String key, ImportDirectory dir) {
        synchronized (cache) {
            ImportDirectory cached = cache.get(key);
            if (cached != null)
                return cached;

            cache.put(key, dir);
            return dir;
        }
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    private static boolean normalizedEquals(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return normalize(a).equals(normalize(b));
    }

    private static ImportDirectory refresh(ImportDirectory dir) {
        if (dir != null) {
            try {
                dir.update();
            } catch (IOException ioe) {}
        }
        return dir;
    }

    private static final boolean NO_REMOTE = true;
    private static final boolean CHECK_REMOTE = false;
}
