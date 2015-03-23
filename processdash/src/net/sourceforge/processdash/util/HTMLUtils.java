// Copyright (C) 2001-2012 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


public class HTMLUtils {

    public static final String HTML_STRICT_DOCTYPE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/strict.dtd\">\n";
    public static final String HTML_TRANSITIONAL_DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/loose.dtd\">\n";
    public static final String HTML_FRAMESET_DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/frameset.dtd\">\n";

    public static String escapeEntities(String value) {
        if (StringUtils.containsChars(value, "<>&\"'") == false)
            return value;

        StringTokenizer tok = new StringTokenizer(value, "<>&\"'", true);
        StringBuffer result = new StringBuffer();
        String token;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if      ("<".equals(token))  result.append("&lt;");
            else if (">".equals(token))  result.append("&gt;");
            else if ("&".equals(token))  result.append("&amp;");
            else if ("\"".equals(token)) result.append("&quot;");
            else if ("'".equals(token))  result.append("&#x27;");
            else                         result.append(token);
        }
        return result.toString();
    }

    public static final StringMapper ESC_ENTITIES = new StringMapper() {
        public String getString(String str) {
            return escapeEntities(str);
        }
    };

    public static String unescapeEntities(String value) {
        if (value.indexOf('&') == -1)
            return value;

        value = StringUtils.findAndReplace(value, "&lt;",   "<");
        value = StringUtils.findAndReplace(value, "&gt;",   ">");
        value = StringUtils.findAndReplace(value, "&quot;", "\"");
        value = StringUtils.findAndReplace(value, "&apos;", "'");
        value = StringUtils.findAndReplace(value, "&#x27;", "'");
        value = StringUtils.findAndReplace(value, "&nbsp;", " ");
        value = StringUtils.findAndReplace(value, "&amp;",  "&");
        return value;
    }

    public static final StringMapper UNESC_ENTITIES = new StringMapper() {
        public String getString(String str) {
            return unescapeEntities(str);
        }
    };

    public static String urlEncodePath(String path) {
        path = urlEncode(path);
        path = StringUtils.findAndReplace(path, "%2F", "/");
        path = StringUtils.findAndReplace(path, "%2f", "/");
        return path;
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
    }

    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
    }

    /** parse the inner contents of a tag as a set of
     * attrName=attrValue pairs
     * @param contents the text beginning after the tag name and ending
     *        before the closing ">" or "/>"
     */
    public static Map parseAttributes(String contents) {
        HashMap result = new HashMap();
        if (contents == null || contents.length() == 0) return result;

        String attrs = contents, name, value;
        int equalsPos;
        while ((equalsPos = attrs.indexOf('=')) != -1) {
            name = attrs.substring(0, equalsPos).trim();
            attrs = attrs.substring(equalsPos+1).trim();
            if (attrs.length() == 0) break;
            if (name.startsWith(">") || name.startsWith("/>")) break;

            int endPos;
            if (attrs.charAt(0) == '\'' || attrs.charAt(0) == '"') {
                endPos = attrs.indexOf(attrs.charAt(0), 1);
                if (endPos == -1) {
                    value = attrs; attrs = "";
                } else {
                    value = attrs.substring(1, endPos);
                    attrs = attrs.substring(endPos+1);
                }
            } else if (attrs.charAt(0) == '[') {
                endPos = attrs.indexOf(']', 1);
                if (endPos == -1) {
                    value = attrs; attrs = "";
                } else {
                    value = attrs.substring(0, endPos+1);
                    attrs = attrs.substring(endPos+1);
                }
            } else {
                endPos = whitespacePos(attrs);
                if (endPos == -1) endPos = attrs.length();
                value = attrs.substring(0, endPos);
                attrs = attrs.substring(endPos);
            }
            value = unescapeEntities(value);
            result.put(name, value);
        }
        return result;
    }

    private static int whitespacePos(String t) {
        int result = t.indexOf(' ');
        if (result == -1) result = t.indexOf('\t');
        if (result == -1) result = t.indexOf('\r');
        if (result == -1) result = t.indexOf('\n');
        return result;
    }

    public static String appendQuery(String uri, String name, String value) {
        if (uri == null || value == null)
            return uri;
        StringBuffer result = new StringBuffer(uri);
        appendQuery(result, name, value);
        return result.toString();
    }

    public static void appendQuery(StringBuffer uri, String name, String value) {
        if (uri != null && value != null) {
            uri.append(uri.indexOf("?") == -1 ? '?' : '&');
            uri.append(urlEncode(name)).append('=').append(urlEncode(value));
        }
    }

    public static String appendQuery(String uri, String query) {
        StringBuffer result = new StringBuffer(uri);
        appendQuery(result, query);
        return result.toString();
    }

    public static void appendQuery(StringBuffer uri, String query) {
        if (query == null)
            return;

        while (query.length() > 0 && "?&".indexOf(query.charAt(0)) != -1)
            query = query.substring(1);

        if (query.length() > 0)
            uri.append(uri.indexOf("?") == -1 ? '?' : '&').append(query);
    }

    public static String removeParam(String uri, String paramName) {
        int pos = uri.indexOf("?") + 1;
        if (pos == 0) return uri;
        String paramStart = paramName + "=";
        if (uri.indexOf(paramStart, pos) == -1) return uri;

        StringBuffer result = new StringBuffer(uri);
        removeParam(result, paramStart, pos);
        return result.toString();
    }

    public static void removeParam(StringBuffer uri, String paramName) {
        int pos = uri.indexOf("?") + 1;
        if (pos == 0) return;
        String paramStart = paramName + "=";
        if (uri.indexOf(paramStart, pos) == -1) return;

        removeParam(uri, paramStart, pos);
    }

    private static void removeParam(StringBuffer uri, String paramStart,
            int pos) {
        // pos always points at the 1st character following either '?' or a '&'
        while (pos < uri.length()) {
            // point end at the character following the next '&', or at 0 if
            // there are no more parameters.
            int end = uri.indexOf("&", pos) + 1;

            if (StringUtils.startsWith(uri, paramStart, pos)) {
                if (end == 0)
                    // strip off the final parameter, and also the '?' or '&'
                    // that preceeded it.
                    uri.setLength(pos-1);
                else
                    // delete this parameter, up through the following '&'
                    uri.replace(pos, end, "");
            } else {
                // this parameter is not one we're looking for.
                if (end == 0)
                    break;   // no more parameters? exit the loop.
                else
                    pos = end; // advance to start looking at the next param.
            }
        }
    }

    /**
     * Parse the parameters associated with a URL, and return them as a Map.
     * 
     * @param url
     *            a complete URL, or just the query string portion of a URL
     * @return a Map of param = value pairs, as returned by the
     *         {@link HttpQueryParser} class.
     * @since Process Dashboard version 1.13.0.17
     */
    public static Map parseQuery(String url) {
        if (url == null)
            return Collections.EMPTY_MAP;

        String query;
        int queryPos = url.indexOf('?');
        if (queryPos == -1)
            query = url;
        else
            query = url.substring(queryPos + 1);

        Map result = new HashMap();
        try {
            QUERY_PARSER.parse(result, query);
        } catch (IOException ioe) {
            // does not occur for the simple query parser class
        }
        return result;
    }

    private static final HttpQueryParser QUERY_PARSER = new HttpQueryParser();


    public static String cssLinkHtml(String href) {
        return "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + href
                + "\">\n";
    }

    public static String scriptLinkHtml(String href) {
        return "<script type=\"text/javascript\" src=\"" + href
                + "\"></script>\n";
    }

    /**
     * Some scripts may wish to display a page, then redirect to another page
     * after a short delay. For example, this is a common way to display a
     * "please wait" page, followed by a page that actually performs the work.
     * 
     * A meta-refresh tag would normally be used for this purpose, but some web
     * browsers (namely, newer versions of IE with High security enabled) ignore
     * the meta refresh tag. This method generates a block of HTML that can be
     * placed in the &lt;head&gt; of a webpage.
     * 
     * @param uri
     *            the URI to redirect to; may be absolute or relative. Must
     *            be a well-formed URI, not containing forbidden characters
     * @param delay
     *            the number of seconds to delay before redirecting; can be 0
     *            for an immediate redirect
     * @since 1.14.3
     */
    public static String redirectScriptHtml(String uri, int delay) {
        if (uri.indexOf('"') != -1)
            throw new IllegalArgumentException(
                    "URIs cannot contain double-quote characters");

        StringBuffer result = new StringBuffer();
        result.append("<script type=\"text/javascript\"> ");

        if (delay > 0)
            result.append("var metaRefreshTimeout = window.setTimeout(function(){ ");

        result.append("window.location.replace(\"").append(uri).append("\");");

        if (delay > 0)
            result.append(" }, ").append(delay * 1000).append("); ");

        result.append("</script>");
        return result.toString();
    }

}
