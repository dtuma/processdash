// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import net.sourceforge.processdash.tool.redact.RedactFilter;
import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.util.LineFilteringReader;
import net.sourceforge.processdash.util.StringMapper;

public class FilterLineBasedFiles implements RedactFilter {

    private RedactFilterData data;

    private List<LineBasedFilter> lineFilters;

    public void afterPropertiesSet() throws IOException {
        lineFilters = RedactFilterUtils.getExtensions(data,
            "redact-line-filter");
    }

    public Reader filter(RedactFilterData data, String filename, Reader contents)
            throws IOException {
        StringMapper filter = getFilter(filename);

        if (filter == null)
            return contents;
        else
            return new LineFilteringReader(contents, filter);
    }

    private StringMapper getFilter(String filename) {
        StringMapper result = null;
        for (LineBasedFilter f : lineFilters) {
            if (f.shouldFilter(filename)) {
                if (result == null)
                    result = f;
                else
                    result = new ConcatStringMapper(f, result);
            }
        }
        return result;
    }

    private static final class ConcatStringMapper implements StringMapper {

        private StringMapper outerMap, innerMap;

        public ConcatStringMapper(StringMapper outerMap, StringMapper innerMap) {
            this.outerMap = outerMap;
            this.innerMap = innerMap;
        }

        public String getString(String str) {
            String innerResult = innerMap.getString(str);
            if (innerResult == null)
                return null;
            else
                return outerMap.getString(innerResult);
        }
    }

}
