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

package pspdash;

import pspdash.data.DataRepository;
import pspdash.data.ListData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;


/** Class for performing server-side includes and other preprocessing on
 *  HTML files.
 */
public class HTMLPreprocessor {

    TinyWebServer web;
    DataRepository data;
    Map env, params;
    String prefix;


    public HTMLPreprocessor(TinyWebServer web, DataRepository data, Map env) {
        this(web, data, (String) env.get("PATH_TRANSLATED"), env, null);
        QueryParser p = new QueryParser();
        try {
            p.parseInput((String) env.get("QUERY_STRING"));
        } catch (IOException ioe) {}
        params = p.parameters;
    }

    public HTMLPreprocessor(TinyWebServer web, DataRepository data,
                            String prefix, Map env, Map params) {
        this.web = web;
        this.data = data;
        this.prefix = (prefix == null ? "" : prefix);
        this.env = env;
        this.params = params;
    }


    /** preprocess the given content, and return the result. */
    public String preprocess(String content) throws IOException {
        StringBuffer text = new StringBuffer(content);

        processIfDirectives(text);
        processForeachDirectives(text);
        processEchoDirectives(text);
        processIncludeDirectives(text);

        return text.toString();
    }


    /** process any include directives within the buffer */
    private void processIncludeDirectives(StringBuffer text)
        throws IOException
    {
        DirectiveMatch include;
        while ((include = new DirectiveMatch(text, "include")).matches()) {
            String file = include.getAttribute("file");
            String uri = (String) env.get("REQUEST_URI");
            include.replace(isNull(file) ? ""
                            : new String(web.getRequest(uri, file, true)));
        }
    }


    /** process any echo directives within the buffer */
    private void processEchoDirectives(StringBuffer text) {
        DirectiveMatch echo;
        String var, value, encoding;
        while ((echo = new DirectiveMatch(text, "echo")).matches()) {
            value = echo.getAttribute("value");

            if (isNull(value)) {
                var = echo.getAttribute("var");
                if (isNull(var)) var = echo.contents;
                value = (isNull(var) ? "" : getString(var));
            }
            encoding = echo.getAttribute("encoding");
            if ("none".equalsIgnoreCase(encoding))
                ;
            else if ("url".equalsIgnoreCase(encoding))
                value = URLEncoder.encode(value);
            else
                // default: entity encoding
                value = web.encodeHtmlEntities(value);
            echo.replace(value);
        }
    }


    /** process any foreach directives within the buffer */
    private void processForeachDirectives(StringBuffer text) {
        String loopIndex, listName, values;
        ListData list;
        StringTokenizer tok;
        DirectiveMatch foreach, endFor;

        while ((endFor = new DirectiveMatch(text, "endfor")).matches()) {
            foreach = new DirectiveMatch(text, "foreach", endFor.begin, false);
            if (!foreach.matches()) break;

            loopIndex = foreach.getAttribute("name");
            values = foreach.getAttribute("values");
            if (isNull(values)) {
                listName = foreach.getAttribute("list");
                list = getList(listName);
            } else {
                list = new ListData(values);
            }

            String loopContents = text.substring(foreach.end, endFor.begin);
            StringBuffer replacement = new StringBuffer();
            for (int i = 0;   i < list.size();   i++)
                replacement.append
                    (StringUtils.findAndReplace(loopContents,
                                                loopIndex,
                                                (String)list.get(i)));
            text.replace(foreach.begin, endFor.end, replacement.toString());
        }
    }


    /** process any if directives within the buffer */
    private void processIfDirectives(StringBuffer text) {
        String symbolName, value;
        DirectiveMatch ifdir, endif;

        while ((endif = new DirectiveMatch(text, "endif")).matches()) {
            ifdir = new DirectiveMatch(text, "if", endif.begin, false);
            if (!ifdir.matches()) break;

            symbolName = cleanup(ifdir.contents);
            if (symbolName == null || symbolName.length() == 0) break;

            value = getString(symbolName);
            if (value == null || value.length() == 0) {
                text.replace(ifdir.begin, endif.end, "");
            } else {
                endif.replace("");
                ifdir.replace("");
            }
        }
    }


    private static String cleanup(String t) {
        t = t.trim();
        if (t.length() == 0) return t;
        if (t.charAt(0) == '"' || t.charAt(0) == '\'') {
            int endPos = t.indexOf(t.charAt(0), 1);
            if (endPos != -1) t = t.substring(1, endPos);
        }
        return t;
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
            return EMPTY_LIST;
        } else {
            // listName names an environment variable or parameter
            ListData result = new ListData();

                                // try for an environment variable first.
            Object envVal = env.get(listName);
            if (envVal instanceof String) {
                result.add((String) envVal);
                return result;
            }
                                // look for a parameter value.
            String[] param = (String[]) params.get(listName + "_ALL");
            if (param != null)
                for (int i = 0;   i < param.length;   i++)
                    result.add(param[i]);
            return result;
        }
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
    private String getString(String name) {
        if (name.startsWith("[")) {
            // listName names a data element
            name = trimDelim(name);
            SimpleData d = getSimpleValue(name);
            return (d == null ? "" : d.format());
        } else {
                                // try for an environment variable first.
            Object result = env.get(name);
            if (result instanceof String) return (String) result;

                                // look for a parameter value.
            result = params.get(name);
            if (result instanceof String) return (String) result;
            if (result != null) return result.toString();

            return "";
        }
    }

    /** lookup a named value in the data repository. */
    private SimpleData getSimpleValue(String name) {
        return data.getSimpleValue(data.createDataName(prefix, name));
    }

    /** trim the first and last character from a string */
    private String trimDelim(String str) {
        return str.substring(1, str.length() - 1);
    }

    /** Class which can locate and parse directives of the form <!--#foo -->
     */
    private class DirectiveMatch {
        StringBuffer buf;
        public int begin, end;
        public String contents;
        private Map attributes = null;

        /** Find the first occurrence of the named directive in buf. */
        public DirectiveMatch(StringBuffer buf, String directive) {
            this(buf, directive, 0, true); }

        /** Find a directive in buf */
        public DirectiveMatch(StringBuffer buf, String directive,
                              int pos, boolean after) {
            this.buf = buf;
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
        }

        /** @return true if a directive was found */
        public boolean matches() { return begin != -1; }

        /** replace the directive found with the given text. */
        public void replace(String text) {
            buf.replace(begin, end, text);
        }

        /** parse the inner contents of the directive as a set of
         * attrName=attrValue pairs, and return the value associated
         * with the named attribute. returns null if the attribute was
         * not present. */
        public String getAttribute(String attrName) {
            if (attributes == null)
                attributes = parseAttributes();
            return (String) attributes.get(attrName);
        }

        /** parse the inner contents of the directive as a set of
         * attrName=attrValue pairs */
        private Map parseAttributes() {
            HashMap result = new HashMap();
            if (contents == null || contents.length() == 0) return result;
            String attrs = contents, name, value;
            int equalsPos;
            while ((equalsPos = attrs.indexOf('=')) != -1) {
                name = attrs.substring(0, equalsPos).trim();
                attrs = attrs.substring(equalsPos+1).trim();
                if (attrs.length() == 0) break;

                int endPos;
                if (attrs.charAt(0) == '\'' || attrs.charAt(0) == '"') {
                    endPos = attrs.indexOf(attrs.charAt(0), 1);
                    if (endPos == -1) {
                        value = attrs; attrs = "";
                    } else {
                        value = attrs.substring(1, endPos);
                        attrs = attrs.substring(endPos+1);
                    }
                } else {
                    endPos = attrs.indexOf(' ');
                    if (endPos == -1) endPos = attrs.indexOf('\t');
                    if (endPos == -1) endPos = attrs.indexOf('\r');
                    if (endPos == -1) endPos = attrs.indexOf('\n');
                    if (endPos == -1) endPos = attrs.length();
                    value = attrs.substring(0, endPos);
                    attrs = attrs.substring(endPos);
                }
                result.put(name, value);
            }
            return result;
        }
    }

    private static class QueryParser extends TinyCGIBase {
        protected boolean supportQueryFiles() { return false; }
    }

    /** @return true if t is null or the empty string */
    private boolean isNull(String t) {
        return (t == null || t.length() == 0);
    }

    private static final String DIRECTIVE_START = "<!--#";
    private static final String DIRECTIVE_END   = "-->";
}
