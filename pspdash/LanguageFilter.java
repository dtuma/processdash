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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/** The LanguageFilter interface supports the generation of LOC counts
 *  and redlines in custom programming languages.
 *
 * The pspdiff script can count physical LOC changes in any text-based
 * language.  To support new languages, simply write a java class which
 * implements this interface.
 */
public interface LanguageFilter extends TinyCGI {

    /** a constant indicating that this class definitely <b>cannot</b> act
        as a filter for a particular file. */
    int LANGUAGE_MISMATCH = 0;

    /** a constant indicating that this class definitely <b>can</b> act as
        a filter for a particular file. */
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
     * <li>If you would like to reformat the source code intelligently,
     *     placing no more than one logical LOC on each physical LOC, this
     *     method can do that as well.  This, however, is <b>not</b>
     *     required.</ul>
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



    /** Display HTML describing the options provided by this language filter.
     *
     * The HTML output by this function will be included in a larger page,
     * so it should not include the typical
     *     <xmp><HTML>, <HEAD>, or <BODY></xmp>
     * tags.
     */
    void service(InputStream in, OutputStream out, Map env)
        throws IOException;
}
