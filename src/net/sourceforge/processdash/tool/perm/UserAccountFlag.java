// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class UserAccountFlag {

    private String code;

    private String msg;

    private String uri;

    private String baseUrl;

    public UserAccountFlag(Map<String, String> json, String baseUrl) {
        this(json.get("code"), json.get("msg"), json.get("uri"), baseUrl);
    }

    public UserAccountFlag(String code, String msg, String uri,
            String baseUrl) {
        this.code = code;
        this.msg = msg;
        this.uri = uri;
        this.baseUrl = baseUrl;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getUri() {
        return uri;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getHtml() {
        String html = HTMLUtils.escapeEntities(msg);
        if (StringUtils.hasValue(uri)) {
            html = StringUtils.findAndReplace(html, "{{", //
                "<a href='" + baseUrl + uri + "'>");
            html = StringUtils.findAndReplace(html, "}}", "</a>");
        }
        return html;
    }


    public static List<UserAccountFlag> parseList(List<Map> jsonFlagList,
            String baseUrl) {
        if (jsonFlagList == null || jsonFlagList.isEmpty())
            return Collections.EMPTY_LIST;

        List<UserAccountFlag> result = new ArrayList<UserAccountFlag>();
        for (Map oneFlag : jsonFlagList)
            result.add(new UserAccountFlag(oneFlag, baseUrl));
        return result;
    }

}
