// Copyright (C) 2013 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.api;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;


public class WebApiException extends RuntimeException {

    private String errorCode;

    private int httpCode;

    private Map attrs;

    public WebApiException(String errorCode, int httpCode, String description) {
        super(description);
        this.errorCode = errorCode;
        this.httpCode = httpCode;
        this.attrs = new HashMap();
    }

    public WebApiException putAttr(String name, Object value) {
        attrs.put(name, value);
        return this;
    }

    public WebApiException causedBy(Throwable t) {
        initCause(t);
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public Map getAttrs() {
        return attrs;
    }

    public JSONObject asJSON() {
        JSONObject err = new JSONObject();
        err.put("code", errorCode);
        err.put("msg", getMessage());
        err.putAll(attrs);

        JSONObject result = new JSONObject();
        result.put("stat", "fail");
        result.put("err", err);
        return result;
    }

}
