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

package pspdash.data;

import java.rmi.RemoteException;
import netscape.javascript.JSObject;
import java.util.Vector;

abstract public class DataField implements DataListener, InputListener {

    Repository data;
    boolean readOnly = false;
    String dataName = null;
    private SimpleData lastValue = null;
    SimpleData value = null;
    protected boolean noConnection = false;

    DataField(Repository r, String name, String defaultValue,
              String prefix, boolean readOnly) {
        data = r;
        dataName = name;
        this.readOnly = readOnly;
        setEditable(!readOnly);

        try {
            if (defaultValue.length() != 0) {
                defaultValue = instantiateValue(name, defaultValue, prefix);
                data.maybeCreateValue(name, defaultValue, prefix);
            }
            data.addDataListener(dataName, this);
        } catch (RemoteException e) {
            noConnection = true;
        }
    }

    private String instantiateValue(String name,
                                    String defaultValue,
                                    String prefix) {
        StringBuffer val = new StringBuffer();
        String digits;
        int pos;

        if (name.startsWith(prefix + "/"))
            name = name.substring(prefix.length()+1);

        pos = name.length();
        while ((pos > 0) && (Character.isDigit(name.charAt(pos-1)))) pos--;
        digits = name.substring(pos);

        while ((pos = defaultValue.indexOf('=')) != -1) {
            val.append(defaultValue.substring(0, pos));
            switch (defaultValue.charAt(pos+1)) {
                case '#':	val.append(digits); break;
                case 'p':	val.append(prefix); break;
                case 'n':	val.append(name); break;
            };
            defaultValue = defaultValue.substring(pos+2);
        }

        val.append(defaultValue);

        return val.toString();
    }

    private void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }


    /** resync this object with the user interface component.  The general
     * contract is that this method should examine the user interface
     * component, parse it as a SimpleData element, and save the result
     * in the this.value.  A MalformedValueException may be thrown if the
     * user interface component does not contain a valid value.  */
    abstract void parse() throws MalformedValueException;


    /** resync the user interface component with this object.  The general
     * contract is that this method should look at the this.value
     * variable and update the user interface accordingly.  A
     * ClassCastException may be thrown if this.value is of the wrong type. */
    abstract void paint() throws ClassCastException;


    /** (dis)allow the user from editing the associated user interface
     * element. */
    abstract void setEditable(boolean editable);


    public void dataValueChanged(DataEvent e)	{
        if (e != null)
            value = e.getValue();
        try {
            paint();
            setEditable(!readOnly && (value == null || value.isEditable()));
            lastValue = value;
        } catch (ClassCastException c) {
            value = lastValue;
            try { paint(); } catch (ClassCastException c2) {}
        }
    }


    public void dataValuesChanged(Vector v) {
        if (v == null || v.size() == 0) return;
        for (int i = v.size();  i > 0; )
            dataValueChanged((DataEvent) v.elementAt(--i));
    }


    public void event() {
                                // if this is read-only data,
        if (readOnly || (value != null && !value.isEditable()))
            try {			// repaint it in case the user messed it up.
                paint();
            } catch (ClassCastException e) {
                printError(e);
            } finally { return; }


        try {			// get the new user input.
            parse();
                                    // if the data value has changed,
            if (value == null || lastValue == null ||
                !value.saveString().equals(lastValue.saveString())) {

                                    // save it to the repository.
                data.putValue(dataName, value);
                lastValue = value;
            }

        } catch (MalformedValueException e) {
            value = lastValue;
            try { paint(); } catch (ClassCastException e2) {}

        } catch (RemoteException e) {
            printError(e);
        }
    }


    public void destroy(boolean dataRepositoryExists) {
        if (dataRepositoryExists)
            try {
                data.removeDataListener(dataName, this);
            } catch (RemoteException e) {
                printError(e);
            }
        data = null;
        dataName = null;
        value = lastValue = null;
    }

}
