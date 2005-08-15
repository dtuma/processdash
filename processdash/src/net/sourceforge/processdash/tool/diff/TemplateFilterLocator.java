// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2005 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.tool.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.ResourcePool;


public class TemplateFilterLocator {

    private static List FILTER_CLASSES = null;
    static final Resources resource = Resources.getDashBundle("LOCDiff");


    public static List getFilters(WebServer web) {
        if (FILTER_CLASSES == null)
            FILTER_CLASSES = getFilterClasses(web);
        return instantiateFilters(FILTER_CLASSES);
    }

    private static List getFilterClasses(WebServer web) {
        List result = new ArrayList();

        List filterNames = TemplateLoader.getLanguageFilters();
        Collections.sort(filterNames);
        Iterator i = filterNames.iterator();
        String filterName = null;
        while (i.hasNext()) try {
            filterName = (String) i.next();

            try {
                web.getRequest("/" + filterName, false);
                ResourcePool pool = web.getCGIPool(filterName);
                LanguageFilter filter = (LanguageFilter) pool.get();
                result.add(filter.getClass());
            } catch (Exception e) {
                // This could be a null pointer exception, because there was
                // no CGIPool for the given filter, or a class cast exception
                // if the CGI script returned is not a LanguageFilter.
                continue;
            }

        } catch (Exception e) {
            System.err.println
                (resource.format("PspDiffCouldNotInit_FMT", filterName));
            e.printStackTrace();
        }

        return result;
    }

    private static List instantiateFilters(List filterClasses) {
        List result = new ArrayList();
        for (Iterator i = filterClasses.iterator(); i.hasNext();) try {
            Class clz = (Class) i.next();
            result.add(clz.newInstance());
        } catch (Exception e) {}
        return result;
    }

}
