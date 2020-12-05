// Copyright (C) 2001-2020 Tuma Solutions, LLC
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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.PropertyKeyHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.util.EscapeString;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HttpQueryParser;
import net.sourceforge.processdash.util.Perl5Util;
import net.sourceforge.processdash.util.PerlPool;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;



/** Class for performing server-side includes and other preprocessing on
 *  HTML files.
 */
public class HTMLPreprocessor {

    public static final String RESOURCES_PARAM = "RESOURCES";
    public static final String REPLACEMENTS_PARAM = "EAGERLY_REPLACE";

    ContentSource web;
    DataContext data;
    PropertyKeyHierarchy props;
    Map env, params;
    String prefix;
    boolean echoBareParams = true;
    boolean foreachParams = true;
    String defaultEchoEncoding = null;
    LinkedList resources;


    public HTMLPreprocessor(ContentSource web, DataContext data, Map env) {
        this(web,
             data,
             (PropertyKeyHierarchy) env.get(TinyCGI.PSP_PROPERTIES),
             (String) env.get("PATH_TRANSLATED"),
             env,
             parseParameters((String) env.get("QUERY_STRING")));
    }

    public HTMLPreprocessor(ContentSource web, DataContext data,
            PropertyKeyHierarchy props, String prefix, Map env, Map params) {
        this.web = web;
        this.data = data;
        this.props = props;
        this.prefix = (prefix == null ? "" : prefix);
        this.env = env;
        this.params = params;

        if (env.get(RESOURCES_PARAM) instanceof Resources) {
            resources = new LinkedList();
            resources.add(env.get(RESOURCES_PARAM));
        }
    }


    /** preprocess the given content, and return the result. */
    public String preprocess(String content) throws IOException {
        StringBuffer text = new StringBuffer(content);
        cachedTestExpressions.clear();
        maybePerformEagerReplacements(text);

        numberBlocks(text, "foreach", "endfor", null, null);
        numberBlocks(text, "fortree", "endtree", null, null);
        numberBlocks(text, "if", "endif", "else", "elif");
        numberBlocks(text, "replace", "endreplace", null, null);

        DirectiveMatch dir;
        int pos = 0;
        while ((dir = new DirectiveMatch(text, "", pos, true)).matches()) {
            if ("echo".equals(dir.directive))
                processEchoDirective(dir);
            else if ("include".equals(dir.directive))
                processIncludeDirective(dir);
            else if (blockMatch("foreach", dir.directive))
                processForeachDirective(dir);
            else if (blockMatch("if", dir.directive))
                processIfDirective(dir);
            else if ("incr".equals(dir.directive))
                processIncrDirective(dir);
            else if ("set".equals(dir.directive))
                processSetDirective(dir);
            else if ("break".equals(dir.directive))
                processBreakDirective(dir);
            else if (blockMatch("replace", dir.directive))
                processReplaceDirective(dir);
            else if (blockMatch("fortree", dir.directive))
                processForTreeDirective(dir);
            else if ("resources".equals(dir.directive))
                processResourcesDirective(dir);
            else
                dir.replace("");
            pos = dir.end;
        }
        return text.toString();
    }


    private void maybePerformEagerReplacements(StringBuffer text) {
        if (env.get(REPLACEMENTS_PARAM) instanceof Map) {
            Map replacements = (Map) env.get(REPLACEMENTS_PARAM);
            for (Iterator i = replacements.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String find = (String) e.getKey();
                String replace = (String) e.getValue();
                StringUtils.findAndReplace(text, find, replace);
            }
        }
    }

    /** process an include directive within the buffer */
    private void processIncludeDirective(DirectiveMatch include)
        throws IOException
    {
        // what file do they want us to include?
        String url = include.getAttribute("file");
        if (isNull(url))
            include.replace(""); // no file specified - delete this directive.
        else {
            url = appendFileParameters(url, include);

            // fetch the requested url (relative to the current url) and
            // replace the include directive with its contents.
            String context = (String) env.get("REQUEST_URI");
            String incText = new String
                (web.getContent(context, url, false), "UTF-8");

            // If the page author wants us to extract only a certain piece of
            // the file, discard the rest.
            incText = cutHtml(incText, include.getAttribute("cutToken"));

            // does the page author want us to parse the included text
            // for directives, or just insert it verbatim?
            if (include.getAttribute("parse") != null) {
                // parse the insertion.
                include.replace("");
                include.buf.insert(include.end, incText);
            } else {
                // insert it verbatim (default);
                include.replace(incText);
            }
        }
    }

    private String appendFileParameters(String url, DirectiveMatch include) {
        while (hasTrailingParam(include)) {
            DirectiveMatch param = new DirectiveMatch(include.buf, "parameter",
                    include.end, true);
            // if we don't find a "parameter" directive, we're done.
            if (!param.matches())
                return url;
            // if there is anything but whitespace between the end of the include
            // directive and the beginning of the parameter directive, then the
            // parameter doesn't belong to us, and we're done.
            if (!StringUtils.isWhiteSpace(include.buf.subSequence(include.end,
                    param.begin)))
                return url;

            include.buf.replace(include.end, param.end, "");

            String paramName = param.getAttribute("name");
            if (paramName == null || paramName.length() == 0)
                continue;

            if ("query_string".equalsIgnoreCase(paramName))
                url = appendParam(url, (String) env.get("QUERY_STRING"));

            else {
                String query = HTMLUtils.urlEncode(paramName);

                String encoding = param.getAttribute("encoding");
                if (encoding == null)
                    param.attributes.put("encoding", "url");
                else if (encoding.indexOf("url") == -1)
                    param.attributes.put("encoding", encoding + ",url");
                String text = getEchoText(param);
                if (StringUtils.hasValue(text))
                    query = query + '=' + text;

                url = appendParam(url, query);
            }
        }
        return url;
    }
    private boolean hasTrailingParam(DirectiveMatch include) {
        StringBuffer buf = include.buf;
        int i = include.end;
        while (true) {
            if (i >= buf.length())
                return false;

            char c = buf.charAt(i);
            if (Character.isWhitespace(c))
                i++;
            else if (c == '<')
                break;
            else
                return false;
        }
        int end = i + PARAM_DIRECTIVE.length();
        return (end <= buf.length() && StringUtils.equals(PARAM_DIRECTIVE, buf
                .subSequence(i, end)));
    }
    private static final String PARAM_DIRECTIVE = "<!--#parameter";
    private String appendParam(String url, String params) {
        if (!StringUtils.hasValue(params))
            return url;

        if ("?&".indexOf(params.charAt(0)) != -1)
            params = params.substring(1);
        if (url.indexOf('?') == -1)
            return url + '?' + params;
        else
            return url + '&' + params;
    }
    private String cutHtml(String text, String cutToken) {
        if (cutToken != null) {
            cutToken = cutToken.trim();
            if ("none".equalsIgnoreCase(cutToken))
                cutToken = "";
            else if (cutToken.length() > 0)
                cutToken = "\\Q:" + cutToken + "\\E";

            Pattern begToken = Pattern.compile("<!--\\s*cutStart" + cutToken
                    + "\\s*-->");
            Matcher m = begToken.matcher(text);
            if (m.find())
                text = text.substring(m.end());

            Pattern endToken = Pattern.compile("<!--\\s*cutEnd" + cutToken
                    + "\\s*-->");
            m = endToken.matcher(text);
            if (m.find())
                text = text.substring(0, m.start());
        }

        return text;
    }



    /** process an echo directive within the buffer */
    private void processEchoDirective(DirectiveMatch echo) {
        String value = getEchoText(echo);

        // replace the echo directive with the resulting value.
        echo.replace(value);
    }

    /** Get the effective text specified by an echo-like directive */
    private String getEchoText(DirectiveMatch echo) {
        // Determine the requested encoding(s) for this directive
        updateEchoPrefs(echo);
        String encodings = echo.getAttribute("encoding");
        if (encodings == null)
            encodings = defaultEchoEncoding;
        boolean bareParamsOK = echoBareParams && (encodings != null
                && !encodings.equalsIgnoreCase("none"));

        String value;
        // Was an explicit value specified? (This is used for performing
        // encodings on strings, and is especially useful when the string
        // in question came from interpolating a foreach statement.)
        value = echo.getAttribute("value");

        if (isNull(value)) {
            // was a variable name specified? If so, look up the associated
            // string value.
            String var = echo.getAttribute("var");
            if (isNull(var)) var = echo.contents.trim();
            value = (isNull(var) ? "" : getString(var, bareParamsOK));
        }

        if (!isNull(echo.getAttribute("arg0"))) {
            LinkedList args = new LinkedList();
            int argNum = 0;
            while (true) {
                String arg = echo.getAttribute("arg" + argNum);
                if (arg == null) break;

                if (arg.startsWith("'"))
                    arg = cleanup(arg);
                else
                    arg = getString(arg, bareParamsOK);
                args.add(arg);
                argNum++;
            }
            try {
                value = MessageFormat.format(value, args.toArray());
            } catch (Exception e) {
                System.out.println("Bad message format: '"+value+"'");
            }
        }

        // Apply the requested encoding(s)
        String context = (String) env.get("REQUEST_URI");
        value = applyEncodings(value, encodings, context);
        return value;
    }
    private void updateEchoPrefs(DirectiveMatch echo) {
        // update the parameter echoing flag
        String newEchoBareParams = echo.getAttribute("bareParams");
        if ("true".equalsIgnoreCase(newEchoBareParams))
            echoBareParams = true;
        else if (newEchoBareParams != null)
            echoBareParams = false;

        // update the default echo encoding
        String newDefaultEncoding = echo.getAttribute("defaultEncoding");
        if (newDefaultEncoding != null)
            defaultEchoEncoding = newDefaultEncoding;
    }
    /** @since 2.4.3 */
    public void setEchoBareParams(boolean echoParams) {
        this.echoBareParams = echoParams;
    }
    public void setDefaultEchoEncoding(String enc) {
        defaultEchoEncoding = enc;
    }
    public static String applyEncodings(String value, String encodings) {
        return applyEncodings(value, encodings, "/");
    }
    private static String applyEncodings(String value, String encodings,
            String baseUri) {
        if (encodings == null || "none".equalsIgnoreCase(encodings))
            return value;
        StringTokenizer tok = new StringTokenizer(encodings, ",");
        String encoding;
        while (tok.hasMoreTokens()) {
            encoding = tok.nextToken();

            if ("url".equalsIgnoreCase(encoding)) {
                // url encode the value
                value = HTMLUtils.urlEncode(value);
                value = StringUtils.findAndReplace(value, "%2F", "/");
                value = StringUtils.findAndReplace(value, "%2f", "/");
            } else if ("xml".equalsIgnoreCase(encoding))
                // encode the value as an xml entity
                value = XMLUtils.escapeAttribute(value);
            else if ("data".equalsIgnoreCase(encoding))
                // this code must be kept in sync with AutoData
                value = EscapeString.escape(value, '\\', "'\"[]");
            else if ("dir".equalsIgnoreCase(encoding))
                value = dirEncode(value);
            else if ("javaStr".equalsIgnoreCase(encoding))
                value = StringUtils.javaEncode(value);
            else if ("translate".equalsIgnoreCase(encoding))
                value = Translator.translate(value);
            else if ("relUri".equalsIgnoreCase(encoding))
                value = resolveRelativeURI(baseUri, value);
            else
                // default: HTML entity encoding
                value = HTMLUtils.escapeEntities(value);
        }

        return value;
    }


    private static String resolveRelativeURI(String baseUri, String value) {
        try {
            URL u = new URL("http://ignored" + baseUri);
            u = new URL(u, value);
            return u.getFile();
        } catch (Exception e) {
            return value;
        }
    }

    /** process a foreach directive within the buffer */
    private void processForeachDirective(DirectiveMatch foreach) {
        StringBuffer text = foreach.buf;
        String blockNum = blockNum("foreach", foreach.directive);
        // find the matching endfor.
        DirectiveMatch endfor = new DirectiveMatch
            (text, blockNum + "endfor", foreach.end, true);

        if (!endfor.matches()) {
            // if the endfor is missing, delete this directive and abort.
            System.err.println
                ("foreach directive without matching endfor - aborting.");
            foreach.replace("");
            return;
        }

        // get the list of values that we should iterate over
        ListData list = getForeachListValues(foreach);

        // iterate over the list and calculate the resulting contents.
        String loopVar = foreach.getAttribute("name");
        String loopIndex = foreach.getAttribute("index");
        Integer loopLimit = getInteger(foreach, "limit");
        String loopContents = text.substring(foreach.end, endfor.begin);
        int loopEnd = list.size();
        if (loopLimit != null)
            loopEnd = Math.min(loopEnd, loopLimit);
        StringBuffer replacement = new StringBuffer();
        for (int i = 0;   i < loopEnd;   i++) {
            Object oneVal = list.get(i);
            String strVal = (oneVal == null ? "" : oneVal.toString());
            String iterResults = loopContents;
            if (loopVar != null)
                iterResults = StringUtils.findAndReplace(iterResults, //
                    loopVar, strVal);
            if (loopIndex != null)
                iterResults = StringUtils.findAndReplace(iterResults, //
                    loopIndex, Integer.toString(i));
            replacement.append(iterResults);
        }

        // replace the directive with the iterated contents.  Note
        // that we explicitly replace the initial foreach tag with an
        // empty string, so the overall processing loop (in the
        // preprocess method) will process these iterated contents.
        text.replace(foreach.end, endfor.end, replacement.toString());
        foreach.replace("");
    }

    private ListData getForeachListValues(DirectiveMatch foreach) {
        // if the directive included a literal list (specified with a "values"
        // attribute), parse that literal
        String values = foreach.getAttribute("values");
        if (!isNull(values))
            return new ListData(values);

        // if the directive included a "count" attribute, iterate numerically
        String countVar = foreach.getAttribute("count");
        if (!isNull(countVar)) {
            ListData result = new ListData();
            Integer count = getInteger(countVar);
            if (count != null) {
                for (int i = 0; i < count; i++)
                    result.add(i);
            }
            return result;
        }

        // look for a list variable, named via the "list" attribute
        String listName = foreach.getAttribute("list");
        return getList(listName);
    }

    /** process a fortree directive within the buffer */
    private void processForTreeDirective(DirectiveMatch fortree) {
        StringBuffer text = fortree.buf;
        String blockNum = blockNum("fortree", fortree.directive);
        // find the matching endtree.
        DirectiveMatch endtree = new DirectiveMatch
            (text, blockNum + "endtree", fortree.end, true);

        if (!endtree.matches()) {
            // if the endtree is missing, delete this directive and abort.
            System.err.println
                ("fortree directive without matching endtree - aborting.");
            fortree.replace("");
            return;
        }

        // determine the root prefix - possibly alter it based on the
        // directive's value for the startAt attribute.
        String rootPrefix = this.prefix;
        String startAt = fortree.getAttribute("startAt");
        if (startAt != null && startAt.length() > 0)
            rootPrefix = rootPrefix + "/" + startAt;
        PropertyKey rootNode = PropertyKey.fromPath(rootPrefix);

        // get the expandName from the directive.
        String expandName = fortree.getAttribute("expandName");

        // should the root node be included?
        boolean includeRoot =
            "true".equalsIgnoreCase(fortree.getAttribute("includeRoot"));

        // how deep should the iteration go?
        int maxDepth = Integer.MAX_VALUE;
        String depthStr = fortree.getAttribute("depth");
        if (depthStr != null) try {
            maxDepth = Integer.parseInt(depthStr);
        } catch (Exception e) {}

        // should the parent be displayed before or after children?
        boolean parentLast =
            "true".equalsIgnoreCase(fortree.getAttribute("parentLast"));

        // iterate over the tree and calculate the resulting contents.
        String loopContents = text.substring(fortree.end, endtree.begin);
        StringBuffer replacement = new StringBuffer();
        addSetDirective(replacement, "ROOT", rootPrefix);
        addSetDirective(replacement, "SPACER", SPACER);
        recurseTreeNode(replacement, loopContents, rootNode, 0, "",
                        expandName, includeRoot, maxDepth, parentLast);

        // replace the directive with the iterated contents.  Note
        // that we explicitly replace the initial fortree tag with an
        // empty string, so the overall processing loop (in the
        // preprocess method) will process these iterated contents.
        text.replace(fortree.end, endtree.end, replacement.toString());
        fortree.replace("");
    }

    private void addSetDirective(StringBuffer buf, String varName,
                                 String value) {
        buf.append(DIRECTIVE_START).append("set")
            .append(" var=\"").append(varName);
        if (value != null)
            buf.append("\" value=\"").append(dirEncode(value));
        buf.append("\" ").append(DIRECTIVE_END);
    }

    private void outputTreeNode(StringBuffer buf, String loopContents,
                                PropertyKey node, int depth, String relPath,
                                String expansionName, boolean isLeaf,
                                boolean isExpanded)
    {
        addSetDirective(buf, "PATH", node.path());
        if (relPath.length() == 0) {
            addSetDirective(buf, "RELPATH", "");
            addSetDirective(buf, "RELPATHLIST", null);
        } else {
            addSetDirective(buf, "RELPATH", relPath.substring(1));
            addSetDirective(buf, "RELPATHLIST", "LIST=" + relPath + "/");
        }
        addSetDirective(buf, "NAME", node.name());
        addSetDirective(buf, "DEPTH", Integer.toString(depth));
        addSetDirective(buf, "DEPTH_SPACER", makeDepthSpacer(depth));
        addSetDirective(buf, "ISLEAF", isLeaf ? "true" : null);
        if (expansionName != null) {
            addSetDirective(buf, "EXPANSIONNAME", expansionName);
            addSetDirective(buf, "ISEXPANDED", isExpanded ? "true" : null);
            addSetDirective(buf, "EXPANDLINK",
                            makeExpansionLink(expansionName, isLeaf,
                                              isExpanded));
        }
        buf.append(loopContents);
    }

    private void recurseTreeNode(StringBuffer buf, String loopContents,
                                 PropertyKey node, int depth,
                                 String relPath, String expandName,
                                 boolean outputNode, int remainingDepth,
                                 boolean parentLast)
    {
        boolean isLeaf = (props.getNumChildren(node) == 0);
        String expansionName = makeExpansionName(relPath, expandName);
        boolean isExpanded = (expandName == null ||
                              outputNode == false ||
                              testDataElem("["+expansionName+"]"));


        if (outputNode && !parentLast)
            outputTreeNode(buf, loopContents, node, depth, relPath,
                           expansionName, isLeaf, isExpanded);

        if (remainingDepth > 0 && isExpanded) {
            int numKids = props.getNumChildren(node);
            for (int i = 0;   i < numKids;  i++) {
                PropertyKey child = props.getChildKey(node, i);
                recurseTreeNode(buf, loopContents, child, depth+1,
                                relPath+"/"+child.name(), expandName,
                                true, remainingDepth-1, parentLast);
            }
        }

        if (outputNode && parentLast)
            outputTreeNode(buf, loopContents, node, depth, relPath,
                           expansionName, isLeaf, isExpanded);
    }
    private String makeDepthSpacer(int depth) {
        boolean isExcel = "excel".equals(params.get("EXPORT"));
        StringBuffer result = new StringBuffer();
        while (depth-- > 0)
            result.append(isExcel ? EXCEL_SPACER : SPACER);
        return result.toString();
    }
    private String makeExpansionName(String relPath, String expandName) {
        if (expandName == null) return null;
        return expandName + relPath;
    }
    private String makeExpansionLink(String expansionName, boolean isLeaf,
                                     boolean isExpanded) {
        if (isLeaf) return LEAF_LINK;
        String dataName = prefix + "/" + expansionName;
        String anchor = "exp_" + dataName.hashCode();
        dataName = HTMLUtils.urlEncode(dataName);
        String result = StringUtils.findAndReplace
            (isExpanded ? COLLAPSE_LINK : EXPAND_LINK, "%%%", dataName);
        result = StringUtils.findAndReplace(result, "###", anchor);
        return result;
    }
    private static final String SPACER =
        "<img width=9 height=9 src='/Images/blank.png'>";
    private static final String EXCEL_SPACER = "&nbsp;&nbsp;&nbsp;";
    private static final String LEAF_LINK = SPACER;
    private static final String ANCHOR_TEXT = "<a name='###'></a>";
    private static final String COLLAPSE_LINK =
        "<a border=0 href='/dash/expand.class?collapse=%%%'>"+
        "<img border=0 width=9 height=9 src='/Images/minus.png'></a>"+
        ANCHOR_TEXT;
    private static final String EXPAND_LINK =
        "<a border=0 href='/dash/expand.class?expand=%%%'>"+
        "<img border=0 width=9 height=9 src='/Images/plus.png'></a>"+
        ANCHOR_TEXT;



    /** process an if directive within the buffer */
    private void processIfDirective(DirectiveMatch ifdir) {
        processIfDirective(ifdir, blockNum("if", ifdir.directive));
    }
    private void processIfDirective(DirectiveMatch ifdir, String blockNum) {
        StringBuffer text = ifdir.buf;
        // find the matching endif.
        DirectiveMatch endif = new DirectiveMatch
            (text, blockNum + "endif", ifdir.end, true);

        if (!endif.matches()) {
            // if the endif is missing, delete this directive and abort.
            System.err.println
                ("if directive without matching endif - aborting.");
            ifdir.replace("");
            return;
        }

        // See if there was an elif or an else.
        DirectiveMatch elsedir = new DirectiveMatch
            (text, blockNum + "elif", ifdir.end, true);
        if (!elsedir.matches() || elsedir.begin > endif.begin)
            elsedir = new DirectiveMatch
                (text, blockNum + "else", ifdir.end, true);
        if (elsedir.matches() && elsedir.begin > endif.begin)
            elsedir.begin = -1;

                                // if this is an else clause
        if (blockMatch("else", ifdir.directive) ||
                                // or if the test expression was true,
            ifTest(ifdir.contents))
        {
            endif.replace("");  // delete the endif
            if (elsedir.matches()) // delete the entire else clause if present
                text.replace(elsedir.begin, endif.begin, "");
            ifdir.replace("");  // delete the if directive.
        } else if (elsedir.matches()) {
            // if the test was false, and there was an else clause, evaluate
            // the else clause as its own if statement.
            processIfDirective(elsedir, blockNum);
            // then delete the "true" clause (the text between the if
            // and the else)
            text.replace(ifdir.end, elsedir.begin, "");
            // finally, delete the if directive itself.
            ifdir.replace("");
        } else {
            // if the test was false and there was no else clause, delete
            // everything.
            text.replace(ifdir.end, endif.end, "");
            ifdir.replace("");
        }
    }

    Map cachedTestExpressions = new HashMap();
    private boolean ifTest(String expression) {
        expression = expression.replace('\n',' ').replace('\t',' ').trim();
        Boolean result = (Boolean) cachedTestExpressions.get(expression);
        if (result == null) {
            boolean test = false;
            boolean reverse = false;
            boolean containsVolatileVar = false;

            // if the expression contains multiple OR clauses, evaluate
            // them individually and return true if one is true.
            int orPos = expression.indexOf(" || ");
            if (orPos != -1) {
                expression = expression + " || ";
                String subExpr;
                while (orPos != -1) {
                    subExpr = expression.substring(0, orPos);
                    if (ifTest(subExpr)) return true;
                    expression = expression.substring(orPos+4);
                    orPos = expression.indexOf(" || ");
                }
                return false;
            }

            // if the expression contains multiple AND clauses, evaluate
            // them individually and return false if one is false.
            int andPos = expression.indexOf(" && ");
            if (andPos != -1) {
                expression = expression + " && ";
                String subExpr;
                while (andPos != -1) {
                    subExpr = expression.substring(0, andPos);
                    if (!ifTest(subExpr)) return false;
                    expression = expression.substring(andPos+4);
                    andPos = expression.indexOf(" && ");
                }
                return true;
            }

            RelationalExpression re = parseRelationalExpression(expression);
            if (re != null) {
                test = re.test();
                containsVolatileVar = re.containsVolatileVar;
            } else {
                String symbolName = cleanup(expression);
                if (symbolName.startsWith("not") &&
                    whitespacePos(symbolName) == 3) {
                    reverse = true;
                    symbolName = cleanup(symbolName.substring(4));
                } else if (symbolName.startsWith("!")) {
                    reverse = true;
                    symbolName = cleanup(symbolName.substring(1));
                }
                boolean checkDefined = false;
                if (symbolName.startsWith("defined") &&
                    whitespacePos(symbolName) == 7) {
                    checkDefined = true;
                    symbolName = cleanup(symbolName.substring(8));
                }

                if (volatileVariables.contains(symbolName))
                    containsVolatileVar = true;

                if (!isNull(symbolName))
                    test = (symbolName.startsWith("[") ?
                            testDataElem(symbolName, checkDefined) :
                            !isNull(getString(symbolName, true)));
                if (reverse)
                    test = !test;
            }
            result = test ? Boolean.TRUE : Boolean.FALSE;
            if (!containsVolatileVar)
                cachedTestExpressions.put(expression, result);
        }
        return result.booleanValue();
    }

    private class RelationalExpression {
        public String lhs, operator, rhs;
        public boolean containsVolatileVar = false;
        public boolean test() {
            if (lhs.length() == 0 || rhs.length() == 0) {
                System.err.println
                    ("malformed relational expression - aborting.");
                return false;
            }
            String lhval = getVal(lhs);
            String rhval = getVal(rhs);

            if ("eq".equals(operator)) return eq(lhval, rhval);
            if ("ne".equals(operator)) return !eq(lhval, rhval);
            if ("=~".equals(operator)) return matches(lhval, rhval);
            if ("!~".equals(operator)) return !matches(lhval, rhval);
            if ("gt".equals(operator)) return gt(lhval, rhval);
            if ("lt".equals(operator)) return lt(lhval, rhval);
            if ("ge".equals(operator))
                return gt(lhval, rhval) || eq(lhval, rhval);
            if ("le".equals(operator))
                return lt(lhval, rhval) || eq(lhval, rhval);
            return false;
        }
        private String getVal(String t) {
            if (t.startsWith("'")) return cleanup(t);
            t = cleanup(t);
            if (volatileVariables.contains(t)) containsVolatileVar = true;
            return getString(cleanup(t), true);
        }
        private boolean eq(String l, String r) {
            if (l == null && r == null) return true;
            if (l == null || r == null) return false;
            try {
                double ll = Double.parseDouble(l);
                double rr = Double.parseDouble(r);
                return (ll == rr);
            } catch (NumberFormatException nfe) {}

            if (r.startsWith(VERSION_PREFIX))
                return DashPackage.compareVersions(l,
                        r.substring(VERSION_PREFIX.length())) == 0;

            return l.equals(r);
        }
        private boolean lt(String l, String r) {
            if (l == null || r == null) return false;
            try {
                double ll = Double.parseDouble(l);
                double rr = Double.parseDouble(r);
                return (ll < rr);
            } catch (NumberFormatException nfe) {}

            if (r.startsWith(VERSION_PREFIX))
                return DashPackage.compareVersions(l,
                        r.substring(VERSION_PREFIX.length())) < 0;

            return (l.compareTo(r) < 0);
        }
        private boolean gt(String l, String r) {
            if (l == null || r == null) return false;
            try {
                double ll = Double.parseDouble(l);
                double rr = Double.parseDouble(r);
                return (ll > rr);
            } catch (NumberFormatException nfe) {}

            if (r.startsWith(VERSION_PREFIX))
                return DashPackage.compareVersions(l,
                        r.substring(VERSION_PREFIX.length())) > 0;

            return (l.compareTo(r) > 0);
        }
        private boolean matches(String l, String r) {
            if (l == null || r == null) return false;
            boolean result = false;
            Perl5Util perl = null;
            try {
                String re = "m\n" + r + "\n";
                perl = PerlPool.get();
                result = perl.match(re, l);
            } catch (Exception e) {
            } finally {
                PerlPool.release(perl);
            }
            return result;
        }
    }
    private RelationalExpression parseRelationalExpression(String expr) {
        if (expr == null) return null;
        expr = expr.replace('\n', ' ').replace('\t', ' ');
        int pos = expr.indexOf(" eq ");
        if (pos == -1) pos = expr.indexOf(" ne ");
        if (pos == -1) pos = expr.indexOf(" =~ ");
        if (pos == -1) pos = expr.indexOf(" !~ ");
        if (pos == -1) pos = expr.indexOf(" lt ");
        if (pos == -1) pos = expr.indexOf(" le ");
        if (pos == -1) pos = expr.indexOf(" gt ");
        if (pos == -1) pos = expr.indexOf(" ge ");
        if (pos == -1) return null;

        RelationalExpression result = new RelationalExpression();
        result.lhs = expr.substring(0, pos).trim();
        result.operator = expr.substring(pos+1, pos+3);
        result.rhs = expr.substring(pos+4).trim();
        return result;
    }

    private HashSet volatileVariables = new HashSet();
    private void processIncrDirective(DirectiveMatch incrDir) {
        String varName = cleanup(incrDir.contents);
        int numberValue = 0;

        Object param = params.get(varName);
        if (param instanceof String) try {
            numberValue = Integer.parseInt((String) param) + 1;
        } catch (NumberFormatException nfe) {}
        params.put(varName, Integer.toString(numberValue));
        volatileVariables.add(varName);

        incrDir.replace("");
    }

    /** process a set directive within the buffer */
    private void processSetDirective(DirectiveMatch setDir) throws IOException {
        String varName = setDir.getAttribute("var");
        String valueName = setDir.getAttribute("value");

        if (valueName == null && setDir.getAttribute("inline") != null) {
            DirectiveMatch setEnd = new DirectiveMatch
                (setDir.buf, "endset", setDir.end, true);
            if (setEnd.matches()) {
                valueName = preprocess(setDir.buf.substring(setDir.end,
                        setEnd.begin));
                setDir.buf.replace(setDir.end, setEnd.end, "");
            }
        }

        params.put(varName, valueName);
        volatileVariables.add(varName);

        setDir.replace("");
    }

    /** process a break directive within the buffer */
    private void processBreakDirective(DirectiveMatch breakDir) {
        String label = cleanup(breakDir.contents);
        DirectiveMatch breakEnd = new DirectiveMatch
            (breakDir.buf, "endbreak "+label, breakDir.end, true);

        if (!breakEnd.matches()) {
            // if the endbreak is missing, delete this directive and abort.
            System.err.println
                ("break directive without matching endbreak - aborting.");
            breakDir.replace("");
            return;
        }
        breakDir.buf.replace(breakDir.end, breakEnd.end, "");
        breakDir.replace("");
    }

    /** process a replace directive within the buffer */
    private void processReplaceDirective(DirectiveMatch replaceDir) throws IOException {
        String blockNum = blockNum("replace", replaceDir.directive);
        DirectiveMatch replaceEnd = new DirectiveMatch
            (replaceDir.buf, blockNum + "endreplace", replaceDir.end, true);
        if (!replaceEnd.matches()) {
            // if the endreplace is missing, delete this directive and abort.
            System.err.println
                ("replace directive without matching endreplace - aborting.");
            replaceDir.replace("");
            return;
        }

        // check for the presence of if/unless attributes and possibly do
        // nothing if they say so.
        String unlessExpr = replaceDir.getAttribute("unless");
        String ifExpr = replaceDir.getAttribute("if");
        if ((unlessExpr != null && ifTest(cleanup(unlessExpr)) == true)
                || (ifExpr != null && ifTest(cleanup(ifExpr)) == false)) {
            replaceEnd.replace("");
            replaceDir.replace("");
            return;
        }

        // find the token or regular expression the user wants to replace
        String regexp = replaceDir.getAttribute("regexp");
        String token = replaceDir.getAttribute("token");
        if (regexp == null && token == null) {
            System.err.println
            ("replace directive without token or regexp - aborting.");
            replaceEnd.replace("");
            replaceDir.replace("");
            return;
        }

        // what replacement text does the user want?  This supports all
        // the same attributes as the echo directive (value, var, encoding)
        String text = getEchoText(replaceDir);

        // does the user want this replacement to happen *after*
        // other processing has taken place?
        boolean post = (replaceDir.getAttribute("post") != null);

        // get the content that appears between the two matching directives.
        String content = replaceDir.buf.substring(replaceDir.end,
                replaceEnd.begin);

        // if the user wants replacement to happen after other processing,
        // make a recursive preprocess call to perform that other processing.
        if (post)
            content = preprocess(content);

        // perform token or regexp replacement, as applicable.
        if (token != null)
            content = StringUtils.findAndReplace(content, cleanup(token), text);
        else if (regexp != null)
            content = content.replaceAll(cleanup(regexp), text);

        // place the resulting content into the buffer
        if (post) {
            // insert the content in such a way that processing will pick up
            // with the character *after* the already processed content.
            replaceDir.buf.replace(replaceDir.end, replaceEnd.end, "");
            replaceDir.replace(content);
        } else {
            // insert the content in such a way that subsequent processing will
            // start with the first character of the new content.
            replaceDir.buf.replace(replaceDir.end, replaceEnd.end, content);
            replaceDir.replace("");
        }
    }

    /** process a resources directive within the buffer */
    private void processResourcesDirective(DirectiveMatch resDir)
        throws IOException
    {
        Resources r = null;

        // what bundle do they want us to include?
        String bundle = resDir.getAttribute("bundle");
        if (bundle != null) {
            try {
                r = Resources.getDashBundle(bundle);
            } catch (MissingResourceException mre) {
                throw new FileNotFoundException(bundle + ".properties");
            }

        } else {
            // what file do they want us to include?
            String url = resDir.getAttribute("file");
            if (isNull(url))
                url = resDir.contents;
            if (!isNull(url)) {
                // fetch the requested url (relative to the current url) and
                // replace the include directive with its contents.
                String context = (String) env.get("REQUEST_URI");
                int pos = context.indexOf("//");
                if (pos != -1)
                    context = context.substring(pos+1);
                URL tempURL = new URL("http://ignored" + context);
                tempURL = new URL(tempURL, url);
                url = tempURL.getFile();
                String resName =
                    url.substring(1).replace('.', '$').replace('/', '.');
                try {
                    r = Resources.getTemplateBundle(resName);
                } catch (MissingResourceException mre) {
                    throw new FileNotFoundException(url + ".properties");
                }
            }
        }

        if (r != null) {
            if (resources == null) resources = new LinkedList();
            resources.add(r);
        }
        resDir.replace("");
    }

    /** search for blocks created by matching start and end directives, and
     * give them unique numerical prefixes so it will be easy to figure out
     * which start directive goes with which end directive.  This handles
     * nested blocks correctly.
     */
    private void numberBlocks(StringBuffer text,
                              String blockStart, String blockFinish,
                              String blockMid1, String blockMid2) {
        DirectiveMatch start, finish;
        int blockNum = 0;
        String prefix;

        while ((finish = new DirectiveMatch(text, blockFinish)).matches()) {
            start = new DirectiveMatch(text, blockStart, finish.begin, false);
            if (!start.matches()) break;
            prefix = "0" + blockNum++;

            finish.rename(prefix + blockFinish);

            int end = finish.begin;
            if (!isNull(blockMid1))
                end = renameDirectives(text, start.end, end,
                                       blockMid1, prefix + blockMid1);
            if (!isNull(blockMid2))
                end = renameDirectives(text, start.end, end,
                                       blockMid2, prefix + blockMid2);

            start.rename(prefix + blockStart);
        }
    }

    /** Find all directives with the given name in text, starting at
     * position <code>from</code> and going to position <code>to</code>,
     * and rename them to newname.
     */
    private int renameDirectives(StringBuffer text, int from, int to,
                                 String name, String newName) {
        DirectiveMatch dir;
        int delta = newName.length() - name.length();

        while ((dir = new DirectiveMatch(text, name, from, true)).matches() &&
               dir.begin < to) {
            dir.rename(newName);
            from = dir.end;
            to += delta;
        }
        return to;
    }


    /** trim whitespace and unimportant delimiters from t */
    private static String cleanup(String t) {
        t = t.trim();
        if (t.length() == 0) return t;
        if (t.charAt(0) == '"' || t.charAt(0) == '\'') {
            int endPos = t.indexOf(t.charAt(0), 1);
            if (endPos != -1) t = t.substring(1, endPos);
        }
        return dirUnencode(t);
    }

    private static String dirEncode(String s) {
        if (s == null) return null;
        for (int i = s.length();   i-- > 0; )
            // look for unsafe characters in the string.
            if (-1 == SAFE_CHARS.indexOf(s.charAt(i)))
                // if we find an unsafe character, encode the entire value.
                return DIR_ENC_BEG + HTMLUtils.urlEncode(s) + DIR_ENC_END;
        return s;
    }
    private static final String SAFE_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789" +
        "/_,. "; // Note: space char IS included
    private static final String DIR_ENC_BEG = "%(";
    private static final String DIR_ENC_END = "%)";
    private static String dirUnencode(String s) {
        if (s == null) return null;
        int beg = s.indexOf(DIR_ENC_BEG);
        while (beg != -1) {
            int end = s.indexOf(DIR_ENC_END, beg);
            if (end == -1) break;
            String decoded =
                HTMLUtils.urlDecode(s.substring(beg+DIR_ENC_BEG.length(), end));
            s = s.substring(0, beg) +
                decoded +
                s.substring(end+DIR_ENC_END.length());

            beg = s.indexOf(DIR_ENC_BEG, beg + decoded.length());
        }
        return s;
    }


    /** Lookup the named list and return it.
     *
     * if the name is enclosed in braces [] it refers to a data
     * element - otherwise it refers to a cgi environment variable or
     * a query/post parameter.
     *
     * If no list is found by that name, will return an empty list.
     */
    private ListData getList(String listName) {
        if (listName.startsWith("[")) {
            // listName names a data element
            listName = trimDelim(listName);
            SimpleData d = getSimpleValue(listName);
            if (d instanceof ListData)   return (ListData) d;
            if (d instanceof StringData) return ((StringData) d).asList();
            if (d instanceof SimpleData) {
                ListData result = new ListData();
                result.add(d.format());
                return result;
            }
            return EMPTY_LIST;
        } else {
            // listName names an environment variable or parameter
            ListData result = new ListData();

                                // try for an environment variable first.
            Object envVal = env.get(listName);
            if (envVal != null) {
                result.addAll(envVal);
                return result;
            }
                                // if this file doesn't want us to iterate
                                // over params, abort
            if (foreachParams == false) {
                return result;
            }
                                // look for a parameter value.
            Object allParam = params.get(listName + "_ALL");
            if (allParam instanceof String[]) {
                String[] param = (String[]) allParam;
                for (int i = 0;   i < param.length;   i++)
                    result.add(param[i]);
            } else {
                Object p = params.get(listName);
                if (p == null) p = getResource(listName);
                if (p instanceof String) {
                    String paramVal = (String) p;
                    if (paramVal.startsWith("LIST="))
                        return new ListData(paramVal.substring(5));
                    else
                        result.add(paramVal);
                }
            }

            return result;
        }
    }
    public void setForeachParams(boolean foreachParams) {
        this.foreachParams = foreachParams;
    }
    private static ListData EMPTY_LIST = new ListData();


    /** Lookup the named string and return it.
     *
     * if the name is enclosed in braces [] it refers to a data
     * element - otherwise it refers to a cgi environment variable or
     * a query/post parameter.
     *
     * If no string is found by that name, will return an empty string.
     */
    private String getString(String name, boolean allowBareParams) {
        if (name.startsWith("[")) {
            // listName names a data element
            name = trimDelim(name);
            SimpleData d = getSimpleValue(name);
            return (d == null ? "" : d.format());
        } else if (name.startsWith("(")) {
            // name names a parameter value (@since 2.4.3)
            name = trimDelim(name);
            Object result = params.get(name);
            return (result == null ? "" : result.toString());
        } else if ("_UNIQUE_".equals(name)) {
            return Long.toString(uniqueNumber++);
        } else {
                                // try for an environment variable first.
            Object result = env.get(name);
            if (result instanceof String) return (String) result;

                                // look for a parameter value.
            if (allowBareParams || volatileVariables.contains(name)) {
                result = params.get(name);
                if (result instanceof String) return (String) result;
                if (result != null) return result.toString();
            }

                                // look for a resource value.
            result = getResource(name);
            if (result != null) return (String) result;

                                // look for a user setting
            result = Settings.getVal(name);
            if (result != null) return result.toString();

            return "";
        }
    }
    private static long uniqueNumber = System.currentTimeMillis();

    private Integer getInteger(DirectiveMatch dir, String attrName) {
        String attrVal = dir.getAttribute(attrName);
        if (isNull(attrVal))
            return null;
        try {
            return Integer.valueOf(attrVal);
        } catch (NumberFormatException nfe) {
            return getInteger(attrVal);
        }
    }

    private Integer getInteger(String name) {
        String value = null;
        try {
            value = getString(name, false);
            if (!isNull(value))
                return Double.valueOf(value).intValue();
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid number '" + value
                + "' for variable '" + name + "'.");
        }
        return null;
    }

    private String getResource(String name) {
        if (resources != null) {
            Iterator i = resources.iterator();
            while (i.hasNext()) {
                Resources r = (Resources) i.next();
                try {
                    return r.getString(name);
                } catch (Exception e) {}
            }
        }
        return null;
    }


    /** lookup a named value in the data repository. */
    private SimpleData getSimpleValue(String name) {
        if (data == null) return null;
        return data.getSimpleValue(name);
    }

    /** trim the first and last character from a string */
    private String trimDelim(String str) {
        return str.substring(1, str.length() - 1);
    }

    /** look up a named data element and perform a test() on it. */
    private boolean testDataElem(String name) {
        return testDataElem(name, false);
    }
    private boolean testDataElem(String name, boolean checkDefined) {
        if (name.startsWith("[")) {
            // listName names a data element
            name = trimDelim(name);
            if (checkDefined) {
                return (data != null && data.getValue(name) != null);
            } else {
                SimpleData d = getSimpleValue(name);
                return (d == null ? false : d.test());
            }
        }
        return false;
    }

    /** Class which can locate and parse directives of the form <!--#foo -->
     */
    private class DirectiveMatch {
        StringBuffer buf;
        public int begin, end;
        public String directive, contents;
        private Map attributes = null;

        /** Find the first occurrence of the named directive in buf. */
        public DirectiveMatch(StringBuffer buf, String directive) {
            this(buf, directive, 0, true); }

        /** Find a directive in buf */
        public DirectiveMatch(StringBuffer buf, String directive,
                              int pos, boolean after) {
            this.buf = buf;
            this.directive = directive;
            String dirStart = DIRECTIVE_START + directive;
            if (after)
                begin = StringUtils.indexOf(buf, dirStart, pos);
            else
                begin = StringUtils.lastIndexOf(buf, dirStart, pos);

            if (begin == -1) return;

            end = StringUtils.indexOf(buf, DIRECTIVE_END, begin);
            if (end == -1) {
                begin = -1;
                return;
            }
            contents = buf.substring(begin + dirStart.length(), end);
            end += DIRECTIVE_END.length();

            if (contents.endsWith("#")) {
                contents = contents.substring(0, contents.length()-1);
                while (end < buf.length() &&
                       Character.isWhitespace(buf.charAt(end)))
                    end++;
            }

            if (directive.length() == 0) {
                StringTokenizer tok = new StringTokenizer(contents);
                if (tok.hasMoreTokens())
                    this.directive = tok.nextToken();
                if (tok.hasMoreTokens())
                    contents = tok.nextToken("\u0000");
                else
                    contents = "";
            }
        }

        /** @return true if a directive was found */
        public boolean matches() { return begin != -1; }

        /** replace the directive found with the given text. */
        public void replace(String text) {
            buf.replace(begin, end, text);
            end = begin + text.length();
        }

        /** parse the inner contents of the directive as a set of
         * attrName=attrValue pairs, and return the value associated
         * with the named attribute. returns null if the attribute was
         * not present. */
        public String getAttribute(String attrName) {
            if (attributes == null)
                attributes = parseAttributes();
            return dirUnencode((String) attributes.get(attrName));
        }

        /** parse the inner contents of the directive as a set of
         * attrName=attrValue pairs */
        private Map parseAttributes() {
            return HTMLUtils.parseAttributes(contents);
        }

        public void rename(String newName) {
            int from = begin + DIRECTIVE_START.length();
            int to   = from + directive.length();
            buf.replace(from, to, newName);
            end = end + newName.length() - directive.length();
            directive = newName;
        }
    }


    private static Map parseParameters(String params) {
        Map result = new HashMap();
        try {
            new HttpQueryParser().parse(result, params);
        } catch (IOException ioe) {}
        return result;
    }

    /** @return true if t is null or the empty string */
    private boolean isNull(String t) {
        return (t == null || t.length() == 0);
    }
    private int whitespacePos(String t) {
        int result = t.indexOf(' ');
        if (result == -1) result = t.indexOf('\t');
        if (result == -1) result = t.indexOf('\r');
        if (result == -1) result = t.indexOf('\n');
        return result;
    }

    private boolean blockMatch(String name, String directive) {
        return (directive != null && directive.endsWith(name));
    }
    private String blockNum(String name, String directive) {
        return directive.substring(0, directive.length() - name.length());
    }

    private static final String DIRECTIVE_START = "<!--#";
    private static final String DIRECTIVE_END   = "-->";
    private static final String VERSION_PREFIX = "version ";
}
