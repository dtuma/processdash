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
// Foundation, Inc., 59 Temple Place -Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import com.oroinc.text.perl.Perl5Util;
import com.oroinc.text.perl.MalformedPerl5PatternException;

/** Perform cpp-like macro expansion on a stream of text.
 *
 * Currently only expands #define macros. Macros with and without
 * arguments are supported. Expansion is done in traditional style, as
 * per "cpp -E -x -P -traditional".
 *
 * Does NOT process #include directives, and does NOT filter out
 * comments.
 */
public class CppFilter {

    BufferedReader in;
    Perl5Util perl;
    HashMap substitutions;

    public CppFilter(BufferedReader in) {
        this.in = in;
        this.perl = (Perl5Util) ValueFactory.perlPool.get();
        this.substitutions = new HashMap();
    }

    public void dispose() {
        if (perl != null)
            ValueFactory.perlPool.release(perl);
        perl = null;
    }

    public String readLine() throws IOException {
        String line = in.readLine();
        if (line == null) {
            dispose();
            return null;
        }
        if (line.startsWith(DEFINE_DIRECTIVE)) {
            while (line.endsWith("\\")) {
                line = line.substring(0, line.length()-1);
                String extra = in.readLine();
                if (extra == null) {
                    dispose();
                    break;
                }
                line = line + extra;
            }

            processDefineDirective(line);
            return "";
        } else
            return expandMacros(line);
    }

    private static final String DEFINE_DIRECTIVE = "#define ";
    private static char DOLLAR_SIGN = '$';
    private static char DOLLAR_REPLACEMENT = '\u9999';

    public void processDefineDirective(String line) {
        line = line.substring(DEFINE_DIRECTIVE.length());
        int spacePos = line.indexOf(' ');
        if (spacePos == -1) {
            System.err.println("Malformed directive: " +DEFINE_DIRECTIVE+line);
            return;
        }
        String macro, definition;
        macro = line.substring(0, spacePos);
        definition = line.substring(spacePos+1);
        int i;
        for (i = 0;   i < definition.length();   i++)
            if (definition.charAt(i) != ' ') break;
        definition = definition.substring(i);

        definition = definition.replace(DOLLAR_SIGN, DOLLAR_REPLACEMENT);

        StringBuffer regexp = new StringBuffer();

        int openParenPos = macro.indexOf('(');
        if (openParenPos == -1) {
            regexp.append("s\n\\b").append(macro).append("\\b\n")
                .append(definition).append("\ng");

        } else {
            StringTokenizer tok = new StringTokenizer(macro, "(,)");
            String argName, separator;

            macro = tok.nextToken();
            regexp.append("s\n\\b").append(macro);
            separator = "\\(";
            int argPos = 0;
            while (tok.hasMoreTokens()) {
                argName = tok.nextToken();
                argPos++;
                regexp.append(separator).append("([^(,)]*)");
                separator = ",";
                definition = perl.substitute
                    ("s\n\\b" + argName + "\\b\n$" + argPos + "\ng",
                     definition);
            }
            regexp.append("\\)\n").append(definition).append("\ng");
        }
        substitutions.put(macro, regexp.toString());
    }

    public String expandMacros(String line) {
        Iterator i = substitutions.values().iterator();
        while (i.hasNext()) try {
            line = perl.substitute((String) i.next(), line);
        } catch (MalformedPerl5PatternException mppe) {
            System.err.println(mppe);
        }
        return line.replace(DOLLAR_REPLACEMENT, DOLLAR_SIGN);
    }

    private String escapeDollars(String str) {
        return pspdash.StringUtils.findAndReplace(str, "$", "\\$");
    }

    public static void main(String [] argv) {
        try {
            CppFilter f = new CppFilter
                (new BufferedReader(new FileReader(argv[0])));
            String line;
            while ((line = f.readLine()) != null)
                System.out.println(line);
        } catch (IOException ioe) {}
    }

}
