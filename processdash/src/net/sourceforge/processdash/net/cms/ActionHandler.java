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

import java.io.IOException;
import java.io.Writer;

/** Interface for an object which can handle CMS action requests
 */
public interface ActionHandler {

    /** Handle an action request.
     * 
     * Handlers can operate in one of two modes:  after performing some
     * action, they can either write data to the client, or ask the dispatcher
     * to redirect to a related page.
     * 
     * If a handler wants to write data to the client, it can use the writer
     * passed into this method.  In that case, it should return null from this
     * method to indicate that the response has been sent.
     * 
     * If a handler instead wants the dispatcher to redirect to a related
     * page (the most common behavior), it should return a query string (which
     * can be the empty string).  The dispatcher will append the query string
     * to the URL for the page in question, and redirect the browser to that
     * URL.
     * 
     * For access to posted parameter data, etc, this object should implement
     * one or more of the {@link Needs} subinterfaces.  All relevant objects
     * will be communicated via those subinterfaces before this method is
     * called.
     * 
     * @param out a writer for optionally sending data to the client.
     * @param pageName the name of the page that triggered the action request.
     * @return null if this object has completely handled the request and
     *     written output to the client; or a query string if a redirect is
     *     desired.
     */
    public String service(Writer out, String pageName) throws IOException;

}
