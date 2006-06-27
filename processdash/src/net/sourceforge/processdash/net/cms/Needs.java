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

import java.util.Map;

import net.sourceforge.processdash.data.DataContext;

/** An interface for dependency-injection-style communication of various
 * objects.
 * 
 *  {@link net.sourceforge.processdash.net.cms.PageAssembler} and
 *  {@link net.sourceforge.processdash.net.cms.ActionHandler} objects can
 *  optionally implement one of the subinterfaces below to indicate their
 *  interest in various items.
 */
public interface Needs {

    public interface Dispatcher {
        public void setDispatcher(CmsContentDispatcher dispatcher);
    }

    public interface Environment {
        public void setEnvironment(Map env);
    }

    public interface Parameters {
        public void setParameters(Map parameters);
    }

    public interface Prefix {
        public void setPrefix(String prefix);
    }

    public interface Data {
        public void setData(DataContext context);
    }

    public interface Filename {
        public void setFilename(String filename);
    }

}
