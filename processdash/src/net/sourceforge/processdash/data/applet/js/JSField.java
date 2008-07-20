// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.applet.js;

import net.sourceforge.processdash.data.applet.DataApplet;
import net.sourceforge.processdash.data.applet.HTMLField;
import net.sourceforge.processdash.data.applet.InterpreterFactory;
import net.sourceforge.processdash.data.repository.Repository;

public class JSField extends HTMLField {

    String id;
    Object jsValue;
    JSFieldManager mgr;

    public JSField(String id, String dataName, JSFieldManager mgr,
                   Repository data, String dataPath) {
        this.id = id;
        this.mgr = mgr;

        if (data == null) {
            jsValue = variantValue = dataPath;
            redraw();
            return;
        }

        i = InterpreterFactory.create(data, dataName, dataPath);
        i.setConsumer(this);
    }


    public void userChangedValue(Object value) {
        jsValue = value;
        parse();
        if (i != null)
            i.userChangedValue(variantValue);
    }


    public void parse() {
        variantValue = (jsValue == null ? null : jsValue.toString());
    }

    public void fetch() {
        variantValue = i.getString();
    }

    private String asString(Object obj) {
        return (obj == null ? null : obj.toString());
    }

    public void redraw() {
        if (mgr != null) {
            mgr.addDataNotification(id, asString(variantValue), !isEditable());
        }
    }

    public void repositoryChangedValue() {
        fetch();
        redraw();
    }

    public void unlock() {
        super.unlock();
        redraw();
    }


    private static final String PKG_PREFIX =
        "net.sourceforge.processdash.data.applet.js.";
    protected void debug(String msg) {
        if (DataApplet.debug)
            // print this object's classname (minus package name) and the message
            System.out.println
                (getClass().getName().substring(PKG_PREFIX.length()) + ": " + msg);
    }


}
