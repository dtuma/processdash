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

public class CppFilterReader extends StringReader {

    public CppFilterReader(BufferedReader in) throws IOException {
        super(filter(in));
    }

    private static String filter(BufferedReader in) throws IOException {
        StringBuffer result = new StringBuffer();
        String line;
        CppFilter filt = null;
        try {
            filt = new CppFilter(in);
            while ((line = filt.readLine()) != null)
                result.append(line).append("\n");
        } finally {
            if (filt != null) filt.dispose();
        }
        return result.toString();
    }
}
