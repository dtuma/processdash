// Copyright (C) 2000-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.data.applet.ns;


import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.Repository;
import netscape.javascript.JSObject;


abstract class NSField extends HTMLField {

    Repository data;
    public JSObject element;

    public NSField(JSObject element, Repository data, String dataPath) {
        this.element = element;

        if (data == null) {
            variantValue = dataPath;
            redraw();
            return;
        }

        // redraw(); // necessary?

        String dataName = (String) element.getMember("name");
        i = InterpreterFactory.create(data, dataName, dataPath);
        i.setConsumer(this);
    }


    abstract public void paint(); // update the Html element with variantValue.
    abstract public void parse(); // update variantValue from the HTML element.
    abstract public void fetch(); // update variantValue from DataInterpreter i.


    private static Object EDITABLE = new Double(1.0);
    private static Object READONLY = new Double(0.0);

    public void redraw() {
        if (element != null) try {
            paint();

            boolean readOnly = !isEditable();
            element.setMember("className",
                              (readOnly ? "readOnlyElem" : "editableElem"));

            if (DataApplet.nsVersion() > 4) {
                Object style = element.getMember("style");
                if (style instanceof JSObject) try {
                                        // This will execute only in Netscape 6+
                    element.eval(isEditable() ? "readOnly=false;" : "readOnly=true;");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace();
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

    public void userEvent() {
        if (element != null) parse();
        if (i != null) i.userChangedValue(variantValue);
    }

    private static final String PKG_PREFIX =
        "net.sourceforge.processdash.data.applet.ns.";
    protected void debug(String msg) {
        if (DataApplet.debug)
            // print this object's classname (minus package name) and the message
            System.out.println
                (getClass().getName().substring(PKG_PREFIX.length()) + ": " + msg);
    }

}
