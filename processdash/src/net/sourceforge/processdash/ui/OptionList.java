// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2007 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ui;

import javax.swing.*;

import net.sourceforge.processdash.i18n.*;
import net.sourceforge.processdash.ui.lib.*;
import net.sourceforge.processdash.util.*;

import java.util.*;

public class OptionList {

    public Vector options;
    public Map translations;
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
        if (Translator.isTranslating())
            translations = new HashMap();
        else
            translations = null;

        StringTokenizer tok = new StringTokenizer(optionString, "|");
        String oneOption, oneComment;
        int beg, end;
        while (tok.hasMoreTokens()) {
            oneOption = tok.nextToken().trim();
            beg = oneOption.indexOf('(');
            if (beg != -1) {
                end = oneOption.indexOf(')', beg);
                if (end != -1) {
                    oneComment = oneOption.substring(beg+1, end);
                    oneOption = oneOption.substring(0, beg).trim();
                    comments.put(oneOption, oneComment);
                }
            }
            this.options.add(oneOption);
            if (translations != null)
                translations.put(oneOption, Translator.translate(oneOption));
        }

        comments = Collections.unmodifiableMap(comments);
        if (translations != null)
            translations = Collections.unmodifiableMap(translations);
    }


    /** Construct a new option list containing the items in the
     * given collection.
     */
    public OptionList(Collection opts) {
        options = new Vector(opts);
        comments = Collections.EMPTY_MAP;
        if (Translator.isTranslating()) {
            translations = new HashMap();
            Iterator i = options.iterator();
            while (i.hasNext()) {
                String item = (String) i.next();
                String trans = Translator.translate(item);
                translations.put(item, trans);
            }
        }
    }


    /** Create a new option list that is a copy of another.
     * 
     * @param list the OptionList to copy
     */
    public OptionList(OptionList list) {
        options = new Vector(list.options);
        if (list.comments != null)
            comments = new HashMap(list.comments);
        if (list.translations != null)
            translations = new HashMap(list.translations);
    }


    public String getAsHTML(String name) {
        StringBuffer result = new StringBuffer();
        result.append("<SELECT NAME='")
            .append(HTMLUtils.escapeEntities(name)).append("'>\n");
        Iterator i = options.iterator();
        while (i.hasNext()) {
            String elem = (String) i.next();
            String trans = null;
            if (translations != null)
                trans = (String) translations.get(elem);
            elem = HTMLUtils.escapeEntities(elem);

            if (trans == null)
                result.append("<OPTION>").append(elem).append("\n");
            else
                result.append("<OPTION VALUE='").append(elem).append("'>")
                    .append(HTMLUtils.escapeEntities(trans)).append("\n");
        }
        result.append("</SELECT>");
        return result.toString();
    }

    public JComboBox getAsComboBox() {
        JComboBox result = new JComboBox((Vector) options.clone());

        if ((comments != null && !comments.isEmpty()) ||
            (translations != null && !translations.isEmpty())) {
            ToolTipCellRenderer renderer =
                new ToolTipCellRenderer(comments, translations);
            result.setRenderer(renderer);
        }

        return result;
    }
}
