// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.Writer;

/** Interface for an object which can render a page
 */
public interface PageAssembler {


    /** Handle an page rendering request.
     * 
     * By the time this method is called, headers will already have been
     * written to the client.  Only page content needs to be generated.
     *
     * For access to posted parameter data, etc, this object should implement
     * one or more of the {@link Needs} subinterfaces.  All relevant objects
     * will be communicated via those subinterfaces before this method is
     * called.
     * 
     * @param out a writer for sending data to the client.
     * @param page the content of the page to render.
     */
    public void service(Writer out, PageContentTO page) throws IOException;

}
