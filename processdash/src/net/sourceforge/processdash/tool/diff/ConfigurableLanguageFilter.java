// Copyright (C) 2007-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ConfigurableLanguageFilter extends AbstractLanguageFilter
        implements AbstractLanguageFilter.NamedFilter {

    protected String id;
    protected Resources res;
    protected String[] commentStarters;
    protected String[] commentEnders;
    protected String[] stringStarters;
    protected char[] stringEscapes;
    protected String[] stringEmbeds;
    protected String[] stringEnders;
    protected String[] filenameEndings;
    protected String[] firstLinePatterns;
    protected String[][] options;
    protected IgnoredLinePattern[] ignoredLinePatterns;

    /** Return the name of this filter */
    public String getFilterName() {
        return id;
    }

    /** Return the list of strings that start comments */
    @Override
    protected String[] getCommentStarters() {
        return commentStarters;
    }

    /** Return the list of strings that end comments */
    @Override
    protected String[] getCommentEnders() {
        return commentEnders;
    }

    /** Return the list of strings that start string literals */
    @Override
    protected String[] getStringStarters() {
        return stringStarters;
    }

    /** Return the list of escape characters for string literals */
    @Override
    protected char[] getStringEscapes() {
        return stringEscapes;
    }

    /** Return the list of embedded escape sequences for string literals */
    @Override
    protected String[] getStringEmbeds() {
        return stringEmbeds;
    }

    /** Return the list of strings that end string literals */
    @Override
    protected String[] getStringEnders() {
        return stringEnders;
    }

    /** Return the list of filename suffixes that suggest this filter */
    @Override
    protected String[] getDefaultFilenameEndings() {
        return filenameEndings;
    }

    /** Return the names and descriptions of options this filter supports */
    @Override
    public String[][] getOptions() {
        return options;
    }

    /** See if the contents begin with a non-comment string that appears to
     * be in our language, and increase the match-worthiness if so. */
    @Override
    protected int doubleCheckFileContents(String contents, int match) {
        if (firstLinePatterns != null) {
            for (int i = 0; i < firstLinePatterns.length; i++) {
                if (contents.startsWith(firstLinePatterns[i]))
                    return match + 30;
            }
        }

        return match;
    }

    /** give preferential treatment to ConfigurableLanguageFilter objects
     * over hardcoded objects;  this allows a user to create their own filter,
     * and have it be selected in advance of the filters in the dashboard.
     */
    @Override
    public int languageMatches(String filename, String contents, String options) {
        int result = super.languageMatches(filename, contents, options);
        if (result > 0)
            result += 10;
        return result;
    }

    /** Set the user-specified options that are in effect for counting a
     * particular file */
    @Override
    protected void setOptions(String options) {
        if (ignoredLinePatterns == null)
            return;

        String[] opts;
        if (hasValue(options))
            opts = options.split("\\s+");
        else
            opts = new String[0];
        for (int i = 0; i < ignoredLinePatterns.length; i++) {
            ignoredLinePatterns[i].setOptions(opts);
        }
    }

    /** Return true if a given line of code is countable, false otherwise */
    public boolean isSignificant(String line) {
        line = line.trim();
        if (line.length() == 0)
            return false;

        if (ignoredLinePatterns != null) {
            for (int i = 0; i < ignoredLinePatterns.length; i++) {
                if (ignoredLinePatterns[i].shouldIgnore(line))
                    return false;
            }
        }

        return true;
    }


    /** Configure this filter based on information in the given xml Element */
    public void setConfigElement(Element xml, String attrName) {
        checkElementTagName(xml);
        loadFilterID(xml);
        loadResourceBundle(xml);
        loadCommentSyntaxInformation(xml);
        loadStringSyntaxInformation(xml);
        loadFilenameEndings(xml);
        loadOptions(xml);
        loadFirstLinePatterns(xml);
        loadIgnoredLinePatterns(xml);
    }

    /** Ensure the xml looks like a declaration for a LOC Filter extension */
    protected void checkElementTagName(Element xml) {
        if (!TemplateFilterLocator.EXTENSION_TAG.equals(xml.getTagName()))
            throw new IllegalArgumentException("<"
                    + TemplateFilterLocator.EXTENSION_TAG
                    + "> tag expected for declaration");
    }

    /** Read the filter ID from the xml configuration */
    protected void loadFilterID(Element xml) {
        // get the ID of this filter
        id = xml.getAttribute("id");
        if (!hasValue(id))
            throw new IllegalArgumentException(
                    "The id attribute must be specified");
        setLangName(id);
    }

    /** Check to see if this filter uses any resource bundles */
    protected void loadResourceBundle(Element xml) {
        Element e = (Element) xml.getElementsByTagName(RESOURCES_TAG).item(0);
        if (e != null) {
            String resourceBundleName = XMLUtils.getTextContents(e);
            if (hasValue(resourceBundleName))
                res = Resources.getDashBundle(resourceBundleName);
        }
    }

    /** Read the comment syntaxes recognized by this filter */
    protected void loadCommentSyntaxInformation(Element xml) {
        NodeList nodes = xml.getElementsByTagName(COMMENT_SYNTAX_TAG);
        if (nodes.getLength() == 0)
            return;

        List<String> starters = new ArrayList<String>();
        List<String> enders = new ArrayList<String>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String start = e.getAttribute(COMMENT_BEGIN_ATTR);
            if (!hasValue(start)) {
                throw new IllegalArgumentException("Missing "
                        + COMMENT_BEGIN_ATTR + " for <"
                        + COMMENT_SYNTAX_TAG + "> tag");
            }
            String end = e.getAttribute(COMMENT_END_ATTR);
            if (!hasValue(end) || "\\n".equals(end))
                end = "\n";

            starters.add(start);
            enders.add(end);
        }

        commentStarters = starters.toArray(new String[starters.size()]);
        commentEnders = enders.toArray(new String[enders.size()]);
    }

    /** Read the string syntaxes recognized by this filter */
    protected void loadStringSyntaxInformation(Element xml) {
        NodeList nodes = xml.getElementsByTagName(STRING_SYNTAX_TAG);
        if (nodes.getLength() == 0)
            return;

        String[] starts = new String[nodes.getLength()];
        char[] escapes = new char[nodes.getLength()];
        String[] embeds = new String[nodes.getLength()];
        String[] ends = new String[nodes.getLength()];

        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String start = e.getAttribute(STRING_BEGIN_ATTR);
            String delim = e.getAttribute(STRING_DELIM_ATTR);
            String end = e.getAttribute(STRING_END_ATTR);
            if (hasValue(delim)) {
                starts[i] = ends[i] = delim;
            } else if (hasValue(start) && hasValue(end)) {
                starts[i] = start;
                ends[i] = end;
            } else {
                throw new IllegalArgumentException("Either "
                        + STRING_DELIM_ATTR + " or both "
                        + STRING_BEGIN_ATTR + " and "
                        + STRING_END_ATTR + " must be provided for <"
                        + STRING_SYNTAX_TAG + "> tag");
            }

            String escape = e.getAttribute(STRING_ESCAPE_ATTR);
            escapes[i] = (hasValue(escape) ? escape.charAt(0) : (char) 0);

            String embed = e.getAttribute(STRING_EMBED_ATTR);
            if (hasValue(embed)) {
                if ("\\n".equals(embed))
                    embed = "\n";
                embeds[i] = embed;
            }
        }

        stringStarters = starts;
        stringEscapes = escapes;
        stringEmbeds = embeds;
        stringEnders = ends;
    }

    /** Read the filename suffixes that suggest the use of this filter */
    protected void loadFilenameEndings(Element xml) {
        String suffixList = xml.getAttribute(FILENAME_SUFFIXES_ATTR);
        if (hasValue(suffixList))
            filenameEndings = suffixList.split("\\s+");
    }

    /** Read the documentation for the options supported by this filter */
    protected void loadOptions(Element xml) {
        NodeList nodes = xml.getElementsByTagName(OPTION_TAG);
        if (nodes.getLength() == 0)
            return;

        String[][] result = new String[nodes.getLength()][2];
        for (int i = 0;  i < nodes.getLength();  i++) {
            Element e = (Element) nodes.item(i);
            String text = e.getAttribute(OPTION_TEXT_ATTR);
            if (!hasValue(text))
                throw new IllegalArgumentException("Missing "
                    + OPTION_TEXT_ATTR + " for <"
                    + OPTION_TAG + "> tag");

            String description;
            String descriptionKey = e.getAttribute(OPTION_DESCR_ATTR);
            if (res != null && hasValue(descriptionKey)) {
                description = res.getString(descriptionKey);
            } else {
                description = XMLUtils.getTextContents(e);
            }
            if (description == null) description = "";

            result[i][0] = text;
            result[i][1] = description;
        }

        options = result;
    }

    /** Read the first line patterns that suggest the use of this filter */
    protected void loadFirstLinePatterns(Element xml) {
        NodeList nodes = xml.getElementsByTagName(FIRST_LINE_TAG);
        if (nodes.getLength() == 0)
            return;

        List<String> result = new ArrayList<String>();
        for (int i = 0;  i < nodes.getLength();  i++) {
            Element e = (Element) nodes.item(i);
            String pattern = XMLUtils.getTextContents(e);
            if (hasValue(pattern))
                result.add(pattern);
        }

        firstLinePatterns = result.toArray(new String[result.size()]);
    }

    /** Read the ignored line patterns recognized by this filter */
    protected void loadIgnoredLinePatterns(Element xml) {
        NodeList nodes = xml.getElementsByTagName(IGNORED_TAG);
        if (nodes.getLength() == 0)
            return;

        List<IgnoredLinePattern> result = new ArrayList<IgnoredLinePattern>();
        for (int i = 0;  i < nodes.getLength();  i++) {
            Element e = (Element) nodes.item(i);
            result.add(new IgnoredLinePattern(e));
        }
        ignoredLinePatterns = result.toArray(new IgnoredLinePattern[result.size()]);
    }

    /** A class to manage a single ignored line pattern, along with its
     * enabling/disabling options and its current enabled/disabled state
     */
    protected class IgnoredLinePattern extends PatternList {
        /** The name of an option that enables this pattern (can be null) */
        String enablingOption;
        /** The name of an option that disables this pattern (can be null) */
        String disablingOption;
        /** Whether this pattern is currently enabled */
        boolean enabled;

        IgnoredLinePattern(Element xml) {
            // load the enabling/disabling option information
            enablingOption = getAttr(xml, "if");
            disablingOption = getAttr(xml, "unless");

            // load information about the patterns recognized by this filter
            containsItems = addPattern(xml, "containing");
            startsWithItems = addPattern(xml, "beginningWith");
            endsWithItems = addPattern(xml, "endingWith");
            equalsItems = addPattern(xml, "equalTo");

            String re = xml.getAttribute("regExp");
            if (hasValue(re))
                addRegexp(re);
        }

        protected String getAttr(Element xml, String name) {
            String result = xml.getAttribute(name);
            return (hasValue(result) ? result : null);
        }

        protected List addPattern(Element xml, String attr) {
            String text = xml.getAttribute(attr);
            if (hasValue(text))
                return addToList(null, text);
            else
                return null;
        }

        /** Look at the user-specified options passed in, and decide whether
         * this pattern should be enabled or disabled. */
        public void setOptions(String[] options) {
            if (enablingOption != null) {
                enabled = contains(options, enablingOption);
            } else if (disablingOption != null) {
                enabled = !contains(options, disablingOption);
            } else {
                enabled = true;
            }
        }

        /** Return true if the given line should be ignored */
        public boolean shouldIgnore(String trimmedLine) {
            return this.enabled && this.matches(trimmedLine);
        }
    }

    /** Returns true if the list of strings contains a case-insensitive match
     * for a particular string.
     * @param list the list of strings to search
     * @param s a string to look for
     * @return true if the list contains the given string
     */
    protected static boolean contains(String[] list, String s) {
        for (int i = 0; i < list.length; i++) {
            if (s.equalsIgnoreCase(list[i]))
                return true;
        }
        return false;
    }

    protected static boolean hasValue(String s) {
        return XMLUtils.hasValue(s);
    }

    protected static final String RESOURCES_TAG = "resources";
    protected static final String COMMENT_SYNTAX_TAG = "commentSyntax";
    protected static final String COMMENT_BEGIN_ATTR = "beginsWith";
    protected static final String COMMENT_END_ATTR = "endsWith";
    protected static final String STRING_SYNTAX_TAG = "stringSyntax";
    protected static final String STRING_DELIM_ATTR = "delimiter";
    protected static final String STRING_BEGIN_ATTR = COMMENT_BEGIN_ATTR;
    protected static final String STRING_END_ATTR = COMMENT_END_ATTR;
    protected static final String STRING_ESCAPE_ATTR = "escapeChar";
    protected static final String STRING_EMBED_ATTR = "mayInclude";
    protected static final String FILENAME_SUFFIXES_ATTR = "fileSuffixes";
    protected static final String OPTION_TAG = "option";
    protected static final String OPTION_TEXT_ATTR = "text";
    protected static final String OPTION_DESCR_ATTR = "descriptionKey";
    protected static final String FIRST_LINE_TAG = "possibleFirstLine";
    protected static final String IGNORED_TAG = "ignoreLine";

}
