// Copyright (C) 2008-2011 Tuma Solutions, LLC
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

    private File baseDirectory;

    private String[] preferCachesFor;

    private ImportDirectoryFactory() {
        cache = Collections
                .synchronizedMap(new HashMap<String, ImportDirectory>());
        baseDirectory = null;
        preferCachesFor = null;
    }

    public void setBaseDirectory(File dir) {
        this.baseDirectory = dir;
    }

    public void setPreferCachesFor(String[] preferCachesFor) {
        this.preferCachesFor = preferCachesFor;
    }

    /**
     * Return an ImportDirectory object capable of serving data from a
     * particular resource collection.
     * 
     * @param locations
     *                a list of filenames or URLs that might point to the
     *                desired resource collection.
     * @return an {@link ImportDirectory} object that can be used to read from
     *         the specified resource collection, or null if no ImportDirectory
     *         object could be successfully created from the list of locations
     */
    public ImportDirectory get(String... locations) {
        try {
            return new DynamicImportDirectory(locations);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Return an ImportDirectory object capable of serving data from a
     * particular resource collection.
     * 
     * @param locations
     *                a list of filenames or URLs that might point to the
     *                desired resource collection. The first location that can
     *                be successfully used will be returned.
     * @return a resource collection, or null if no ImportDirectory object could
     *         be successfully created from the list of locations
     */
    ImportDirectory getImpl(String[] locations) {
        ImportDirectory fallbackResult = null;

        for (String location : locations) {
            // ignore null or empty locations
            if (!StringUtils.hasValue(location))
                continue;

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
            if (TeamServerSelector.isUrlFormat(location)) {
                // See if a cached version of the data is available.  If so,
                // and a cached version is preferred, return it.
                ImportDirectory c = getCachedImportDirectory(location);
                if (isViable(c, REQUIRE_CONTENTS)) {
                    if (preferCacheFor(location))
                        return c;
                    else
                        fallbackResult = c;
                }

                // See if we can contact the team server.
                URL remoteURL = TeamServerSelector.resolveServerURL(location);
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
            // resulting object could be a plain local directory, or could be
            // a bridged directory if we discover that a team server is
            // handling the named directory.
            else {
                File dir;
                if ((location.startsWith("./") || location.startsWith(".\\"))
                        && baseDirectory != null) {
                    dir = new File(baseDirectory, location.substring(2));
                } else {
                    dir = new File(location);
                }
                ImportDirectory fileResult = get(dir, CHECK_REMOTE);
                if (isViable(fileResult, NO_CONTENTS_REQUIRED))
                    return fileResult;

                // if the local directory doesn't exist, see if there is a
                // cached import directory that appears to correspond to the
                // desired directory
                String possibleURL = TeamServerSelector.getDefaultURL(dir);
                if (possibleURL != null) {
                    ImportDirectory c = getCachedImportDirectory(possibleURL);
                    if (isViable(c, REQUIRE_CONTENTS))
                        fallbackResult = c;
                }

                // The desired local directory is nonexistent, no team server
                // is filling in, and no cached directory is available.  There
                // isn't much we can do but save the local directory away in
                // case we have no other options.
                if (fallbackResult == null) {
                    fallbackResult = fileResult;
                }
            }
        }

        return fallbackResult;
    }

    public ImportDirectory get(File dir) {
        return get(dir, CHECK_REMOTE);
    }

    private ImportDirectory get(File dir, boolean noRemote) {
        URL u = (noRemote ? null : TeamServerSelector.getServerURL(dir));
        if (u != null) {
            try {
                return get(u);
            } catch (IOException e) {
                logger.log(Level.WARNING,
                    "Encountered error when contacting server " + u, e);
            }
        }

        String path = normalize(dir.getPath());
        ImportDirectory cached = cache.get(path);
        if (cached != null)
            return refresh(cached);

        ImportDirectory result = new LocalImportDirectory(dir);
        return putInCache(path, result);
    }

    public ImportDirectory get(URL url) throws IOException {
        String urlStr = url.toString();
        ImportDirectory cached = cache.get(urlStr);
        if (cached != null)
            return refresh(cached);

        ImportDirectory result = new BridgedImportDirectory(urlStr,
                TeamDataDirStrategy.INSTANCE);
        return putInCache(urlStr, result);
    }

    private boolean preferCacheFor(String location) {
        if (location == null || preferCachesFor == null)
            return false;

        location = location.toLowerCase();
        for (String preferCacheToken : preferCachesFor)
            if ("*".equals(preferCacheToken)
                    || location.contains(preferCacheToken))
                return true;

        return false;
    }

    private ImportDirectory getCachedImportDirectory(String url) {
        String key = "[CACHED]:" + url;
        ImportDirectory cached = cache.get(key);
        if (cached != null)
            return refresh(cached);

        ImportDirectory result = new CachedImportDirectory(url);
        return putInCache(key, result);
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

    private static boolean isViable(ImportDirectory d, boolean requireContents) {
        if (d == null)
            return false;

        File dir = d.getDirectory();
        if (dir == null || !dir.isDirectory())
            return false;

        if (requireContents) {
            String[] files = dir.list();
            if (files == null || files.length == 0)
                return false;
        }

        return true;
    }

    private static final boolean NO_REMOTE = true;
    private static final boolean CHECK_REMOTE = false;
    private static final boolean REQUIRE_CONTENTS = true;
    private static final boolean NO_CONTENTS_REQUIRED = false;
}
