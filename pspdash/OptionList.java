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

import javax.swing.*;
import java.util.*;

public class OptionList {

    public Vector options;
    public Map comments;

    /** Construct a new option list based on the given string.
     *
     * @param optionString a pipe-delimited list of options.  Each option can
     * have an optional tool tip, enclosed in parentheses. Example: <PRE>
     *       foo|bar (comment for bar)|baz (comment for baz)|blah</PRE>
     * leading and trailing whitespace will be trimmed from each option.
     */
    public OptionList(String optionString) {
        options = new Vector();
        comments = new HashMap();

        StringTokenizer tok = new StringTokenizer(optionString, "|");
        String oneOption, oneComment;
        int beg, end;
        while (tok.hasMoreTokens()) {
            oneOption = tok.nextToken().trim();
            beg = oneOption.indexOf('(');
            if (beg != -1) {
                end = oneOption.indexOf(')', beg);
                oneComment = oneOption.substring(beg+1, end);
                oneOption = oneOption.substring(0, beg).trim();
                comments.put(oneOption, oneComment);
            }
            this.options.add(oneOption);
        }

        comments = Collections.unmodifiableMap(comments);
    }

    public String getAsHTML(String name) {
        StringBuffer result = new StringBuffer();
        result.append("<SELECT NAME='").append(name).append("'>\n");
        Iterator i = options.iterator();
        String option;
        while (i.hasNext())
            result.append("<OPTION>").append((String) i.next()).append("\n");
        result.append("</SELECT>");
        return result.toString();
    }

    public JComboBox getAsComboBox() {
        JComboBox result = new JComboBox((Vector) options.clone());

        if (!comments.isEmpty()) {
            ToolTipCellRenderer renderer = new ToolTipCellRenderer();
            result.setRenderer(renderer);

            Map.Entry oneComment;
            Iterator i = comments.entrySet().iterator();
            while (i.hasNext()) {
                oneComment = (Map.Entry) i.next();
                renderer.setToolTip((String) oneComment.getKey(),
                                    (String) oneComment.getValue());
            }
        }

        return result;
    }
}
