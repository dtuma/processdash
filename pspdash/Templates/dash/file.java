// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

/*
    Still to do -
        + extra testing, extra error handling.
        + allow templates to come from TemplateLoader URLs, not just from files.
        + don't hardcode the XML_URL, and don't assume only one XML document list.
            dynamically choose the correct XML document list based on inheritableData
             - done, needs testing.
        + if no XML document list is provided, choose a default.
 */

import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.InterpolatingFilter;
import pspdash.data.ListData;
import pspdash.data.SaveableData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.xml.sax.SAXException;
import org.w3c.dom.*;


/** CGI script for integrating external documents into the dashboard.
 *
 * Nearly every process instructs you to create various documents.
 * PSP, for example, tells you that you should create documents like
 * Process Improvement Proposal forms, Logic Specification Templates,
 * Design Review Checklists, etc.  Although it might be possible to spit
 * out a lot of java code and incorporate such forms into the dashboard
 * (as was done for the Size Estimating Template), the CONs outweigh
 * the PROs:
 * <ul>
 * <li><b>PRO:</b> form is handy, no external (commercial) software is needed
 * <li><b>CON:</b> requires LOTS of java coding for EACH new addition, and
 *     tons of work to maintain all that code
 * <li><b>CON:</b> dramatically limits the form/structure/content of the
 *     document (for example, it would be impossible for a developer to
 *     include a diagram in their design document)
 * <li><b>CON:</b> since the information captured in such documents is
 *     typically free-form, there really wouldn't be any (value added)
 *     analysis you could do on it anyway...so you haven't gained much by
 *     storing it inside the Data Repository.
 * </ul>
 *
 * A much better solution is to leverage existing application software for
 * document creation (for example, Microsoft Office on Windows, or
 * KOffice/StarOffice on Unix/Linux platforms).  Even people with
 * absolutely no commercial software probably still have access to some
 * sort of WYSIWYG editor for rich-text-format files.  The dashboard should
 * not attempt to rewrite or replace these applications.
 *
 * So this new CGI script is designed, based on the assumption that:
 * <ul>
 * <li>the user has other programs that are capable of editing the types
 *     of documents they want to edit, and
 * <li>their web browser is intelligent enough to open such documents in
 *     the appropriate editor.
 * </ul>
 * This is true of both IE and Netscape.
 *
 * Here's how the new script works, from an end-user perspective:
 * <ol>
 * <li>The user has created a project using the Hierarchy Editor and is
 *     viewing a process script for that process.
 * <li>They click on a hyperlink for an external document (e.g., Process
 *     Improvement Proposal Form).
 * <li>The first time they click on such a link, they might get a form
 *     asking them to fill in some information (for example, the name of
 *     the directory where the dashboard should store external documents
 *     for this project).  They fill out the form and click "OK".
 * <li>The document opens for editing.
 * </ol>
 *
 * Step #3 only happens if the dashboard doesn't already have all the
 * information it needs to locate the file.  Once the user fills out the
 * requested information, it is saved in the Data Repository appropriately.
 * So typically, the user might see that form once for any given project;
 * then, any time they click on a hyperlink, the document just opens for
 * editing.
 *
 * In the background, this script is:
 * <ol>
 * <li>Dynamically loading one or more XML files which describe the various
 *     documents that apply to the task at hand (what the documents are
 *     called and where they are located).
 * <li>Asking the user for more information if needed
 * <li>Checking to see if the destination directory/file already exists;
 *     if not,<ol>
 *     <li>creating the destination directory and all needed parents
 *     <li>locating the appropriate template for the file
 *     <li>copying the template to the destination</ol>
 * <li>Sending an HTTP redirect message back to the browser, with a
 *     file:// url pointing to the destination document.
 * </ol>
 *
 * The mechanism is very flexible, and driven by XML files (which would
 * presumably be written by the process author).
 */
public class file extends TinyCGIBase {

    public static final String FILE_PARAM = "file";
    public static final String PAGE_COUNT_PARAM = "pageCount";
    public static final String FILE_XML_DATANAME = "FILES_XML";

    public static final String NAME_ATTR = "name";
    public static final String PATH_ATTR = "path";
    public static final String DEFAULT_PATH_ATTR = "defaultPath";
    public static final String TEMPLATE_PATH_ATTR = "templatePath";
    public static final String TEMPLATE_VAL_ATTR = "templateVal";
    public static final String DIRECTORY_TAG_NAME = "directory";

    public static final String TEMPLATE_ROOT_WIN = "\\Templates\\";
    public static final String TEMPLATE_ROOT_UNIX = "/Templates/";


    protected void writeHeader() {}

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {

        // Read any form data posted to this script.
        parseFormData();

        // What file does the user want displayed?
        String filename = getParameter(FILE_PARAM);
        if (filename == null)
            ;                   // some sort of error handling?

        // Find the node in the XML document list that describes this file.
        Element file = findFile(filename);
        if (file == null)
            { sendNoSuchFileMessage(filename);   return; }

        // Compute the path to the requested file.
        File result = computePath(file, false);

        if (!metaPathVariables.isEmpty()) {
            // If any meta variables turned up missing, prompt the
            // user for them before continuing.
            pathVariables = metaPathVariables;
            pathVariableNames = metaPathVariableNames;
            displayNeedInfoForm(filename, null, false, file);
            return;
        }

        if (result == null && !needPathInfo()) {
            // This is an odd situation - no result was found, but we don't
            // have anything to ask the user about...
            sendNoSuchFileMessage(filename);
            return;
        }

        if (result == null) {
            // if we could not locate the file because we need the user
            // to enter more information, display a form.
            displayNeedInfoForm(filename, result, false, file);
            return;
        }

        if (!result.exists() && isDirectory) {
            // if the user is asking for a directory but it doesn't exist,
            // create it for them.
            if (!result.mkdirs()) {
                sendCopyTemplateError("Could not create the directory '" +
                                      result.getPath() + "'.");
                return;
            }
        }

        if (result.exists()) {
            // If we were able to find the named file, and it exists,
            // redirect the user there.
            redirectTo(filename, result);
            return;
        }

        // We had all the information we needed to locate the named file,
        // but it did not exist.  Try to locate a template.
        savePathInfo();
        File template = computePath(file, true);

        if (!metaPathVariables.isEmpty()) {
            // If any meta variables turned up missing, prompt the
            // user for them before continuing.
            pathVariables = metaPathVariables;
            pathVariableNames = metaPathVariableNames;
            displayNeedInfoForm(filename, null, true, file);
            return;
        }

        if (!foundTemplate) {
            // if there was no template information, go back and display a
            // form for the original document.
            restorePathInfo();
            displayNeedInfoForm(filename, result, false, file);
            return;
        }

        String templateURL = null;

        if (isTemplateURL(template)) try {
            // if this template begins with one of the template roots, it
            // isn't really a file at all, but is actually a pseudo-URL.
            templateURL = template.toURL().toString();
            templateURL = templateURL.substring
                (templateURL.indexOf(TEMPLATE_ROOT_UNIX) +
                 TEMPLATE_ROOT_UNIX.length() - 1);
        } catch (MalformedURLException mue) {}


        if (template == null || (templateURL == null && !template.exists())) {
            // if we could not locate the template because we need the user
            // to enter more information, display a form.
            displayNeedInfoForm(filename, template, true, file);
            return;
        }

        // We have located the template and it exists! Create the
        // desired document based upon the template.
        File resultDir = result.getParentFile();
        if (!resultDir.exists())
            if (!resultDir.mkdirs()) {
                sendCopyTemplateError("Could not create the directory '" +
                                      resultDir.getPath() + "'.");
                return;
            }
        if (copyFile(template, templateURL, result) == false) {
            sendCopyTemplateError("Could not copy '" + template.getPath() +
                                  "' to '" + result.getPath() + "'.");
            return;
        }

        // Success! Now redirect the user to the file we just created.
        redirectTo(filename, result);
    }

    /** Send an HTTP REDIRECT message. */
    private void redirectTo(String filename, File result) {
        try {
            if ("redirect".equals(docOpenSetting))
                out.print("Location: " + result.toURL() + "\r\n\r\n");
            else {
                // open the document using the Browser class.
                Browser.launch(result.toURL().toString());

                // now print a null document which takes the user back to
                // the original page they were viewing.
                out.print("Content-type: text/html\r\n");
                out.print("Expires: 0\r\n\r\n");
                String pageCount = getParameter(PAGE_COUNT_PARAM);
                int back = -1;
                if (pageCount != null) back -= pageCount.length();
                out.println("<HTML><HEAD><SCRIPT>");
                out.print("history.go("+back+");");
                out.println("</SCRIPT></HEAD><BODY></BODY></HTML>");
            }
        } catch (MalformedURLException mue) {
            System.out.println("Exception: " + mue);
            displayNeedInfoForm(filename, result, false, null);
        }
    }

    /** Copy a file. */
    private boolean copyFile(File template, String templateURL, File result) {
        if (template == result) return true;
        if (templateURL == null && !template.isFile()) return true;
        try {
            InputStream in = openInput(template, templateURL);
            // Should we read some flag in the file to decide whether
            // or not to use an InterpolatingFilter?
            in = new InterpolatingFilter(in, getDataRepository(), getPrefix());
            OutputStream out = new FileOutputStream(result);
            copyFile(in, out);
            return true;
        } catch (IOException ioe) { }
        return false;
    }

    private InputStream openInput(File template, String templateURL)
        throws IOException
    {
        if (templateURL != null)
            return new ByteArrayInputStream(getRequest(templateURL, true));
        else
            return new FileInputStream(template);
    }


    /** Copy a file. */
    private void copyFile(InputStream in, OutputStream out)
        throws IOException
    {
        byte [] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
            out.write(buffer, 0, bytesRead);
        in.close();
        out.close();
    }

    private boolean isTemplateURL(File f) {
        if (f == null) return false;
        String path = f.getPath();
        return (path.startsWith(TEMPLATE_ROOT_WIN) ||
                path.startsWith(TEMPLATE_ROOT_UNIX));
    }

    private boolean foundTemplate, isDirectory;
    private Map pathVariables, savedPathVariables, metaPathVariables;
    private ArrayList pathVariableNames, savedPathVariableNames,
        metaPathVariableNames;

    /** Backup the pathVariable settings in case we need them later. */
    private void savePathInfo() {
         savedPathVariables = pathVariables;
         savedPathVariableNames = pathVariableNames;
    }
    /** restore the pathVariable settings from the backup. */
    private void restorePathInfo() {
         pathVariables = savedPathVariables;
         pathVariableNames = savedPathVariableNames;
    }
    /** Were any path variables involved in the last computePath operation? */
    private boolean needPathInfo() {
        return !pathVariableNames.isEmpty();
    }

    /** Compute the path for the the file pointed to by <code>n</code> */
    private File computePath(Node n, boolean lookForTemplate) {
        if (n == null) {
            pathVariables = new HashMap();
            pathVariableNames = new ArrayList();
            metaPathVariables = new HashMap();
            metaPathVariableNames = new ArrayList();
            foundTemplate = false;
            return null;
        }

        // Compute the path for the parent of this node.
        File parentPath = computePath(n.getParentNode(), lookForTemplate);

        // see if this node has a path attribute.  If it doesn't, just
        // return the path computed by the parent.
        String pathVal = null;
        boolean isTemplate = false;
        if (n instanceof Element) {
            if (lookForTemplate) {
                pathVal = ((Element) n).getAttribute(TEMPLATE_PATH_ATTR);
                setTemplatePathVariables((Element) n);
            }
            if (XMLUtils.hasValue(pathVal)) {
                if ("none".equals(pathVal)) {
                    foundTemplate = isTemplate = false;
                    return null;
                } else
                    isTemplate = true;
            } else
                pathVal = ((Element) n).getAttribute(PATH_ATTR);
        }
        if (!XMLUtils.hasValue(pathVal))
            return parentPath;

        // Remember whether this item is a directory.
        isDirectory = DIRECTORY_TAG_NAME.equals(((Element) n).getTagName());

        // Look up any defaultPath instruction for this element.
        String defaultPath = ((Element) n).getAttribute(DEFAULT_PATH_ATTR);

        // Examine the path for data elements. backslash escapes in data
        // element names are NOT supported, so we can just break the
        // string apart on "[]" characters.
        StringTokenizer tok = new StringTokenizer(pathVal, "[]", true);
        StringBuffer path = new StringBuffer();
        String token;
        PathVariable pathVar = null;
        boolean unknownsPresent = false, firstItem = true;

        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if ("[".equals(token)) {
                token = tok.nextToken(); // get the name of the data element.
                tok.nextToken(); // discard the "]" following the element.
                // FIXME - catch exceptions, and also ensure that the
                // discarded token is in fact a "]" character.

                String impliedPath = null;
                if (firstItem && parentPath != null)
                    // if this variable is the first thing in the path
                    // expression, and our parent had a path, then the
                    // parent's path is implied for this variable.
                    impliedPath = parentPath.getPath();

                String defaultValue = null;
                if (firstItem && !tok.hasMoreTokens() &&
                    !isTemplate && XMLUtils.hasValue(defaultPath))
                    // if the path expression is comprised of this variable
                    // reference and nothing more, then the defaultPath is
                    // also the default value for this variable.  (This
                    // logic does NOT apply to templates - if a templatePath
                    // is composed of a lone variable, don't presume the
                    // default value.)
                    defaultValue = defaultPath;

                pathVar = getPathVariable(token, impliedPath, defaultValue);

                if (pathVar.isUnknown())
                    unknownsPresent = true;
                else
                    path.append(pathVar.getValue());
            } else {
                path.append(token);
            }

            firstItem = false;
        }
        String selfPath = path.toString();

        // if any data elements were not found, check to see if there is a
        // defaultPath attribute. If there is, use it. Otherwise return null.
        if (unknownsPresent) {
            if (!isTemplate && XMLUtils.hasValue(defaultPath))
                selfPath = defaultPath;
            else {
                foundTemplate = (foundTemplate || isTemplate);
                return null;
            }
        }

        // If we were able to successfully construct a path, check to see if
        // that path is absolute.  If it isn't, resolve it relative to the
        // parent path.
        File f = new File(selfPath);
        if (f.isAbsolute() || isTemplateURL(f))
            foundTemplate = isTemplate;
        else {
            foundTemplate = (foundTemplate || isTemplate);
            if (parentPath == null)
                f = null;
            else
                f = new File(parentPath, selfPath);
        }
        return f;
    }

    private void setTemplatePathVariables(Element n) {
        String attrVal = n.getAttribute(TEMPLATE_VAL_ATTR);
        if (!XMLUtils.hasValue(attrVal)) return;

        StringTokenizer values = new StringTokenizer(attrVal, ";");
        while (values.hasMoreTokens())
            setTemplatePathVariable(values.nextToken());
    }
    private void setTemplatePathVariable(String valSetting) {
        int bracePos = valSetting.indexOf(']');
        if (bracePos < 1) return;
        String valName = valSetting.substring(1, bracePos);
        int equalsPos = valSetting.indexOf('=', bracePos);
        if (equalsPos == -1) return;
        String setting = valSetting.substring(equalsPos+1).trim();
        PathVariable pathVar = getPathVariable(valName);
        pathVar.dataName = null;
        pathVar.value = setting;
    }



    /** Lookup a cached PathVariable, or create one if no cached one exists.
     */
    private PathVariable getPathVariable(String mname, String impliedPath,
                                         String defaultValue) {
        String name = resolveMetaReferences(mname);
        PathVariable result = (PathVariable) pathVariables.get(name);
        if (result == null) {
            result = new PathVariable(name, mname, impliedPath, defaultValue);
            pathVariables.put(name, result);
            pathVariableNames.add(name);
        }
        return result;
    }
    private PathVariable getPathVariable(String name) {
        return getPathVariable(name, null, null);
    }

    /** Resolve meta references within <code>name</code> */
    private String resolveMetaReferences(String name) {
        int beg, end;
        String metaName;
        while ((beg = name.indexOf('{')) != -1) {
            end = name.indexOf('}', beg);
            // FIXME: error handling if end is -1

            metaName = name.substring(beg+1, end);
            PathVariable pv = (PathVariable) metaPathVariables.get(metaName);
            if (pv == null)
                pv = new PathVariable(metaName, metaName, null, "");
            if (pv.isUnknown()) {
                metaPathVariables.put(metaName, pv);
                metaPathVariableNames.add(metaName);
            }
            name = name.substring(0,beg) +pv.getValue()+ name.substring(end+1);
        }
        // canonicalize whitespace, cleaning up problems with that may
        // arise during the interpolation of meta variables.
        name = name.replace('\t', ' ').replace('\n', ' ')
            .replace('\r', ' ').trim();
        while (name.indexOf("  ") != -1)
            name = StringUtils.findAndReplace(name, "  ", " ");
        return name;
    }


    /** Holds information about a data element referenced from a path.
     */
    class PathVariable {
        String metaName, dataName, value = null;

        public PathVariable(String name, String impliedPath) {
            this(name, name, impliedPath, null); }
        public PathVariable(String name) {
            this(name, name, null, null); }
        public PathVariable(String name, String metaName, String impliedPath,
                            String defaultValue) {
            this.metaName = metaName;
            SaveableData val = null;
            DataRepository data = getDataRepository();

            if (name.startsWith("/")) {
                // The name is absolute - look it up in the data repository.
                val = data.getSimpleValue(dataName = name);

            } else {
                // Look for an inheritable value with this name in the data
                // repository.
                StringBuffer prefix = new StringBuffer(getPrefix());
                val = data.getInheritableValue(prefix, name);
                if (val != null && !(val instanceof SimpleData))
                    val = val.getSimpleValue();
                dataName = data.createDataName(prefix.toString(), name);
            }

            // Check to see if a value was POSTed to this CGI script for this
            // data element.  If so, it would override any previous value.
            String postedValue = getParameter(name);
            if (postedValue != null) {
                value = postedValue;
                if (pathStartsWith(value, impliedPath)) {
                    // the user supplied an absolute path.  Rewrite it so it
                    // is relative to the impliedPath.
                    value = value.substring(impliedPath.length());
                    if (value.startsWith(File.separator))
                        value = value.substring(1);
                }
                if (! pathEqual(value, defaultValue)) {
                    // Save this user-specified value in the repository.
                    // (Default values are not saved to the repository.)
                    data.putValue(dataName, StringData.create(value));
                }

            } else if (val instanceof SimpleData)
                value = ((SimpleData) val).format();

            if (isUnknown() && defaultValue != null)
                value = defaultValue;
        }

        public String getDataname() { return dataName; }
        private String getValue()   { return value;    }
        private boolean isUnknown() {
            return (value == null || value.length() == 0 ||
                    value.indexOf('?') != -1); }

        private boolean pathStartsWith(String path, String prefix) {
            if (path == null || prefix == null) return false;
            if (path.length() < prefix.length()) return false;
            return pathEqual(path.substring(0, prefix.length()), prefix);
        }
        private boolean pathEqual(String a, String b) {
            if (a == null || b == null) return false;
            File aa = new File(a), bb = new File(b);
            return aa.equals(bb);
        }
        private String displayName = null;
        private String commentText = null;
        public void lookupExtraInfo(Element e) {
            if (e == null) return;
            Document doc = e.getOwnerDocument();
            if (doc == null) return;

            e = (new FileFinder("[" + metaName + "]", doc)).file;
            if (e != null) {
                displayName = e.getAttribute("displayName");
                if (e.hasChildNodes()) {
                    StringBuffer buf = new StringBuffer();
                    NodeList list = e.getChildNodes();
                    for (int i=0;  i<list.getLength(); i++)
                        buf.append(list.item(i).toString());
                    commentText = buf.toString();
                }
            }
        }
        public String getDisplayName() { return displayName; }
        public String getCommentText() { return commentText; }
    }




    /** Display an error message, stating that the XML document list does
     * not contain any file with the requested name.
     */
    private void sendNoSuchFileMessage(String filename) {
        out.print
            ("Content-type: text/html\r\n\r\n" +
             "<html><head><title>No such file</title></head>\n" +
             "<body><h1>No such file</h1>\n" +
             "None of the document description files\n" +
             "contain an entry for any file named &quot;");
        out.print(filename);
        out.print("&quot;.</body></html>");
    }

    private void sendCopyTemplateError(String message) {
        out.print
            ("Content-type: text/html\r\n\r\n" +
             "<html><head><title>Problem copying template</title></head>\n" +
             "<body><h1>Problem copying template</h1>\n");
        out.print(message);
        out.print("</body></html>");
    }

    /** When we are unable to locate a file, display a form requesting
     * information from the user.
     */
    private void displayNeedInfoForm(String filename, File file,
                                     boolean isTemplate, Element e) {
        out.print("Content-type: text/html\r\n\r\n" +
                  "<html><head><title>Enter File Information</title></head>\n"+
                  "<body><h1>Enter File Information</h1>\n");
        if (file != null) {
            out.print("The dashboard tried to find the ");
            out.print(isTemplate ? "<b>template</b>" : "file");
            out.print(" in the following location: <PRE>        ");
            out.print(file.getPath());
            out.println("</PRE>but no such file exists.<P>");
        }
        out.print("Please provide the following information to help locate " +
                  "the '");
        out.print(filename);
        out.println(isTemplate ? "' template." : "'.");

        out.print("<form method='POST' action='");
        out.print((String) env.get("SCRIPT_PATH"));
        out.println("'><table>");
        for (int i = 0;  i < pathVariableNames.size();   i++) {
            String varName = (String) pathVariableNames.get(i);
            PathVariable pathVar = getPathVariable(varName);
            if (pathVar.getDataname() == null)
                continue;
            pathVar.lookupExtraInfo(e);

            out.print("<tr><td valign='top'>");
            String displayName = pathVar.getDisplayName();
            if (!XMLUtils.hasValue(displayName)) displayName = varName;
            if (displayName.startsWith("/"))
                displayName = displayName.substring(1);
            out.print(TinyWebServer.encodeHtmlEntities(displayName));
            out.print("&nbsp;</td><td valign='top'>" +
                      "<input size='40' type='text' name='");
            out.print(TinyWebServer.encodeHtmlEntities(varName));
            String value = pathVar.getValue();
            if (value != null) {
                out.print("' value='");
                out.print(TinyWebServer.encodeHtmlEntities(value));
            }
            out.print("'>");
            String comment = pathVar.getCommentText();
            if (XMLUtils.hasValue(comment)) {
                out.print("<br><i>");
                out.print(comment);
                out.print("</i><br>&nbsp;");
            }
            out.println("</td></tr>");
        }
        out.println("</table>");
        String pageCount = getParameter(PAGE_COUNT_PARAM);
        pageCount = (pageCount == null ? "x" : pageCount + "x");
        out.print("<input type='hidden' name='"+PAGE_COUNT_PARAM+"' value='");
        out.print(pageCount);
        out.print("'>\n" +
                  "<input type='hidden' name='" + FILE_PARAM + "' value='");
        out.print(TinyWebServer.encodeHtmlEntities(filename));
        out.println("'>\n" +
                    "<input type='submit' name='OK' value='OK'>\n" +
                    "</form></body></html>");
    }

    protected static String docOpenSetting =
        Settings.getVal("extDoc.openMethod");


    /** an XML document describing the various files that can be served up
     *  by this CGI script.
     */
    protected static Hashtable documentMap = new Hashtable();

    protected Document getDocumentTree(String url) throws IOException {
        Document result = null;
        if (parameters.get("init") == null)
            result = (Document) documentMap.get(url);
        if (result == null)
            try {
                result = XMLUtils.parse
                    (new ByteArrayInputStream(getRequest(url, true)));
                documentMap.put(url, result);
            } catch (SAXException se) {
                //throw new IOException("Invalid XML file");
                // FIXME: display error message
                return null;
            }
        return result;
    }

    /** Find a file in the document list.
     * @param name the name of the file to find
     * @return the XML element corresponding to the named document.
     */
    protected Element findFile(String name) throws IOException {
        // Look for an inheritable value for the FILE_XML element in the
        // data repository.
        DataRepository data = getDataRepository();
        StringBuffer prefix = new StringBuffer(getPrefix());
        ListData list;
        Element result = null;
        SaveableData val;
        for (val = data.getInheritableValue(prefix, FILE_XML_DATANAME);
             val != null;
             val = data.getInheritableValue(chop(prefix), FILE_XML_DATANAME)) {

            if (val != null && !(val instanceof SimpleData))
                val = val.getSimpleValue();

            if (val instanceof StringData)
                list = ((StringData) val).asList();
            else if (val instanceof ListData)
                list = (ListData) val;
            else
                list = null;

            if (list != null)
                for (int i=0;   i < list.size();  i++) {
                    String url = (String) list.get(i);
                    Document docList = getDocumentTree(url);
                    if (docList != null) {
                        result = (new FileFinder(name, docList)).file;
                        if (result != null)
                            return result;
                    }
                }

            if (prefix.length() == 0)
                break;
        }

        return null;
    }

    private StringBuffer chop(StringBuffer buf) {
        int slashPos = buf.toString().lastIndexOf('/');
        buf.setLength(slashPos == -1 ? 0 : slashPos);
        return buf;
    }

    class FileFinder extends XMLDepthFirstIterator {
        String name;
        Element file = null;
        public FileFinder(String name, Document docTree) {
            this.name = name;
            run(docTree);
        }
        public void caseElement(Element e, List path) {
            if (name.equalsIgnoreCase(e.getAttribute(NAME_ATTR)) ||
                name.equalsIgnoreCase(e.getAttribute(TEMPLATE_PATH_ATTR)))
                file = e;
        }
    }
}
