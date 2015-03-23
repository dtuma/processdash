// Copyright (C) 2001-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.net.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Map;

public interface TinyCGI {

    /** Key for looking up the PSPProperties hierarchy in the environment. */
    public static final String PSP_PROPERTIES = "PSP_PROPERTIES";

    /** Key for looking up the data repository in the environment. */
    public static final String DATA_REPOSITORY = "DATA_REPOSITORY";

    /** Key for looking up the object cache in the environment. */
    public static final String OBJECT_CACHE = "OBJECT_CACHE";

    /** Key for looking up the web server in the environment. */
    public static final String TINY_WEB_SERVER = "TINY_WEB_SERVER";

    /** Key for looking up the DashContext in the environment. */
    public static final String DASHBOARD_CONTEXT = "DASHBOARD_CONTEXT";

    /**
     * Handle a cgi-like request within a TinyWebServer.
     *
     * <P>Rather than using stdin/stout, streams are passed in to the
     * service method.  Rather than using environment variables
     * (impossible in Java 2), the environment is passed in via a
     * <code>Map</code>.
     *
     * <P><B>WARNING</B>: TinyWebServer request handling is not 100%
     * CGI compliant!  Current differences include:
     * <OL>
     * <LI>Internal redirection is not performed.  If you send back a
     *     Location: /some/place header, it will be sent all the way
     *     back to the browser client for redirection processing.
     *
     * <LI>Some CGI environment variables are set differently than you
     *     might expect:<DL>
     *
     *     <DT><code>PATH_INFO</code>
     *     <DD>rather than being the extra stuff that follows the script
     *         URL, is the initial token before the path URL used to
     *         associate this URL with a project.  For example, for
     *         the URL <code>/234/psp0/script.htm</code>,
     *         <code>PATH_INFO</code> would be <code>234</code>.
     *
     *     <DT><code>PATH_TRANSLATED</code>
     *     <DD>is the translation of the above <code>PATH_INFO</code> into
     *         a project hierarcy path like "/Projects/My Project".  You
     *         can use this as a starting point for queries into the
     *         <code>PSPProperties</code> hierarchy.
     *
     *     <DT><code>SCRIPT_NAME</code>
     *     <DD>is not the full path to the script; it is missing the initial
     *         project token.  For the URL example above,
     *         <code>SCRIPT_NAME</code> would be /psp0/script.htm .  To get
     *         the full path to the script, use <code>SCRIPT_PATH</code>.
     *
     *     <DT><code>REMOTE_HOST</code>
     *     <DD>is not a String; it is an object whose toString() method will
     *         lazily look up the remote host name.
     *
     *     <DT><code>SERVER_NAME</code>
     *     <DD>is not a String; it is an object whose toString() method will
     *         lazily look up the effective server name.
     *
     *     <DT><code>PSP_PROPERTIES</code>
     *     <DD>is an extra variable in the environment, pointing to the
     *         <code>PSPProperties</code> hierarchy.
     *
     *     <DT><code>DATA_REPOSITORY</code>
     *     <DD>is an extra variable in the environment, pointing to the
     *         application's data repository.
     *
     *     <DT><code>TINY_WEB_SERVER</code>
     *     <DD>is an extra variable in the environment, pointing to the
     *         web server which is executing this script.
     * </DL></OL>
     */
    void service(InputStream in, OutputStream out, Map env)
        throws IOException;
}
