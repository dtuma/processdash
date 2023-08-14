// Copyright (C) 2022-2023 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sourceforge.processdash.util.HTMLUtils;

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
     * Return true if the given path begins with a <tt>[key]</tt> portion,
     * optionally followed by a <tt>/relative/path</tt>.
     */
    public static boolean isEncodedPath(String path) {
        return parseEncodedPath(path) != null;
    }


    /**
     * If a given path is in the form <tt>[key]/relative/path</tt>, returns a
     * two-element array with the key and the relative path. The relative path
     * could be an empty string; if not, it will use platform-specific separator
     * characters. If the path is not in that format, returns null.
     */
    private static String[] parseEncodedPath(String path) {
        if (path == null || !path.startsWith("["))
            return null;

        int pos = path.indexOf("]");
        if (pos < 2)
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


    /**
     * Given a path in the form <tt>[key]/relative/path</tt>, look under the
     * mapped folder for the given key, and search recursively for a directory
     * that matches the relative path.
     * 
     * A directory "matches" if the final portions of its full path overlap with
     * the final portions of the <tt>/relative/path</tt> by at least
     * <tt>minMatchLen</tt> segments. If multiple directories match, the one
     * with the most overlapping segments will be returned. If several matching
     * directories have the same number of overlapping segments, a
     * FileNotFoundException will be thrown.
     * 
     * @param path
     *            the encoded path of a directory to search for
     * @param minMatchLen
     *            the number of final path segments that must overlap for a
     *            directory to be considered a match
     * @return the directory that most closely matches the given path. (If none
     *         could be found, an exception will be thrown; this method never
     *         returns null)
     * @throws IllegalArgumentException
     *             if the given path does not include both a <tt>[key]</tt> and
     *             a <tt>/relative/path</tt>
     * @throws MissingMapping
     *             if the [key] in the given path does not match any registered
     *             folder mapping
     * @throws MissingRootFolder
     *             if the root folder for the given [key] does not exist
     * @throws MissingSubfolder
     *             if a matching folder underneath could not be found, or if
     *             multiple folders match equally
     */
    public File searchForDirectory(String path, int minMatchLen)
            throws IllegalArgumentException, MissingMapping, MissingRootFolder,
            MissingSubfolder {
        String[] parsed = parseEncodedPath(path);
        if (parsed == null || parsed.length < 2 || parsed[1].length() == 0)
            throw new IllegalArgumentException(
                    "'" + path + "' is not in [key]/relative/path format");

        String key = parsed[0];
        String relPath = parsed[1];

        // locate the mapped folder named by this path, or abort on error
        tryLoad();
        String folderPath = folders.get(key);
        if (folderPath == null || folderPath.length() == 0)
            throw new MissingMapping(path, key, relPath);
        File folderDir = new File(folderPath);
        if (!folderDir.isDirectory())
            throw new MissingRootFolder(folderPath, key, relPath);

        // see if terminal look-ahead matches were requested
        String[] lookAheads = null;
        String subdirPath = relPath.replace('\\', '/');
        int lookAheadPos = subdirPath.indexOf('?');
        if (lookAheadPos != -1) {
            lookAheads = subdirPath.substring(lookAheadPos + 1).split("\\?");
            subdirPath = subdirPath.substring(0, lookAheadPos);
        }

        // If an exact match is present, return it
        File exactMatch = subdirPath.length() == 0 ? folderDir
                : new File(folderDir, subdirPath);
        if (exactMatch.isDirectory() && lookAheadsMatch(exactMatch, lookAheads))
            return exactMatch;

        // perform a search for matching subdirectories
        String[] searchFor = subdirPath.split("/");
        List<MatchingDir> matchingDirs = new ArrayList();
        scanRecursivelyForMatchingDirectories(folderDir, searchFor, minMatchLen,
            lookAheads, matchingDirs);

        // no matches found? abort
        if (matchingDirs.isEmpty())
            throw new MissingSubfolder(exactMatch.getPath(), key, relPath);

        // if we have a single match, return it
        if (matchingDirs.size() == 1)
            return matchingDirs.get(0).directory;

        // if multiple matches were found, see if one is better than the others
        Collections.sort(matchingDirs);
        if (matchingDirs.get(0).matchLen > matchingDirs.get(1).matchLen)
            return matchingDirs.get(0).directory;

        // multiple matches of similar length: abort
        throw new MissingSubfolder(exactMatch.getPath(), key, relPath);
    }

    private void scanRecursivelyForMatchingDirectories(File directory,
            String[] searchFor, int minMatchLen, String[] lookAheads,
            List<MatchingDir> matchingDirs) {
        int matchLen = countMatchingPathSegments(directory, searchFor);
        if (matchLen >= minMatchLen && lookAheadsMatch(directory, lookAheads)) {
            matchingDirs.add(new MatchingDir(directory, matchLen));
        } else {
            File[] children = directory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory())
                        scanRecursivelyForMatchingDirectories(child, searchFor,
                            minMatchLen, lookAheads, matchingDirs);
                }
            }
        }
    }

    private int countMatchingPathSegments(File dir, String[] searchFor) {
        int pos = searchFor.length;
        int matchLen = 0;
        while (pos > 0 && dir != null) {
            if (searchFor[--pos].equalsIgnoreCase(dir.getName())) {
                matchLen++;
                dir = dir.getParentFile();
            } else {
                break;
            }
        }
        return matchLen;
    }

    private boolean lookAheadsMatch(File dir, String[] lookAheads) {
        if (lookAheads != null) {
            for (String oneLookAhead : lookAheads) {
                File oneLookAheadFile = new File(dir, oneLookAhead);
                if (!oneLookAheadFile.exists())
                    return false;
            }
        }

        return true;
    }

    private class MatchingDir implements Comparable<MatchingDir> {

        File directory;

        int matchLen;

        MatchingDir(File directory, int matchLen) {
            this.directory = directory;
            this.matchLen = matchLen;
        }

        public int compareTo(MatchingDir that) {
            return that.matchLen - this.matchLen;
        }
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


    public class MappingException extends FileNotFoundException {

        private String key, relPath;

        private MappingException(String path, String key, String relPath) {
            super(path);
            this.key = key;
            this.relPath = relPath;
        }

        public String getPath() {
            return getMessage();
        }

        public String getKey() {
            return key;
        }

        public String getRelPath() {
            return relPath;
        }

        public String asQuery() {
            StringBuffer q = new StringBuffer();
            HTMLUtils.appendQuery(q, "sharedFolderError",
                getClass().getSimpleName());
            HTMLUtils.appendQuery(q, "sfPath", getPath());
            HTMLUtils.appendQuery(q, "sfKey", getKey());
            HTMLUtils.appendQuery(q, "sfRelPath", getRelPath().substring(1));
            return q.toString();
        }
    }

    public class MissingMapping extends MappingException {
        private MissingMapping(String path, String key, String relPath) {
            super(path, key, relPath);
        }
    }

    public class MissingRootFolder extends MappingException {
        private MissingRootFolder(String path, String key, String relPath) {
            super(path, key, relPath);
        }
    }

    public class MissingSubfolder extends MappingException {
        private MissingSubfolder(String path, String key, String relPath) {
            super(path, key, relPath);
        }
    }

}
