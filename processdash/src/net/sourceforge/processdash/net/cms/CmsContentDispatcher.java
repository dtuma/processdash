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
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


/** Master servlet which receives cms content requests, and dispatches to
 * appropriate handlers.
 */
public class CmsContentDispatcher extends TinyCGIBase {

    protected void doGet() throws IOException {
        dispatch();
    }

    protected void doPost() throws IOException {
        parseFormData();
        dispatch();
    }

    protected void dispatch() throws IOException {
        SnippetDefinitionManager.initialize();

        // extract the name of the file we should display.  For now, we
        // strip the "/cms/" prefix from the beginning, then hardcode an
        // assumption on an XML file.
        String filename = (String) env.get("SCRIPT_NAME");
        filename = filename.substring(WebServer.CMS_URI_PREFIX.length() + 1)
                + ".xml";

        if (act(filename))
            return;

        render(filename);
    }

    /** Possibly handle an action request.
     * 
     * @param filename the filename for the request.
     * @return true if this was an action request, and was handled.  false otherwise
     */
    private boolean act(String filename) throws IOException {
        String action = (String) parameters.get("action");
        if (action == null || action.length() == 0)
            return false;

        ActionHandler h = selectActionHandler(action);
        initialize(h, filename);
        String newDestQuery = h.service(out, filename);
        if (newDestQuery != null) {
            StringBuffer destUri = new StringBuffer(getSimpleSelfUri(env, true));
            HTMLUtils.appendQuery(destUri, newDestQuery);
            HTMLUtils.appendQuery(destUri, FramesetPageAssemblers
                    .getExtraSaveParams(parameters));
            out.write("Location: " + destUri + "\r\n\r\n");
        }

        return true;
    }

    /** Select an appropriate handler for an action request */
    private ActionHandler selectActionHandler(String action) throws IOException {
        String mode = (String) parameters.get("mode");
        if ("edit".equals(mode) && "save".equals(action))
            return new SavePageHandler();
        else
            throw new IOException("Unsupported mode/action");
    }

    /** Handle a rendering request.
     * 
     * @param filename the filename for the request.
     */
    protected void render(String filename) throws IOException {
        PageContentTO page = getPageContent(filename);
        lookupSnippets(page);

        // select a page assembler based on environment, parameters, and content
        PageAssembler assembler = selectPageAssembler(page);
        initialize(assembler, filename);

        // write an HTML header and delegate to the page assembler
        writeHeader();
        assembler.service(out, page);
    }

    /** Select an appropriate assembler for a rendering request */
    private PageAssembler selectPageAssembler(PageContentTO page)
            throws IOException {
        String mode = getParameter("mode");
        PageAssembler result = null;

        if (mode == null || "view".equals(mode)) {
            result = FramesetPageAssemblers.getViewAssembler(page, parameters);
            if (result == null)
                result = new ViewSinglePageAssembler();

        } else if ("edit".equals(mode)) {
            result = FramesetPageAssemblers.getEditAssembler(page, parameters);
            if (result == null)
                result = new EditSinglePageAssembler();

        } else if ("addNew".equals(mode)) {
            result = new AddNewItemPageAssembler();
        }

        if (result == null)
            throw new IOException("Unsupported mode");
        else
            return result;
    }

    private PageContentTO getPageContent(String filename) throws IOException {
        if ("addNew".equals(parameters.get("mode")))
            return createAddPseudoPage();
        else
            return loadPersistedPageContent(filename);
    }

    private PageContentTO createAddPseudoPage() {
        PageContentTO page = new PageContentTO();
        SnippetInstanceTO snip = new SnippetInstanceTO();
        String snippetID = (String) parameters.get("snippetID");
        if (snippetID == null)
            snippetID = "pdash.addNew";
        snip.setSnippetID(snippetID);
        snip.setPageRegion(PageContentTO.REGION_CONTENT);
        page.setSnippets(Collections.singletonList(snip));
        return page;
    }

    private PageContentTO loadPersistedPageContent(String filename)
            throws IOException {
        // retrieve the contents of the named file from the persistence service
        InputStream content = CmsDefaultConfig.getPersistence().open(filename);
        if (content == null)
            throw new TinyCGIException(404, "Not Found");

        // parse the contents
        PageContentTO page = CmsDefaultConfig.getSerializer().parse(content);
        return page;
    }

    private void lookupSnippets(PageContentTO page) {
        for (Iterator i = page.getSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            snip.setDefinition(SnippetDefinitionManager.getSnippet(snip
                    .getSnippetID()));
        }
    }


    /** Initialize an object via dependency injection */
    protected void initialize(Object obj, String filename) {
        if (obj instanceof Needs.Dispatcher)
            ((Needs.Dispatcher) obj).setDispatcher(this);

        if (obj instanceof Needs.Environment)
            ((Needs.Environment) obj).setEnvironment(env);

        if (obj instanceof Needs.Parameters)
            ((Needs.Parameters) obj).setParameters(parameters);

        if (obj instanceof Needs.Prefix)
            ((Needs.Prefix) obj).setPrefix(getPrefix());

        if (obj instanceof Needs.Data)
            ((Needs.Data) obj).setData(getDataContext());

        if (obj instanceof Needs.Filename)
            ((Needs.Filename) obj).setFilename(filename);
    }

    /** Return a relative URI to the page indicated in the given environment.
     * 
     * For example, if the URI is "/Project/Foo//cms/some/page?bar&baz",
     * this would return "page?bar&baz".  Such a URI could be used in an href
     * to reload the page with different options.
     * 
     * @param environment the environment generated by a web server request
     * @param discardQuery if true, the query string will be removed as well;
     *    the example above would become simply "page".
     * @return
     */
    protected static String getSimpleSelfUri(Map environment,
            boolean discardQuery) {
        String requestUri = (String) environment.get("REQUEST_URI");

        // remove path information from the URI, if it is present.
        Matcher m = SELF_URI_PREFIX_PATTERN.matcher(requestUri);
        if (m.matches())
            requestUri = requestUri.substring(m.end(1));

        // remove the query string if requested
        if (discardQuery) {
            int queryPos = requestUri.indexOf('?');
            if (queryPos != -1)
                requestUri = requestUri.substring(0, queryPos);
        }

        return requestUri;
    }

    private static final Pattern SELF_URI_PREFIX_PATTERN = Pattern
            .compile("([^?#]+/).+");


}
