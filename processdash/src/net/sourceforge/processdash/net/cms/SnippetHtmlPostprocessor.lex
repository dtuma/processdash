// -*- mode:java;  tab-width: 4  -*-
//
// This java file is automatically generated from the file
// "SnippetHtmlPostprocessor.lex".  Do not edit the Java
// file directly; your edits will be overwritten.
//
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Tuma Solutions, LLC
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
//
package net.sourceforge.processdash.net.cms;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

%%

%{

    private String namespace;
    private String selfURI;
    private URL baseURL;
    private boolean clobberForms = false;

    private List headerItems = new ArrayList();
    private StringBuffer content = new StringBuffer();
    private Map headerItem;

    public SnippetHtmlPostprocessor(String namespace, String selfURI,
            String baseURL, boolean clobberForms, String html)
            throws IOException {
        this(new StringReader(html));
        this.namespace = namespace;
        this.selfURI = selfURI;
        this.baseURL = new URL("http://ignored" + baseURL);
        this.clobberForms = clobberForms;
        run();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    public boolean getClobberForms() {
        return clobberForms;
    }

    public void setClobberForms(boolean clobberForms) {
        this.clobberForms = clobberForms;
    }

    public void run() throws IOException {
        synchronized (yy_reader) {
            synchronized (content) {
                while (next() != null)
                    ;
            }
        }
    }

    public List getHeaderItems() {
        return headerItems;
    }

    public String getResults() {
        return content.toString();
    }

    private Boolean addHeaderItemAttr() {
        String txt = yytext();
        int equalsPos = txt.indexOf('=');
        String attrName = txt.substring(0, equalsPos).trim().toLowerCase();

        String attrVal = txt.substring(equalsPos+1).trim();

        // trim the quotes from the attribute value.
        if ((attrVal.startsWith("'") && attrVal.endsWith("'")) ||
            (attrVal.startsWith("\"") && attrVal.endsWith("\"")))
            attrVal = attrVal.substring(1, attrVal.length()-1);

        // escape any double quote characters that might appear in the string
        // (could happen if the value was delimited with single quotes)
        attrVal = StringUtils.findAndReplace(attrVal, "\"", "&quot;");

        // expand the namespace if it's present
        attrVal = StringUtils.findAndReplace(attrVal, "$$$_", getNamespace());

        // reroot URI attributes as needed.
        if (attrName.equals("src") || attrName.equals("href"))
            attrVal = rerootURI(attrVal);

        // save the attribute value in our map.
        headerItem.put(attrName, attrVal);

        return Boolean.TRUE;
    }

    private Boolean finishHeaderItem() {
        if ((headerItem.containsKey("src") || headerItem.containsKey("href"))
                && !"true".equals(headerItem.get("snippetignore"))) {
            StringBuffer item = new StringBuffer();
            String tag = (String) headerItem.remove("");
            item.append(tag);
            for (Iterator i = headerItem.entrySet().iterator();  i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                item.append(" ").append(e.getKey()).append("=\"")
                    .append(e.getValue()).append("\"");
            }
            if (tag.indexOf("script") != -1)
                item.append("></script>");
            else
                item.append(">");
            headerItems.add(item.toString());
        }
        headerItem = null;

        return Boolean.TRUE;
    }

    private Boolean send() {
        // this is phenomenally more efficient than writing yytext().
        // Since Strings must be immutable, yytext's call to new String()
        // must copy the yy_buffer character array.
        content.append(yy_buffer,
                       yy_buffer_start,
                       yy_buffer_end - yy_buffer_start);
        return Boolean.TRUE;
    }

    private Boolean send(String text) {
        content.append(text);
        return Boolean.TRUE;
    }

    private Boolean sendURI() {
        String attr = yytext();
        int attrNameLen = Math.max(attr.lastIndexOf('\''), attr.lastIndexOf('"')) + 1;
        send(attr.substring(0, attrNameLen));

        String uri = attr.substring(attrNameLen);
        uri = rerootURI(uri);
        return send(uri);
    }

    private String rerootURI(String uri) {
        if (uri.startsWith("/"))
            return uri;
        if (uri.startsWith("PAGE_URL")) {
            if (uri.startsWith("PAGE_URL?") && selfURI.indexOf('?') != -1)
                return selfURI + "&amp;" + uri.substring(9);
            else
                return selfURI + uri.substring(8);
        }

        try {
            uri = HTMLUtils.unescapeEntities(uri);
            URL u = new URL(baseURL, uri);
            return HTMLUtils.escapeEntities(u.getFile());
        } catch (IOException ioe) {
            // bad URL.  Leave it alone.
            return uri;
        }
    }


    // For testing purposes only
    public static void main(String[] args) {
        try {
            SnippetHtmlPostprocessor hp = new SnippetHtmlPostprocessor(
                new FileReader(args[0]));
            hp.setNamespace("Namespace_");
            hp.setBaseURL(new URL("http://ignored/Project/Foo//path/to/snippet"));
            hp.setClobberForms(true);
            hp.run();

            System.out.println("Header Items:");
            for (Iterator i = hp.getHeaderItems().iterator();  i.hasNext(); )
                System.out.println("\t" + i.next());

            System.out.println();
            System.out.println("Html content:");
            System.out.println(hp.getResults());

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // just to get Eclipse to stop complaining
    protected void unusedMethod() {
        System.out.println("" + yy_eof_done + yy_last_was_cr + yylength() + YY_E_MATCH);
    }
%}


%unicode
%ignorecase
%public
%class SnippetHtmlPostprocessor
%function next
%type Boolean
%state inHeader, inHeaderElem, inContentElem


ANY=[\x00-\uffff]
WORD_CHAR=[A-Za-z]
NON_WORD_CHAR=[^A-Za-z<]
NON_WORD_CHAR_OR_TAG=[^A-Za-z]
WHITE_SPACE_CHAR=[\n\ \t\b\012]
SP=[\n\ \t\b\012]
EQ=[\n\ \t\b\012]*=[\n\ \t\b\012]*
REL_URI=[^/][^#?\'\"]+
IN_TAG=[^>]
SAFE_INNER=[^<>\-]

%%

<YYINITIAL> "<head>" {
    yybegin(inHeader);
    content.setLength(0);
    return Boolean.TRUE; }

<inHeader> "<script"|"<link" {
    yybegin(inHeaderElem);
    headerItem = new TreeMap();
    headerItem.put("", yytext().toLowerCase());
    return Boolean.TRUE; }

<inHeaderElem> {WORD_CHAR}+{EQ}([a-zA-Z0-9._-]+|"\""[^\"]*"\""|"'"[^\']*"'") {
    return addHeaderItemAttr(); }

<inHeaderElem> "/>"|">" {
    yybegin(inHeader);
    return finishHeaderItem(); }

<inHeader,inHeaderElem> {ANY} { return Boolean.TRUE; }

<inHeader> "</head>" {
    yybegin(YYINITIAL);
    content.setLength(0);
    return Boolean.TRUE; }


<inContentElem,YYINITIAL> "$$$_" {
    return send(getNamespace()); }

<inContentElem> ("src"|"href"){EQ}[\'\"]{REL_URI} {
    return sendURI(); }

<YYINITIAL> "<form" {
    yybegin(inContentElem);
    if (clobberForms)
        return send("<div");
    else
        return send(); }

<YYINITIAL> "</form" {
    yybegin(inContentElem);
    if (clobberForms)
        return send("</div");
    else
        return send(); }

<YYINITIAL> "<" { yybegin(inContentElem); return send(); }

<inContentElem> ">" { yybegin(YYINITIAL); return send(); }

<inHeaderElem,inContentElem,YYINITIAL> {ANY} { return send(); }


<YYINITIAL> "<html>"|"<body>"|"<!--"{SP}*"cutStart"{SP}*"-->" {
    content.setLength(0);
    return Boolean.TRUE; }


<YYINITIAL> "</html>"|"</body>"|"<!--"{SP}*"cutEnd"{SP}*"-->" {
    return null; }





{ANY} { throw new IOException("Unmatched input: " + yytext()); }
