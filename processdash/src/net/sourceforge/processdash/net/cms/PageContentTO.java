// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.util.IteratorFilter;

/** Object that holds data about a CMS page.
 */
public class PageContentTO {

    public static final int REGION_HEADER = 1;

    public static final int REGION_CONTENT = 2;

    private String pageTitle;

    private List snippets;

    /** Gets the title of the page */
    public String getPageTitle() {
        return pageTitle;
    }

    /** Set the title of the page */
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
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
     * @param region one of {@link PageContentTO#REGION_HEADER} or
     *    {@link PageContentTO#REGION_CONTENT}
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
