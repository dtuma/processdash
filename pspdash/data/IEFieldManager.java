// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash.data;


import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Vector;

import com.ms.osp.*;


class IEFieldManager implements OLEDBSimpleProvider, HTMLFieldManager,
                                DataListener {


    IEField[] fields = null;
    OLEDBSimpleProviderListener listener = null;
    Repository data = null;
    String dataPath = null;       // may also store an error message
    int waitingOnElements;
    Vector dataInfo = null;
    boolean unlocked = false;
    DataApplet applet;


    public static final String checkboxType = new String("checkbox");

    class IEField extends HTMLField {

        public String fieldName = null;
        int idnum;
        boolean isCheckbox = false;

        boolean valueQueried = false;
        boolean heardFromRepository = false;



        public IEField(String name, int id) {
            fieldName = name;
            idnum = id;
        }


        public void initialize(String dataName, String fieldType) {
            if (data == null)
                changeValue(dataPath);
            else {
                isCheckbox = checkboxType.equalsIgnoreCase(fieldType);
                i = InterpreterFactory.create(data, dataName, dataPath);
                i.setConsumer(this);
                if (unlocked) unlock();
                if (i.isActive()) i.setChangeListener(IEFieldManager.this);
            }
        }


        public int getRWStatus() {
            // Internet Explorer expects that the RWStatus of a field is
            // static.  This means that it reads the RWStatus of a field
            // once (typically in response to the datasetComplete or
            // datasetChanged event), then never checks again.  When the
            // value of a field changes, IE requeries to get the new value,
            // but does not requery to see if the editability has changed.
            // As a result, IE causes problems when data is initially
            // read-only, then changes to become editable (e.g., thawing a
            // data element).
            //
            // To get around this IE deficiency, we tell the IE data binding
            // logic that everything is editable; then we handle editability
            // ourselves, in both JavaScript and in the DataInterpreter logic.

            return OSPRW.OSPRW_READWRITE;
        }


        private void changeValue(Object newValue) {
            if (valueQueried && listener != null) try {
                listener.aboutToChangeCell(1, idnum);
            } catch (Exception e) {}

            variantValue = newValue;

            if (valueQueried && listener != null) try {
                listener.cellChanged(1, idnum);
            } catch (Exception e) {}
        }


        public void repositoryChangedValue() {
            if (isCheckbox)
                changeValue(i.getBoolean());
            else
                changeValue(i.getString());

            if (!heardFromRepository) {
                heardFromRepository = true;
                synchronized (IEFieldManager.this) {
                    if (--waitingOnElements == 0) IEFieldManager.this.notifyAll();
                }
            }
        }


        public Object getVariant(int iRow) {
            if (iRow == 0)
                return fieldName;
            else {
                valueQueried = true;
                return variantValue;
            }
        }


        public void setVariant(Object var) {
            if (i != null) {
                changeValue(var);
                i.userChangedValue(var);
            }
        }


        private void debug(String s) {
            System.out.println("IEFieldManager["+fieldName+"]."+s);
        }
    }



    public IEFieldManager(DataApplet a) {
        Vector colNames = new Vector();
        int dataCount = 0;
        String dataName, fieldName;
        dataInfo = new Vector();
        unlocked = a.unlocked();
        this.applet = a;

                                    // scan the applet parameters for field specs
        while ((dataName = a.getParameter(fieldName="field"+dataCount)) != null) {
            colNames.addElement(fieldName);
            dataInfo.addElement(StringData.unescapeString(dataName));
            dataInfo.addElement(a.getParameter("type"+(dataCount++)));
        }

                                    // create and initialize the fields[] array
        fields = new IEField[dataCount];
        while (dataCount-- != 0)
            fields[dataCount] = new IEField((String) colNames.elementAt(dataCount),
                                            dataCount+1);
    }



    public void initialize(Repository data, String dataPath) {
        debug("initialize");
        this.data = data;
        this.dataPath = dataPath;
        waitingOnElements = (data == null ? 0 : fields.length);


        Enumeration d = dataInfo.elements();
        String dataName, fieldType;
        int i = 0;
        while (d.hasMoreElements()) {
            dataName = (String) d.nextElement();
            fieldType = (String) d.nextElement();
            fields[i++].initialize(dataName, fieldType);
        }
        synchronized (this) {
            if (waitingOnElements != 0) try {
                debug("waiting for everyone...");
                wait();
                debug("heard from everyone!");
            } catch (InterruptedException e){}
            else
                debug("we're all here! no need to wait.");
        }
        if (listener != null) try {
            debug("about to fire transfer complete");
            listener.transferComplete(OSPXFER.OSPXFER_COMPLETE);
            debug("transfer complete done firing.");
        } catch (Exception e) {}
        debug("initialize done");
    }


    public void notifyListener(Object id) {
        // not used by IEDataApplet
    }


    public void dispose(boolean repositoryExists) {
        debug("dispose");
        for (int i = fields.length; i-- != 0; )
            fields[i].dispose(repositoryExists);

        listener = null;
        fields = null;
    }


    public void dataValuesChanged(Vector v) { dataValueChanged(null); }
    public void dataValueChanged(DataEvent e) { applet.refreshPage(); }


    public int getRWStatus(int iRow,int iColumn) {
        int result;
        debug("getRWStatus("+iRow+","+iColumn+")");
        if (iColumn < 1)
            result = OSPRW.OSPRW_MIXED;
        else
            result = fields[iColumn-1].getRWStatus();
        debug("    returning "+result);
        return result;
    }


    public Object getVariant(int iRow, int iColumn, int formatType) {
        Object result = null;
        result = fields[iColumn-1].getVariant(iRow);
        debug("getVariant("+iRow+","+iColumn+") returning "+result);
        return result;
    }


    public void setVariant(int iRow,int iColumn, int formatType, Object var) {
        debug("setVariant("+iRow+","+iColumn+","+var+")");
        fields[iColumn-1].setVariant(var);
    }


    public boolean isEditable(String fieldName) {
        for (int i = fields.length; i-- != 0; )
            if (fields[i].fieldName.equals(fieldName))
                return fields[i].isEditable();
        debug("IEFieldManager couldn't find "+fieldName);
        return true;
    }


    public void addOLEDBSimpleProviderListener(OLEDBSimpleProviderListener l) {
        debug("addOLEDBSimpleProviderListener");
        listener = new OLEDBListenerWrapper(l);
        // listener = wrapListener(l);
    }

    public void removeOLEDBSimpleProviderListener(OLEDBSimpleProviderListener l){
        debug("removeOLEDBSimpleProviderListener");
        //listener = null;
    }


    public int getRowCount()      { return 1; }
    public int getEstimatedRows() { return 2; }
    public int getColumnCount()   { return fields.length; }
    public int isAsync()          { return 1; }

    public int find(int iStartRow, int iColumn, Object varSearchVal,
                    int findFlags, int compType) { return 0; }
    public int deleteRows(int iRow,int cRows) { return 0; }
    public int insertRows(int iRow,int cRows) { return 0; }
    public String getLocale() { return ""; }
    public void stopTransfer() {}

    private void debug(String s) { /*System.out.println("IEFieldManager."+s);*/}


    private OLEDBSimpleProviderListener wrapListener
        (OLEDBSimpleProviderListener listener)
    {
        try {
            Class wrapperClass = Class.forName("pspdash.data.OLEDBListenerWrapper");
            Constructor c = wrapperClass.getDeclaredConstructor(CONSTRUCTOR_ARGS);
            Object[] constructor_args = { listener };
            return (OLEDBSimpleProviderListener) c.newInstance(constructor_args);
        } catch (Throwable e) {
            OLEDBAlertWindow.display();
            return null;
        }
    }
    private static final Class[] CONSTRUCTOR_ARGS = {
        OLEDBSimpleProviderListener.class };
}
