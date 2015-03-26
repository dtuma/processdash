// Copyright (C) 2006-2010 Tuma Solutions, LLC
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class FramesetPageAssemblers {

    private static final String FRAME_PARAM = "frame";
    private static final String FRAME_TOP = "top";
    private static final String FRAME_TOC = "toc";
    private static final String FRAME_CONTENT = "content";
    private static final String FRAME_NONE = "none";
    private static final String TOP_FRAME_NAME = "topFrame";

    private static final String SECTION_ID_PARAM = "section";


    /** Determine whether it would be appropriate to view this page using
     * frames.
     * 
     * @param page the page to display
     * @param parameters the parameters passed to the CmsContentDispatcher
     * @return a {@link PageAssembler} to use.  If frames are not appropriate,
     *     returns null.
     */
    public static PageAssembler getViewAssembler(PageContentTO page,
            Map parameters) {
        if (hasSectionHeading(page) == false)
            return null;

        String frameParam = (String) parameters.get(FRAME_PARAM);
        if (FRAME_NONE.equals(frameParam))
            return null;
        else if (FRAME_TOC.equals(frameParam))
            return new TocPageAssembler();
        else if (FRAME_CONTENT.equals(frameParam))
            return new SectionContentAssembler();
        else if (FRAME_TOP.equals(frameParam))
            return new FramesetPageAssembler();
        else
            return new HiddenFramesetPageAssembler();
    }


    /** Determine whether it would be appropriate to edit a portion of this
     * page, as a result of a request to edit from frames mode.
     * 
     * @param page the page to edit
     * @param parameters the parameters passed to the CmsContentDispatcher
     * @return a {@link PageAssembler} to use.  If a frameset-based editor is
     *     not appropriate, returns null.
     */
    public static PageAssembler getEditAssembler(PageContentTO page,
            Map parameters) {

        String frameParam = (String) parameters.get(FRAME_PARAM);
        if (FRAME_TOC.equals(frameParam))
            return new EditTocPageAssembler();
        else if (FRAME_CONTENT.equals(frameParam))
            return new EditSectionPageAssembler();
        else
            return null;
    }


    /** When a page is saved and we are redirecting to the page which will
     * render the saved results, the content dispatcher will call this method
     * to determine whether we need to add any parameters to the resulting URL.
     * 
     *  When the user edits a single section of a page, and saves those changes,
     *  they presumably want to that section of the page to be displayed after
     *  the save is complete.  In that scenario, this method will return a
     *  query parameter that will trigger the display of that section of the
     *  page.  In all other scenarios, this method will return null.
     * 
     * @param parameters the parameters that were posted to the save operation
     */
    public static String getExtraSaveParams(Map parameters) {
        String sectionID = (String) parameters.get(SECTION_ID_PARAM);
        if (sectionID != null)
            return SECTION_ID_PARAM + "=" + HTMLUtils.urlEncode(sectionID);
        else
            return null;
    }



    /** A PageAssembler that can display an enclosing, placeholder frameset.
     * 
     * Internet Explorer's handling of MTHML files cannot handle "target=_top"
     * hyperlinks.  So when an element wants to replace the entire frameset,
     * we can't replace "_top".  This class creates a placeholder frameset
     * which has no obvious/visible appearance, but it defines a frame called
     * "topFrame" which we can target to replace to change the entire page.
     */
    private static class HiddenFramesetPageAssembler extends
            AbstractViewPageAssembler {

        protected boolean shouldInvokeSnippet(SnippetInstanceTO snip) {
            return false;
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {
            out.write("<html>\n");
            Set customHeaderItems = new HashSet();
            addScript(customHeaderItems, "/lib/fixSlash.js");
            writeHead(out, customHeaderItems, page);

            out.write("<frameset cols=\"1,*\" frameborder=\"0\" border=\"0\" "
                    + "framespacing=\"0\">\n");
            String selfUrl = CmsContentDispatcher.getSimpleSelfUri(environment,
                    false);

            out.write("<frame name=\"empty\" src=\"about:blank\" noresize " +
                        "scrolling=\"no\" frameborder=\"0\">\n");

            out.write("<frame name=\"" + TOP_FRAME_NAME + "\" src=\"");
            out.write(HTMLUtils.escapeEntities(HTMLUtils.appendQuery(selfUrl,
                    FRAME_PARAM, FRAME_TOP)));
            out.write("\" frameborder=\"0\" noresize>\n");

            out.write("</frameset>\n");
            out.write("</html>\n");
        }

    }



    /** A PageAssembler that can display the top-level frameset */
    private static class FramesetPageAssembler extends
            AbstractViewPageAssembler {

        protected boolean shouldInvokeSnippet(SnippetInstanceTO snip) {
            return false;
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {
            out.write("<html>\n");
            writeHead(out, Collections.EMPTY_SET, page);

            out.write("<frameset cols=\"153,*\">\n");
            String selfUrl = CmsContentDispatcher.getSimpleSelfUri(environment,
                    false);
            selfUrl = HTMLUtils.removeParam(selfUrl, FRAME_PARAM);
            selfUrl = ensureValidSectionParam(selfUrl, page);

            out.write("<frame name=\"toc\" src=\"");
            out.write(HTMLUtils.escapeEntities(HTMLUtils.appendQuery(selfUrl,
                    FRAME_PARAM, FRAME_TOC)));
            out.write("\" frameborder=\"0\">\n");

            out.write("<frame name=\"contents\" scrolling=\"yes\" src=\"");
            out.write(HTMLUtils.escapeEntities(HTMLUtils.appendQuery(selfUrl,
                    FRAME_PARAM, FRAME_CONTENT)));
            out.write("\">\n");

            out.write("</frameset>\n");
            out.write("</html>\n");
        }

        private String ensureValidSectionParam(String url, PageContentTO page) {
            String sectionId = (String) parameters.get(SECTION_ID_PARAM);
            String firstValidSectionId = null;

            PageSectionHelper.hideInvalidSections(page);
            for (Iterator i = page.getContentSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();

                // if this snip is a section heading that was not hidden by
                // the hideInvalidSections call above,
                if (PageSectionHelper.isSectionHeading(snip)
                        && snip.getStatus() != SnippetInvoker.STATUS_HIDDEN) {

                    if (sectionId != null
                            && sectionId.equals(snip.getInstanceID()))
                        // if the URL named this section, all is well.  Just
                        // return the URL unchanged.
                        return url;

                    else if (firstValidSectionId == null
                            || PageSectionHelper.isDefaultSectionHeading(snip))
                        // if we've just found the first valid section heading,
                        // or if we've found the default section heading,
                        // remember its instance ID for later.
                        firstValidSectionId = snip.getInstanceID();
                }
            }

            // if we reached here, then the URL either didn't name a section,
            // or the section named was invalid.  Set the section heading
            // parameter to indicate the first valid section we found.
            url = HTMLUtils.removeParam(url, SECTION_ID_PARAM);
            url = HTMLUtils.appendQuery(url, SECTION_ID_PARAM,
                    firstValidSectionId);
            return url;
        }
    }


    /** A PageAssembler that can display the table of contents, containing
     * a list of the sections in a page */
    private static class TocPageAssembler extends AbstractViewPageAssembler {

        public void setParameters(Map parameters) {
            parameters.put("mode", "toc");
            super.setParameters(parameters);
        }

        protected void addPageSpecificParameters(Map params, PageContentTO page) {
            super.addPageSpecificParameters(params, page);
            setFrameUriParams(params);
        }

        protected boolean shouldInvokeContentSnippet(SnippetInstanceTO snip) {
            return PageSectionHelper.isSectionHeading(snip);
        }

        protected String getEditURI(Map env) {
            return HTMLUtils.appendQuery(CmsContentDispatcher.getSimpleSelfUri(
                    env, false), "mode", "edit");
        }

        protected void writePage(Writer out, Set headerItems, PageContentTO page)
                throws IOException {
            PageSectionHelper.hideInvalidSections(page);

            out.write(HTML_TRANSITIONAL_DOCTYPE);
            out.write("<html>\n");
            writeHead(out, headerItems, page);
            out.write("<body>\n");

            // write the page header items
            for (Iterator i = page.getHeaderSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                writeSnippet(out, snip);
            }

            // write a hyperlink for each page section.
            String selfUrl = CmsContentDispatcher.getSimpleSelfUri(environment,
                    true);
            String contentUrl = HTMLUtils.appendQuery(selfUrl, FRAME_PARAM,
                    FRAME_CONTENT);
            for (Iterator i = page.getContentSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                if (PageSectionHelper.isSectionHeading(snip)
                        && snip.getStatus() == SnippetInvoker.STATUS_OK) {
                    out.write("<p><a target=\"contents\" href=\"");
                    String sectionUrl = HTMLUtils.appendQuery(contentUrl,
                            SECTION_ID_PARAM, snip.getInstanceID());
                    out.write(HTMLUtils.escapeEntities(sectionUrl));
                    out.write("\">");
                    out.write(snip.getGeneratedContent());
                    out.write("</a></p>\n\n");
                }
            }

            if (!parameters.containsKey("EXPORT")) {
                out.write("<hr>\n");

                // write a link at the bottom to view the page without frames
                String singlePageUrl = HTMLUtils.appendQuery(selfUrl,
                        FRAME_PARAM, FRAME_NONE);
                String anchorHtml = "<a target=\"_top\" href=\""
                    + HTMLUtils.escapeEntities(singlePageUrl) + "\">";
                String html = resources.getString("No_Frames_HTML");
                html = StringUtils.findAndReplace(html, "<A>", anchorHtml);
                html = StringUtils.findAndReplace(html, "<a>", anchorHtml);
                out.write("<p>");
                out.write(html);
                out.write("</p>\n\n");
            }

            // write the page footer items
            for (Iterator i = page.getFooterSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                writeSnippet(out, snip);
            }

            out.write("</body>\n</html>\n");
        }

    }


    /** A PageAssembler that can display the content for a single section
     * of a page. */
    private static class SectionContentAssembler extends
            ViewSinglePageAssembler {

        private PageSectionHelper sectionHelper;

        public void setParameters(Map parameters) {
            super.setParameters(parameters);
            String sectionID = (String) parameters.get(SECTION_ID_PARAM);
            sectionHelper = new PageSectionHelper(sectionID);
        }

        protected void addPageSpecificParameters(Map params, PageContentTO page) {
            super.addPageSpecificParameters(params, page);
            setFrameUriParams(params);
        }

        protected boolean shouldInvokeContentSnippet(SnippetInstanceTO snip) {
            return sectionHelper.test(snip);
        }

        protected String getEditURI(Map env) {
            return HTMLUtils.appendQuery(CmsContentDispatcher.getSimpleSelfUri(
                    env, false), "mode", "edit");
        }

        protected String getEditLinkTarget() {
            // open the editor for this section in the same frame where it
            // was being viewed.
            return null;
        }

    }


    /** A PageAssembler that can display the sections of a page for editing
     * and rearrangement.
     */
    private static class EditTocPageAssembler extends
            EditSinglePageAssembler {

        protected boolean shouldInvokeContentSnippet(SnippetInstanceTO snip) {
            boolean isSectionHeading = PageSectionHelper.isSectionHeading(snip);
            if (isSectionHeading) {
                addFlagParam(snip, "Edit_TOC_Mode", parameters);
                snip.setAlternateName(resources
                        .getString("SectionHeading.TOC_Label"));
            }
            return isSectionHeading;
        }

        protected void writePageSnippetEditors(Writer out, PageContentTO page)
                throws IOException {

            boolean sectionsHaveStarted = false;

            for (Iterator i = page.getContentSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                boolean isSectionHeading = PageSectionHelper
                        .isSectionHeading(snip);

                if (isSectionHeading) {
                    if (sectionsHaveStarted) {
                        // close the div for the previous section.
                        out.write("</div>\n\n");
                    } else {
                        out.write("<div id='snippetContainer'>\n\n");
                        sectionsHaveStarted = true;
                    }
                    writeDivWrapperStart(out, snip.getNamespace());
                }

                int style = (isSectionHeading ? EDIT_STYLE_UNWRAPPED
                        : EDIT_STYLE_INVISIBLE);
                writeSnippet(out, snip, style);
            }


            if (sectionsHaveStarted) {
                // close the div for the previous section.
                out.write("</div>\n\n");
                // close the snippet container div
                out.write("</div>\n\n");
            } else {
                // write an empty snippet container div.
                out.write("<div id='snippetContainer'>\n\n");
                out.write("</div>\n\n");
            }

            setAddNewExtraArgs(out, "&NS_Edit_TOC_Mode=t"
                    + "&altNameKey=TOC_Label&snippetID="
                    + PageSectionHelper.SECTION_HEADING_SNIP_ID);
        }

    }


    private static class EditSectionPageAssembler extends
            EditSinglePageAssembler {

        private PageSectionHelper sectionHelper;

        public void setParameters(Map parameters) {
            super.setParameters(parameters);
            String sectionID = (String) parameters.get(SECTION_ID_PARAM);
            sectionHelper = new PageSectionHelper(sectionID);
        }

        protected void addPageSpecificParameters(Map params, PageContentTO page) {
            super.addPageSpecificParameters(params, page);
            setFrameUriParams(params);
        }

        protected boolean shouldInvokeContentSnippet(SnippetInstanceTO snip) {
            int status = sectionHelper.getStatus(snip);
            if (status == PageSectionHelper.STATUS_START)
                addFlagParam(snip, "Edit_Single_Section_Mode", parameters);

            return (status == PageSectionHelper.STATUS_START
                    || status == PageSectionHelper.STATUS_DURING);
        }

        protected void writePageMetadataEditors(Writer out, PageContentTO page)
                throws IOException {
            // don't allow the user to edit the page title when editing a single
            // section.  But record it so we don't lose the value.
            writeHidden(out, PAGE_TITLE, page.getPageTitle());
            writePageMetadataValues(out, page);
        }

        protected void writePageSnippetEditors(Writer out, PageContentTO page)
                throws IOException {

            sectionHelper.reset();

            out.write("<div class='cmsMetadataSection'>\n\n");

            for (Iterator i = page.getContentSnippets(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                int snipStatus = sectionHelper.getStatus(snip);

                if (snipStatus == PageSectionHelper.STATUS_END)
                    // close the snippetContainer div
                    out.write("</div>\n\n");

                if (snipStatus == PageSectionHelper.STATUS_START) {
                    // write out a snippet editor for the section heading
                    writeSnippet(out, snip, EDIT_STYLE_CHROMELESS);
                    // close the cmsMetadataSection div
                    out.write("</div>\n\n");
                    // start the snippet container div
                    out.write("<div id='snippetContainer'>\n\n");

                } else {
                    boolean isActiveSnippet =
                        (snipStatus == PageSectionHelper.STATUS_DURING);
                    int style = (isActiveSnippet ? EDIT_STYLE_WRAPPED
                            : EDIT_STYLE_INVISIBLE);
                    writeSnippet(out, snip, style);
                }
            }

            int finalStatus = sectionHelper.getStatus(null);
            switch (finalStatus) {
            case PageSectionHelper.STATUS_BEFORE:
                // we never saw the section in question.  This is most
                // certainly an error;  how to handle it?
                break;

            case PageSectionHelper.STATUS_END:
                // close the snippet container div
                out.write("</div>\n\n");
                break;

            case PageSectionHelper.STATUS_START:   // can't happen
            case PageSectionHelper.STATUS_DURING:  // can't happen
            case PageSectionHelper.STATUS_AFTER:   // no action needed.
            }

            setAddNewExtraArgs(out, "&NS_deny="
                    + PageSectionHelper.SECTION_HEADING_SNIP_ID);
            writeHidden(out, SECTION_ID_PARAM, sectionHelper
                    .getSectionInstanceID());
        }

    }


    /** Write a script to the page that will register extra args to be used
     * during the "add snippet" operation. */
    private static void setAddNewExtraArgs(Writer out, String args)
            throws IOException {
        out.write("<script type=\"text/javascript\">");
        out.write("DashCMS.setAddNewExtraArgs('");
        out.write(args);
        out.write("');</script>\n");
    }


    /** In preparation for invoking a snippet, set a new parameter value
     * 
     * @param snip the snippet we're about to invoke
     * @param paramName the name of a parameter to set
     * @param parameters the parameters map
     */
    private static void addFlagParam(SnippetInstanceTO snip, String paramName,
            Map parameters) {
        String ns = snip.getNamespace();
        parameters.put(ns + paramName, "t");
        parameters.put(ns + paramName + "_ALL", new String[] { "t" });
    }


    /** Setup the FULL_PAGE_URI and FULL_PAGE_TARGET parameters on behalf
     * of a PageAssembler that is displaying content in frames.
     */
    private static void setFrameUriParams(Map params) {
        String frameUri = (String) params.get(CMSSnippetEnvironment.CURRENT_FRAME_URI);
        StringBuffer uri = new StringBuffer(frameUri);
        HTMLUtils.removeParam(uri, FRAME_PARAM);
        HTMLUtils.appendQuery(uri, FRAME_PARAM, FRAME_TOP);
        if (params.containsKey("EXPORT"))
            HTMLUtils.removeParam(uri, SECTION_ID_PARAM);
        params.put(CMSSnippetEnvironment.FULL_PAGE_URI, uri.toString());
        params.put(CMSSnippetEnvironment.FULL_PAGE_TARGET, TOP_FRAME_NAME);
    }


    /** Return true if the given page contains at least one section heading. */
    private static boolean hasSectionHeading(PageContentTO page) {
        for (Iterator i = page.getSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            if (PageSectionHelper.isSectionHeading(snip))
                return true;
        }
        return false;
    }

}
