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


import java.util.Vector;


abstract class DataInterpreter implements DataListener {

    Repository data;
    String dataName = null;
    private SimpleData lastValue = null;
    SimpleData value = null;
                                  // if this value is true, this element should
                                  // always be reported as read-only, even if the
    boolean readOnly = false;	// underlying data element is editable.
    boolean receivedEvent = false;
    protected boolean noConnection = false;
    HTMLField consumer = null;


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


    public Boolean getBoolean() {
                                  // null data elements are false.
        if (value == null) return Boolean.FALSE;

                                    // numerically zero elements are false.
        if (value instanceof NumberData &&
            ((NumberData)value).getDouble() == 0.0) return Boolean.FALSE;

                                    // empty strings are false.
        if (value instanceof StringData &&
            ((StringData)value).getString().length() == 0) return Boolean.FALSE;

        else return Boolean.TRUE;	// everything else is true.
    }

    public String getString() {
        if (noConnection) return "NO CONNECTION";
        return (value == null ? "" : value.format());
    }


    public abstract void setBoolean(Boolean b);
    public abstract void setString(String s) throws MalformedValueException;

    public boolean isEditable() {
        return (!noConnection && !readOnly && (value==null || value.isEditable()));
    }

    public void dataValueChanged(DataEvent e) {
        if (e != null) {
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

        if (!isEditable()) {	// if this data is read-only,
                            // restore its old value in case the user messed it up.
            if (consumer != null) consumer.repositoryChangedValue();

        } else try {
                                    // parse the new value.
            lastValue = value;
            if (newValue instanceof Boolean)
                setBoolean((Boolean) newValue);
            else {
                String strval = newValue.toString();
                if (strval == null || strval.length() == 0)
                    value = null;
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
        } catch (Exception e) {	// (from the putValue call).
            value = lastValue;	// restore original value of element.
            if (consumer != null)	// restore the HTMLField to the old value.
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
    }

}
