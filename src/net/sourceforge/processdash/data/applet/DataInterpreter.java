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


package net.sourceforge.processdash.data.applet;


import java.util.Vector;

import net.sourceforge.processdash.data.MalformedValueException;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.RemoteException;
import net.sourceforge.processdash.data.repository.Repository;



public abstract class DataInterpreter implements DataListener {

    Repository data;
    String dataName = null;
    private SimpleData lastValue = null;
    SimpleData value = null;
                                  // if this value is true, this element should
                                  // always be reported as read-only, even if the
    boolean readOnly = false;     // underlying data element is editable.
    boolean unlocked = false;
    boolean active = false;

    /** If this value is true, then this data element is optional, and null
     *  values shouldn't be flagged with "?????". */
    boolean optional = false;
    boolean receivedEvent = false;
    protected boolean noConnection = false;
    HTMLField consumer = null;
    private DataListener changeListener = null;


    /** A string which the user can type into a data field to restore the
     * calculated default value for this data element. */
    public static final String RESTORE_DEFAULT_COMMAND = "DEFAULT";
    // possible future enhancement - read this from the user Settings?

    public static final StringData RESTORE_DEFAULT_TOKEN =
        StringData.create(RESTORE_DEFAULT_COMMAND);

    DataInterpreter(Repository r, String name, boolean readOnly) {
        data = r;
        dataName = name;
        this.readOnly = readOnly;

        try {
            data.addDataListener(dataName, this);
        } catch (RemoteException e) {
            noConnection = true;
        }
    }

    public void setConsumer(HTMLField c) {
        consumer = c;
        if (receivedEvent || noConnection) consumer.repositoryChangedValue();
    }

    private void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }

    public SimpleData getValue() {
        return value;
    }

    public Boolean getBoolean() {
        if (value != null && value.test())
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }

    public String getString() {
        if (noConnection) return "NO CONNECTION";
        if (value == null || !value.isDefined())
            return (optional ? "" : "?????");
        else
            return value.format();
    }


    public abstract void setBoolean(Boolean b);
    public abstract void setString(String s) throws MalformedValueException;

    public SimpleData getNullValue() { return null; }

    public boolean isEditable() {
        if (noConnection) return false;
        if (unlocked) return true;
        return (!readOnly && (value==null || value.isEditable()));
    }

    public void dataValueChanged(DataEvent e) {
        if (e != null) {
            if (active && changeListener != null && receivedEvent) try {
                changeListener.dataValueChanged(e);
                return;
            } catch (Exception ioe) {}

            receivedEvent = true;
            value = e.getValue();

            if (consumer != null)
                consumer.repositoryChangedValue();
        }
    }

    public void dataValuesChanged(Vector v) {
        if (v == null || v.size() == 0) return;
        for (int i = v.size();  i > 0; )
            dataValueChanged((DataEvent) v.elementAt(--i));
    }


    public void userChangedValue(Object newValue) {

        if (!unlocked && !isEditable()) {        // if this data is read-only,
                            // restore its old value in case the user messed it up.
            if (consumer != null) consumer.repositoryChangedValue();

        } else try {
                                    // parse the new value.
            lastValue = value;
            if (newValue instanceof Boolean)
                setBoolean((Boolean) newValue);
            else {
                String strval = null;
                if (newValue != null) strval = newValue.toString();
                if (strval == null || strval.length() == 0)
                    value = (optional ? null : getNullValue());
                else if (strval.equals(RESTORE_DEFAULT_COMMAND))
                    value = RESTORE_DEFAULT_TOKEN;
                else
                    setString(strval);
            }

                                      // if the data value has changed,
            if (value == null || lastValue == null ||
                !value.saveString().equals(lastValue.saveString())) {

                                      // save it to the repository.
                data.putValue(dataName, value);
                lastValue = value;
            }
                                      // This could be a MalformedValueException
                                      // (from the setString call) or RemoteException
        } catch (Exception e) {     // (from the putValue call).
            //System.out.println("Caught " + e); e.printStackTrace();
            value = lastValue;        // restore original value of element.
            if (consumer != null)     // restore the HTMLField to the old value.
                consumer.repositoryChangedValue();
        }
    }


    public void dispose(boolean dataRepositoryExists) {
        if (dataRepositoryExists) try {
            data.removeDataListener(dataName, this);
        } catch (RemoteException e) {
            printError(e);
        }
        data = null;
        dataName = null;
        consumer = null;
        value = lastValue = null;
        changeListener = null;
        active = false;
    }

    public void unlock() { unlocked = true; }

    public void setChangeListener(DataListener l) { changeListener = l; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isActive() { return active; }

}
