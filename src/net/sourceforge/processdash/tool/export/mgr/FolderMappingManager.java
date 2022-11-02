// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages a set of user-defined folder mappings, from string keys to absolute
 * filesystem paths.
 * 
 * Supports the encoding and decoding of child folders into key-based relative
 * paths.
 */
public class FolderMappingManager {

    private static FolderMappingManager INSTANCE = new FolderMappingManager();

    public static FolderMappingManager getInstance() {
        return INSTANCE;
    }


    private Preferences prefs;

    private Map<String, String> folders;

    private FolderMappingManager() {
        this.prefs = Preferences.userRoot()
                .node("/net/sourceforge/processdash/sharedFolders");
        this.folders = Collections.EMPTY_MAP;
    }


    /**
     * Reload mappings from user prefs, to pick up any changes that might have
     * been made in a different process.
     */
    public FolderMappingManager reload() {
        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }


    /**
     * Get the list of folder mappings previously registered with the manager.
     * 
     * If a mapping was removed in the past, its key will still be present in
     * the map, with the empty string as a value.
     */
    public Map<String, String> getFolders() {
        tryLoad();
        return Collections.unmodifiableMap(folders);
    }


    /**
     * Remove a folder mapping from the list.
     */
    public void remove(String key) {
        store(key, "");
    }


    /**
     * Add or modify a folder mapping.
     * 
     * @param key
     *            the user key for the folder
     * @param folder
     *            the absolute filesystem path this key should map to
     */
    public synchronized void store(String key, String folder) {
        try {
            maybeLoad();
            if (folder == null)
                folder = "";
            else if (folder.endsWith("/") || folder.endsWith("\\"))
                folder = folder.substring(0, folder.length() - 1);
            folders.put(key, folder);
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * If a given path is a child of one of our mapped folders, reencode it in
     * <tt>[key]/relative/path</tt> form, where:
     * <ul>
     * <li><tt>key</tt> is the key for one of our folder mappings</li>
     * <li><tt>/relative/path</tt> is the relative subpath, underneath the
     * folder that was mapped by that key. If the given path is equal to our
     * mapped folder, this will be the empty string. Otherwise, it will use
     * forward slashes instead of operating-system-specific separators.</li>
     * </ul>
     * 
     * If the given path is not a child of any of our mapped folders, it will be
     * returned unchanged.
     * 
     * @param path
     *            an absolute filesystem path to encode
     * @return a key-based relative path
     */
    public synchronized String encodePath(String path) {
        if (path == null || path.length() == 0 || path.startsWith("["))
            return path;

        String bestKey = null, bestFolder = "";
        for (Entry<String, String> e : getFolders().entrySet()) {
            String key = e.getKey();
            String folder = e.getValue();
            if (folder != null && folder.length() > 0 //
                    && folder.length() > bestFolder.length()
                    && isParentOfPath(folder, path)) {
                bestKey = key;
                bestFolder = folder;
            }
        }

        if (bestKey == null) {
            return path;
        } else {
            String relPath = path.substring(bestFolder.length())
                    .replace(File.separatorChar, '/');
            return "[" + bestKey + "]" + relPath;
        }
    }

    private boolean isParentOfPath(String parentPath, String path) {
        int parentLen = parentPath.length();
        int pathLen = path.length();
        return path.regionMatches(true, 0, parentPath, 0, parentLen)
                && (pathLen == parentLen
                        || path.charAt(parentLen) == File.separatorChar);
    }


    /**
     * If a given path is in the form <tt>[key]/relative/path</tt>, returns a
     * two-element array with the key and the relative path. The relative path
     * could be an empty string; if not, it will use platform-specific separator
     * characters. If the path is not in that format, returns null.
     */
    public static String[] parseEncodedPath(String path) {
        if (path == null || !path.startsWith("["))
            return null;

        int pos = path.indexOf("]");
        if (pos == -1)
            return null;

        String key = path.substring(1, pos);
        String relPath = path.substring(pos + 1) //
                .replace('/', File.separatorChar);
        return new String[] { key, relPath };
    }


    /**
     * If a given path is in the form <tt>[key]/relative/path</tt>, resolve it
     * as an absolute path by looking up the folder for the given key, and
     * appending the relative path underneath.
     * 
     * If the path does not begin with a <tt>[key]</tt>, it is returned
     * unchanged.
     * 
     * @param path
     *            a path to resolve
     * @return the original path, or a resolved path
     * @throws FileNotFoundException
     *             if the path started with a key that was unrecognized. The
     *             exception message will give the name of the missing key.
     */
    public String resolvePath(String path) throws MissingMapping {
        String[] parsed = parseEncodedPath(path);
        if (parsed == null)
            return path;

        String key = parsed[0];
        String relPath = parsed[1];

        tryLoad();
        String folderPath = folders.get(key);
        if (folderPath == null || folderPath.length() == 0)
            throw new MissingMapping(path, key, relPath);
        else
            return folderPath + relPath;
    }


    private void tryLoad() {
        try {
            maybeLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void maybeLoad() throws BackingStoreException {
        if (folders == Collections.EMPTY_MAP)
            load();
    }

    private void load() throws BackingStoreException {
        Map folders = new TreeMap<String, String>();

        for (String key : prefs.keys()) {
            String path = prefs.get(key, null);
            if (path != null)
                folders.put(key, path);
        }

        this.folders = folders;
    }

    private void save() {
        for (Entry<String, String> e : folders.entrySet()) {
            String key = e.getKey();
            String path = e.getValue();
            if (path == null)
                prefs.remove(key);
            else
                prefs.put(key, path);
        }
    }


    public class MissingMapping extends FileNotFoundException {

        private String key, relPath;

        private MissingMapping(String path, String key, String relPath) {
            super(path);
            this.key = key;
            this.relPath = relPath;
        }

        public String getKey() {
            return key;
        }

        public String getRelPath() {
            return relPath;
        }
    }

}
