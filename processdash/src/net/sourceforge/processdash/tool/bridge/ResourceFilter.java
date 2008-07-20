// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.util.glob.GlobPattern;


public class ResourceFilter {

    private static final long IGNORE_MOD_TIME = -1;

    private long modifiedAfter;

    private List<String> includePatterns;

    private List<String> excludePatterns;


    public ResourceFilter() {
        this.modifiedAfter = IGNORE_MOD_TIME;
    }

    public void setIfModifiedAfter(long time) {
        modifiedAfter = time;
    }

    public void addIncludePattern(String pattern) {
        if (includePatterns == null)
            includePatterns = new ArrayList<String>();
        includePatterns.add(pattern);
    }

    public void addExcludePattern(String pattern) {
        if (excludePatterns == null)
            excludePatterns = new ArrayList<String>();
        excludePatterns.add(pattern);
    }

    public List<String> filterCollection(ResourceCollectionInfo c) {
        List<String> result = new ArrayList<String>();

        for (String oneName : c.listResourceNames()) {
            if (test(oneName, c))
                result.add(oneName);
        }

        return result;
    }

    protected boolean test(String resourceName, ResourceCollectionInfo c) {
        if (excludePatterns != null && matches(resourceName, excludePatterns))
            return false;

        if (modifiedAfter != IGNORE_MOD_TIME) {
            long lastModTime = c.getLastModified(resourceName);
            if (lastModTime <= modifiedAfter)
                return false;
        }

        if (includePatterns == null)
            return true;
        else
            return matches(resourceName, includePatterns);
    }

    protected boolean matches(String resourceName, List<String> patterns) {
        for (String pattern : patterns) {
            if (matches(resourceName, pattern))
                return true;
        }

        return false;
    }

    /**
     * Test to see if a given resource name matches a given pattern.
     * 
     * This implementation provides a simple filename glob capability.
     * Subclasses can override to provide additional/alternate functionality.
     * 
     * @param resourceName
     *                the name of a resource to test
     * @param pattern
     *                the pattern to match against
     * @return true if the given resource name matches the pattern
     */
    protected boolean matches(String resourceName, String pattern) {
        return GlobPattern.test(pattern, resourceName);
    }

}
