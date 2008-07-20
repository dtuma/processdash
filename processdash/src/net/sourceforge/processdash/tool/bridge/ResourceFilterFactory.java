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

import java.util.Collections;
import java.util.Map;

import net.sourceforge.processdash.util.StringUtils;

public class ResourceFilterFactory {

    public static final String LAST_MOD_PARAM = "ifModifiedAfter";

    public static final String INCLUDE_PARAM = "include";

    public static final String EXCLUDE_PARAM = "exclude";

    public static final String DEFAULT_EXCLUDES = "defaultExcludes";

    public static final String[] DEFAULT_EXCLUDE_FILENAMES = { "log.txt" };

    public static final ResourceFilter DEFAULT_FILTER =
        getForRequest(Collections.EMPTY_MAP);

    public static ResourceFilter getForRequest(Map request) {
        ResourceFilter filter = new ResourceFilter();

        // load the modification time from the request parameter if present
        String modTimeCutoff = getParameter(request, LAST_MOD_PARAM);
        if (StringUtils.hasValue(modTimeCutoff)) {
            try {
                filter.setIfModifiedAfter(Long.parseLong(modTimeCutoff));
            } catch (NumberFormatException nfe) {
                filter.setIfModifiedAfter(Long.MAX_VALUE);
            }
        }

        // load include patterns from the request if present
        String[] includes = getParameterValues(request, INCLUDE_PARAM);
        if (includes != null) {
            for (String pattern : includes) {
                filter.addIncludePattern(pattern);
            }
        }

        // load exclude patterns from the request if present
        String[] excludes = getParameterValues(request, EXCLUDE_PARAM);
        if (excludes != null) {
            for (String pattern : excludes) {
                filter.addExcludePattern(pattern);
            }
        }

        // add default excludes unless otherwise specified
        String defaultExcludes = getParameter(request, DEFAULT_EXCLUDES);
        if (!"no".equals(defaultExcludes)) {
            for (String name : DEFAULT_EXCLUDE_FILENAMES) {
                filter.addExcludePattern(name);
            }
        }

        return filter;
    }

    private static String getParameter(Map parameters, String attrName) {
        Object result = parameters.get(attrName);
        if (result == null)
            return null;
        else if (result instanceof String[]) {
            String[] ss = (String[]) result;
            if (ss.length > 0)
                return ss[0];
            else
                return null;
        } else {
            return String.valueOf(result);
        }
    }

    private static String[] getParameterValues(Map parameters, String attrName) {
        Object result = parameters.get(attrName);
        if (result == null)
            return null;
        else if (result instanceof String[])
            return (String[]) result;
        else
            return new String[] { String.valueOf(result) };

    }

}
