// Copyright (C) 2010-2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

public class HtmlRenderedValue extends WrappedValue {

    public String html;

    public String tooltip;

    public HtmlRenderedValue(Object value, String html) {
        this(value, html, null);
    }

    public HtmlRenderedValue(Object value, String html, String tooltip) {
        this.value = value;
        this.tooltip = tooltip;
        if (html.startsWith("<html>"))
            this.html = html;
        else
            this.html = "<html>" + html + "</html>";
    }

}
