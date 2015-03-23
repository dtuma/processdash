// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.IteratorFilter;

/** Object that holds data about a CMS page.
 */
public class PageContentTO {

    public static final int REGION_HEADER = 1;

    public static final int REGION_CONTENT = 2;

    public static final int REGION_FOOTER = 3;

    private String pageTitle;

    private Map<String, String> metadata;

    private List snippets;

    /** Gets the title of the page */
    public String getPageTitle() {
        return pageTitle;
    }

    /** Set the title of the page */
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    /** Get the page metadata */
    public Map<String, String> getMetadata() {
        return metadata != null ? metadata : Collections.EMPTY_MAP;
    }

    /** Set the page metadata */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /** Retrieves a piece of page metadata */
    public String getMetadataValue(String attrName, String defaultVal) {
        String result = null;
        if (metadata != null)
            result = metadata.get(attrName);
        return (result == null ? defaultVal : result);
    }

    /** Stores a piece of page metadata */
    public void setMetadataValue(String attrName, String value) {
        if (metadata == null)
            metadata = new HashMap<String, String>();
        if (value == null)
            metadata.remove(attrName);
        else
            metadata.put(attrName, value);
    }

    /** Get a list of
     * {@link net.sourceforge.processdash.net.cms.SnippetInstanceTO} objects,
     * describing all of the snippet instances that are present on this page.
     */
    public List getSnippets() {
        return snippets;
    }

    /** Set the list of
     * {@link net.sourceforge.processdash.net.cms.SnippetInstanceTO} objects,
     * describing all of the snippet instances that are present on this page.
     */
    public void setSnippets(List contentSnippets) {
        this.snippets = contentSnippets;
    }

    /** Return an iterator containing the snippets in a given region of
     * the page.
     * 
     * @param region one of {@link PageContentTO#REGION_HEADER},
     *    {@link PageContentTO#REGION_CONTENT}, or
     *    {@link PageContentTO#REGION_FOOTER}
     * @return an iterator returning
     * {@link net.sourceforge.processdash.net.cms.SnippetInstanceTO} objects
     */
    public Iterator getSnippets(int region) {
        if (snippets == null || snippets.isEmpty())
            return Collections.EMPTY_LIST.iterator();
        else
            return new PageRegionIterator(region);
    }

    /** Return an iterator containing the snippets in the header region of
     * the page. */
    public Iterator getHeaderSnippets() {
        return getSnippets(REGION_HEADER);
    }

    /** Return an iterator containing the snippets in the content region of
     * the page. */
    public Iterator getContentSnippets() {
        return getSnippets(REGION_CONTENT);
    }

    /** Return an iterator containing the snippets in the content region of
     * the page. */
    public Iterator getFooterSnippets() {
        return getSnippets(REGION_FOOTER);
    }


    private class PageRegionIterator extends IteratorFilter {
        int region;
        public PageRegionIterator(int region) {
            super(snippets.iterator());
            this.region = region;
            init();
        }
        protected boolean includeInResults(Object o) {
            SnippetInstanceTO snip = (SnippetInstanceTO) o;
            return snip.getPageRegion() == region;
        }
    }
}
