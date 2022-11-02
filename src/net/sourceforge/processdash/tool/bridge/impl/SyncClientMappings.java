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

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;

public class SyncClientMappings {

    public static void initialize() {
        initialize((File) null);
    }

    public static void initialize(WorkingDirectory dir) {
        initialize(dir.getTargetDirectory());
    }

    public static void initialize(File seedDir) {
        SyncClientMappings scm = new SyncClientMappings();
        scm.scanCommonLocations();
        if (seedDir != null)
            scm.scanParentsOfDirectory(seedDir);
    }



    private FolderMappingManager mgr;

    private Set<String> currentKeys;

    private Set<String> currentFolders;

    private SyncClientMappings() {
        // make a list of the keys and folders currently known to the
        // FolderMappingManager
        this.mgr = FolderMappingManager.getInstance();
        Map<String, String> currentMappings = mgr.reload().getFolders();
        this.currentKeys = currentMappings.keySet();
        this.currentFolders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);
        this.currentFolders.addAll(currentMappings.values());
    }

    private void scanCommonLocations() {
        // scan the user's home directory looking for sync client subdirs
        File homeDir = new File(System.getProperty("user.home"));
        scanChildrenOfDirectory(homeDir);
    }

    private void scanChildrenOfDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            // look for subdirs that match known naming patterns
            for (File f : files)
                maybeAddDirectory(f);
        }
    }

    private void scanParentsOfDirectory(File dir) {
        // scan the ancestors of the path, looking for known sync client naming
        // patterns. This helps when we're launching a dashboard that's stored
        // under a sync client folder we didn't how to find.
        File f = dir.getParentFile();
        while (f != null) {
            maybeAddDirectory(f);
            f = f.getParentFile();
        }
    }

    private void maybeAddDirectory(File f) {
        String key = f.getName();
        String folder = f.getAbsolutePath();
        if (nameContainsToken(key) //
                && f.isDirectory() //
                && !currentKeys.contains(key) //
                && !currentFolders.contains(folder)) {
            mgr.store(key, folder);
        }
    }

    private static boolean nameContainsToken(String name) {
        name = name.toLowerCase();
        for (String token : TOKENS) {
            if (name.contains(token.toLowerCase()))
                return true;
        }
        return false;
    }

    private static final String[] TOKENS = { "OneDrive", "DropBox",
            "Google Drive", "My Drive" };

}
