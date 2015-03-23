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


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


import net.sourceforge.processdash.data.applet.*;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.Repository;


public class JSFieldManager implements HTMLFieldManager, DataListener {

    Hashtable inputListeners;
    Repository data = null;
    String dataPath = null;
    boolean isRunning, unlocked;
//  private DataApplet applet = null;
    private boolean debug = false;
    private boolean needsPageRefresh = false;


    public JSFieldManager(boolean unlocked) {
        debug("constructor starting");

        isRunning = true;
        inputListeners = new Hashtable();
        this.unlocked = unlocked;

        debug("constructor finished");
    }

    public JSFieldManager(DataApplet a) throws Exception {
        this(a.unlocked());
//      this.applet = a;
        debug = DataApplet.debug;
    }



    public void initialize(Repository data, String dataPath) {
        debug("initializing...");

        this.data = data;
        this.dataPath = dataPath;

        debug("initialization complete.");
    }


    public void dispose(boolean repositoryExists) {
        isRunning = false;
        if (!repositoryExists) data = null;

        try {
            debug("erasing listeners...");
            Enumeration keys = inputListeners.keys();
            while (keys.hasMoreElements())
                destroyInputListener(keys.nextElement());

        } catch (Exception e) { printError(e); }
        inputListeners = null;
        data = null;
        dataPath = null;
    }


    private void destroyInputListener(Object id) {
        JSField field = (JSField) inputListeners.get(id);
        if (field != null)
            field.dispose(data != null);
    }

    public void registerElement(String elementID, Object elementName,
                                Object elementType) {
        HTMLField f = null;

        try {
            int pos = -1;
            if (elementID == null || elementName == null)
                return;
            destroyInputListener(elementID);
            String name = elementName.toString();
            String type = elementType == null ? null : elementType.toString();
            debug("Initializing a " + elementType + " element named "+name);
            if (nameToAvoid(name)) return;

            if ("checkbox".equalsIgnoreCase(type))
                f = new JSCheckboxField(elementID, name, this, data, dataPath);
            else
                f = new JSField(elementID, name, this, data, dataPath);

            if (f != null) {
                debug("storing element");
                inputListeners.put(elementID, f);
                debug("customizing element");
                if (unlocked) f.unlock();
                f.maybeAddActiveListener(this);
            }
        } catch (Exception e) {
            printError(e);
        }
    }
    private boolean nameToAvoid(String name) {
        return (name == null || name.length() == 0 ||
                name.indexOf("NOT_DATA") != -1 ||
                "requiredTag".equalsIgnoreCase(name));
    }


    public void notifyListener(Object id, Object value) {
        debug("notifyListener called by an element with id "+id);
        JSField f = (JSField) inputListeners.get(id);

        debug("field="+f);
        if (f != null)
            f.userChangedValue(value);
    }

    public void dataValuesChanged(Vector v) { dataValueChanged(null); }
    public void dataValueChanged(DataEvent e) {
        needsPageRefresh = true;
        fireFormDataEvent("-1", "page-refresh", false);
    }


    protected void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }
    private void debug(String s) {
        if (DataApplet.debug)
            System.out.println("JSFieldManager: "+s);
    }

    Vector dataNotifications = new Vector();

    public void addDataNotification(String id, String value, boolean readOnly) {
        String roFlag = (readOnly ? "=" : "-");
        String notification = id + "," + roFlag + value;
        dataNotifications.addElement(notification);

        fireFormDataEvent(id, value, readOnly);
    }



    public String getDataNotification() {
          String result = (data == null ? null : "none");
          synchronized (dataNotifications) {
              if (dataNotifications.size() > 0) {
                  result = (String) dataNotifications.elementAt(0);
                  dataNotifications.removeElementAt(0);
              }
          }
          return result;
        }

    Vector formDataListeners = new Vector();
    public void addFormDataListener(FormDataListener l) {
        formDataListeners.add(l);
    }
    protected void fireFormDataEvent(String id, String value, boolean readOnly) {
        for (int i = 0;   i < formDataListeners.size();   i++) {
            FormDataListener l = (FormDataListener) formDataListeners.get(i);
            l.paintData(id, value, readOnly);
        }
    }
}
