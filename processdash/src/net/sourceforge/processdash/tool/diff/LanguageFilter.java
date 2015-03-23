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


package net.sourceforge.processdash.tool.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import net.sourceforge.processdash.net.http.TinyCGI;


/** The LanguageFilter interface supports the generation of LOC counts
 *  and redlines in custom programming languages.
 *
 * The pspdiff script can count physical LOC changes in any text-based
 * language.  To support new languages, simply write a java class which
 * implements this interface.
 */
public interface LanguageFilter extends TinyCGI {

    /** a constant indicating that this class definitely <b>cannot</b> act
     *  as a filter for a particular file. */
    int LANGUAGE_MISMATCH = 0;

    /** a constant indicating that this class definitely <b>can</b> act as
     *  a filter for a particular file. */
    int LANGUAGE_MATCH = 100;

    /** Judge whether this filter is capable of acting as a filter for a
     *  particular file.
     *
     * @param filename the name of the file in question
     * @param contents the contents of the file in question
     * @param options the options given by the user to pspdiff.  You could
     *    check to see if the user was explicitly asking to use your
     *    language filter.  In addition, this method <u>will be called</u>
     *    once before the <code>highlightSyntax</code> method is called
     *    twice, so if you decide to act as a filter for this file, you
     *    could remember user-specific options for use later in the
     *    <code>highlightSyntax</code> or <code>isSignificant</code>
     *    routines.
     * @return LANGUAGE_MISMATCH if this filter definitely <b>cannot</b>
     *    act as a filter; LANGUAGE_MATCH if this filter definitely
     *    <b>can</b> act as a filter; or some number in between to indicate
     *    an intermediate degree of certainty.  If multiple filters all
     *    volunteer to analyze a file, the filter with the highest match
     *    rating will be chosen.
     */
    int languageMatches(String filename, String contents, String options);



    /** a character used to flag the start of a comment */
    char COMMENT_START = '\u0001';

    /** a character used to flag the end of a comment */
    char COMMENT_END   = '\u0002';

    /** a character used to flag logical LOC */
    char LOGICAL_LOC_SEPARATOR = '\u0005';


    /** Insert flags in a file to highlight the syntax of the language.
     *
     * When counting LOC changes between two files, <code>pspdiff</code>
     * will call this method twice - once for each file.
     *
     * @param file The entire contents of a file to be compared.  Line
     *     terminators in this <code>StringBuffer</code> (e.g.  CR-LF on
     *     the PC, or CR on the mac) will <b>already</b> have been
     *     converted to a single newline character ("\n") <b>before</b>
     *     this method is called.  This method should <b>alter</b> the
     *     <code>StringBuffer</code> in the following ways:<ul>

     * <li>find comments in the file; place a <code>COMMENT_START</code>
     *     character immediately before the character starting the comment,
     *     and a <code>COMMENT_END</code> character at the end of the
     *     comment.  The general contract is that the entire text between
     *     and including the <code>COMMENT_START</code> and
     *     <code>COMMENT_END</code> characters could be removed without
     *     altering program logic, and without changing the physical LOC
     *     count.
     *
     * <li>Discard portions of the file that are not <b>really</b> part of
     *     the program source code.  For example, if the file contains some
     *     long header or footer that is inserted by an IDE (and which is
     *     never visible to the programmer), this method should discard
     *     that portion of the file. (Visual Basic is one example of this.)
     *
     * <li>Discard portions of the file that would interfere with proper
     *     line-by-line comparison.  For example, this method should strip
     *     line numbers.
     *
     * <li><i>(Optional)</i> LanguageFilters that wish to support
     *     <b>logical</b> LOC counting should parse the syntax of the
     *     file and insert <code>LOGICAL_LOC_SEPARATOR</code>
     *     characters between each logical line of code.  If pspdiff
     *     finds any <code>LOGICAL_LOC_SEPARATOR</code> characters in
     *     the StringBuffer, it will generate logical LOC counts
     *     rather than physical LOC counts.
     * </ul>
     */
    void highlightSyntax(StringBuffer file);



    /** Test a line of code to see if it is countable.
     *
     * @param line a single line of code from the program.  It will not
     *     contain any comments - they will already have been stripped from
     *     the line before this method is called.  The line will also not
     *     contain any line terminators.
     * @return true if the line should be counted, false otherwise.
     */
    boolean isSignificant(String line);



    /** Return an array describing the options recognized by this language
     *  filter.
     *
     *  The array should contain an entry for each option.  Each
     *  option entry should be a two-element string array; the first
     *  String is the HTML text of the option, and the second string is the
     *  HTML description of that option.  Example:<XMP>
     *   { { "+{",
     *       "Count a curly brace on a line by itself as a line of code." },
     *     { "-{",
     *       "Do not count a curly brace on a line by itself as a LOC <b>(default)</b>." } };
     *  </XMP>
     */
    String[][] getOptions();



    /** Display any caveats about the results generated by this language
     *  filter.  These can take the user-selected options into account.
     *
     * The HTML output by this function will be included in a larger page,
     * so it should not include the typical
     *     <xmp><HTML>, <HEAD>, or <BODY></xmp>
     * tags.
     */
    void service(InputStream in, OutputStream out, Map env)
        throws IOException;

    void setCharset(String charset);
}
