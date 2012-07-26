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

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.XMLUtils;

public abstract class AbstractLineBasedFilter extends RedactFilterUtils
        implements LineBasedFilter {

    protected RedactFilterData data;

    protected PatternList filenamePatterns;

    public void setConfigElement(Element xml, String attrName) {
        String patternAttr = xml.getAttribute("filenamePattern");
        if (XMLUtils.hasValue(patternAttr))
            filenamePatterns = new PatternList(patternAttr);
    }

    protected void setFilenamePatterns(String... patterns) {
        this.filenamePatterns = new PatternList(patterns);
    }

    public boolean shouldFilter(String filename) {
        return filenamePatterns != null && filenamePatterns.matches(filename);
    }

}
