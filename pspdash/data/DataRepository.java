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
// Foundation, Inc., 59 Temple Place -Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Vector;
import java.util.Stack;


public class DataRepository implements Repository {

    public static final String anonymousPrefix = "///Anonymous";

    /** a mapping of data names (Strings) to data values (DataElements) */
    Hashtable data = new Hashtable(8000, (float) 0.5);

    /** a backwards mapping of the above hashtable for data values that happen
     *  to be DataListeners.  key is a DataListener, value is a String. */
    Hashtable activeData = new Hashtable(2000, (float) 0.5);

    PrefixHierarchy repositoryListenerList = new PrefixHierarchy();

    Vector datafiles = new Vector();

    RepositoryServer dataServer = null;

    Hashtable PathIDMap = new Hashtable(20);
    Hashtable IDPathMap = new Hashtable(20);

    private class DataRealizer extends Thread {
        Stack dataElements = null;
        boolean terminate = false;

        public DataRealizer() {
            super("DataRealizer");
            dataElements = new Stack();
            setPriority(MIN_PRIORITY);
        }

        // when adding an element to the data Realizer, also restart it.
        public void addElement(DataElement e) { dataElements.push(e); interrupt(); }

        public void run() {
            // run this thread until ordered to terminate
            while (!terminate) {

                // if there is no data to realize, suspend this thread
                if (dataElements.isEmpty()) {
                    dataNotifier.highPriority();
                    try { sleep(Long.MAX_VALUE); } catch (InterruptedException i) {}
                } else try { // otherwise realize the data
                    sleep(100);
                    ((DataElement) dataElements.pop()).maybeRealize();
                } catch (Exception e) {}
            }

            // when terminating, clean up the dataElements stack
            while (!dataElements.isEmpty()){
                dataElements.pop();
            }
        }

        // command the process to terminate, and resume just in case it is
        // suspended
        public void terminate() {
            terminate = true;
            interrupt();
        }

    }

    DataRealizer dataRealizer;


    private class DataSaver extends Thread {
        public DataSaver() { start(); }
        public void run() {
            while (true) try {
                sleep(120000);         // save dirty datafiles every 2 minutes
                saveAllDatafiles();
            } catch (InterruptedException ie) {}
        }
    }

    DataSaver dataSaver = new DataSaver();


    private class DataFile {
        String prefix = null;
        String inheritsFrom = null;
        File file = null;
        int dirtyCount = 0;
    }


    // The DataElement class tracks the state of a single piece of data.
    private class DataElement {

        // the value of this element.  When data elements are created but not
        // initialized, their value is set to null.  Elements with null values
        // will not be saved out to any datafile.
        //
        private SaveableData value = null;
        private boolean deferred = false;

        // the datafile to which this element should be saved.  If this value
        // is null, the element will not be saved out to any datafile.
        //
        DataFile datafile = null;

        // a list of objects that are interested in changes to the value of this
        // element.  SPECIAL MEANINGS:
        //    1) a null value indicates that no objects have *ever* expressed an
        //       interest in this data element.
        //    2) a Vector with objects in it is a list of objects that should be
        //       notified if the value of this data element changes.
        //    3) an empty Vector indicates that, although some object(s) once
        //       expressed interest in this data element, no objects are
        //       interested any longer.
        //
        Vector dataListenerList = null;

        public DataElement() {}

        public SaveableData getValue() {
            if (deferred) realize();
            return value;
        }

        public SimpleData getSimpleValue() {
            if (deferred) realize();
            return value.getSimpleValue();
        }

        public synchronized void setValue(SaveableData d) {
            if (deferred = ((value = d) instanceof DeferredData))
                dataRealizer.addElement(this);
        }

        private synchronized void realize() {
            // since realize can be entered from several places, ensure someone
            // else didn't run it just before this call.
            if (deferred) {
                deferred = false;
                try {
                    value = ((DeferredData) value).realize();
                } catch (ClassCastException e) {
                } catch (MalformedValueException e) {
                    printError(e);
                    value = null;
                }
            }
        }

        public void maybeRealize() { if (deferred) realize(); }
    }

    private class DataNotifier extends Thread {
        Hashtable notifications = null;
        Hashtable activeListeners = null;
        private volatile boolean suspended = false;

        public DataNotifier() {
            super("DataNotifier");
            notifications = new Hashtable();
            activeListeners = new Hashtable();
            setPriority(MIN_PRIORITY);
        }

        public void highPriority() {
            setPriority(NORM_PRIORITY);
        }
        public void lowPriority()  {
            setPriority((MIN_PRIORITY + NORM_PRIORITY)/2);
        }

        public void dataChanged(String name, DataElement d) {
            if (name == null) return;
            if (d == null) d = (DataElement) data.get(name);
            if (d == null) return;

            Vector dataListenerList = d.dataListenerList;

            if (dataListenerList == null ||
                dataListenerList.size() == 0)
                return;

            DataListener dl;
            Hashtable elements;
            String listenerName;
            for (int i = dataListenerList.size();  i > 0; ) try {
                dl = ((DataListener) dataListenerList.elementAt(--i));
                listenerName = (String) activeData.get(dl);
                if (listenerName != null)
                    activeListeners.put(listenerName, dl);
                synchronized (notifications) {
                    elements = ((Hashtable) notifications.get(dl));
                    if (elements == null)
                        notifications.put(dl, elements =
                                          new Hashtable(2));
                }
                elements.put(name, d);
                dataChanged(listenerName, null);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Someone has been messing with dataListenerList while we're
                // iterating through it.  No matter...the worst that can happen
                // is that we will notify someone who doesn't care anymore, and
                // that is harmless.
            }

            if (suspended) synchronized(this) { notify(); }
        }

        public void addEvent(String name, DataElement d, DataListener dl) {
            if (name == null || dl == null) return;

            String listenerName = (String) activeData.get(dl);
            if (listenerName != null)
                activeListeners.put(listenerName, dl);

            Hashtable elements;
            synchronized (notifications) {
                elements = ((Hashtable) notifications.get(dl));
                if (elements == null)
                    notifications.put(dl, elements = new Hashtable());
            }
            elements.put(name, d);

            fireEvent(dl);
        }

        public void removeDataListener(String name, DataListener dl) {
            Hashtable h = (Hashtable) notifications.get(dl);
            if (h != null)
                h.remove(name);
        }

        public void deleteDataListener(DataListener dl) {
            notifications.remove(dl);
            String listenerName = (String) activeData.get(dl);
            if (listenerName != null)
                activeListeners.remove(listenerName);
        }

        private void fireEvent(DataListener dl) {
            if (dl == null) return;

            Hashtable elements = ((Hashtable) notifications.get(dl));
            if (elements == null) return;

            String listenerName = (String) activeData.get(dl);

            String name;
            DataElement d;
            DataListener activeListener;

                                    // run through the elements to see if any are
                                    // also expected to change, and do those first.
            Enumeration names = elements.keys();
            while (names.hasMoreElements()) {
                name = (String) names.nextElement();
                d    = (DataElement) elements.get(name);
                activeListener = (DataListener) activeListeners.get(name);
                if (activeListener != null)
                    fireEvent(activeListener);
            }

                                    // Build a list of data events to send
            elements = ((Hashtable) notifications.remove(dl));
            if (listenerName != null)
                activeListeners.remove(listenerName);
            if (elements == null) return;

            Vector dataEvents = new Vector();
            names = elements.keys();
            while (names.hasMoreElements()) {
                name = (String) names.nextElement();
                d    = (DataElement) elements.get(name);
                dataEvents.addElement(new DataEvent(DataRepository.this, name,
                                                    DataEvent.VALUE_CHANGED,
                                                    d.getValue() == null ? null :
                                                    d.getSimpleValue()));
            }

                                    // send the data events via dataValuesChanged()
            try {
                dl.dataValuesChanged(dataEvents);
            } catch (RemoteException rem) {
                System.err.println(rem.getMessage());
                System.err.println("    when trying to notify a datalistener.");
            } catch (Exception e) {
                // Various exceptions, most notably NullPointerException, can
                // occur if we erroneously notify a DataListener of changes *after*
                // it has unregistered for those changes.  Such mistakes can happen
                // due to multithreading, but no harm is done as long as the
                // exception is caught here.
            }
        }

        private boolean fireEvent() {
            try {
                fireEvent((DataListener) notifications.keys().nextElement());
                return true;
            } catch (java.util.NoSuchElementException e) {
                return false;
            }
        }

        public void run() {
            while (true) {
                if (fireEvent())
                    yield();
                else
                    doWait();
            }
        }

        private synchronized void doWait() {
            suspended = true;
            try { wait(); } catch (InterruptedException i) {}
            suspended = false;
        }

        public void flush() {
            while (fireEvent()) {}
        }
    }


    DataNotifier dataNotifier;
    Vector templateDirs = new Vector();


    public DataRepository() {
        includedFileCache.put("<dataFile.txt>", globalDataDefinitions);
        dataServer = new RepositoryServer(this);
        dataRealizer = new DataRealizer();
        dataNotifier = new DataNotifier();
        dataServer.start();
        dataRealizer.start();
        dataNotifier.start();
    }

    public void saveAllDatafiles() {
        DataFile datafile;

        for (int i = datafiles.size();   i-- != 0; ) {
            datafile = (DataFile)datafiles.elementAt(i);
            if (datafile.dirtyCount > 0)
                saveDatafile(datafile);
        }
    }

    public void finalize() {
        // Command data realizer to terminate, then wait for it to.
        dataRealizer.terminate();
        try {
            dataRealizer.join(6000);
        } catch (InterruptedException e) {}

        saveAllDatafiles();
        dataServer.quit();
    }


    public void addDatafileSearchDir(String directory) {
                                // ignore null input.
        if (directory == null) return;

                                  // add final directory separator if it
                                  // isn't already there.
        if (!directory.endsWith(File.separator))
            directory = directory + File.separator;

                                    // append path to search list.
        templateDirs.addElement(directory);
    }


    public synchronized void renameData (String oldPrefix, String newPrefix) {

        DataFile datafile = null;
        String datafileName = null;

                                  // find the datafile associated with 'prefix'
        for (int index = datafiles.size();  index-- > 0; ) {
            datafile = (DataFile) datafiles.elementAt(index);
            if (datafile.prefix.equals(oldPrefix)) {
                datafileName = datafile.file.getPath();
                break;
            }
        }

        if (datafileName != null) {

            // I'm commenting out this call, and the resume() call below, because they
            // are deprecated and no longer supported in JDK1.2;  But even worse, they
            // these two lines really had no effect in JDK1.1.  They look like they shut
            // down the dataServer, but in reality, all they do is prevent it from accepting
            // new connections from clients.  (None of the repositoryThreads are suspended.)
            // dataServer.suspend();

            remapIDs(oldPrefix, newPrefix);

                                    // close the datafile, then
            closeDatafile(oldPrefix);

            try {
                                    // open it again with the new prefix.
                openDatafile(newPrefix, datafileName);
            } catch (Exception e) {
                printError(e);
            }

            // dataServer.resume();

        } else {
            datafile = guessDataFile(oldPrefix+"/foo");
            if (datafile != null && datafile.prefix.length() == 0)
                remapDataNames(oldPrefix, newPrefix);
        }
    }

    private void remapDataNames(String oldPrefix, String newPrefix) {
//  Eventually this needs to rename data values in the global datafile.
//
//      Enumeration dataNames = data.keys();
//      String name;
//      oldPrefix = oldPrefix + "/";
//      while (dataNames.hasMoreElements()) {
//        name = (String) dataNames.nextElement();
//        if (name.startsWith(oldPrefix)) {
//          putValue();}
//      }
    }


    public synchronized void dumpRepository (PrintWriter out, Vector filt) {
        Enumeration k = data.keys();
        String name, value;
        DataElement  de;
        SaveableData sd;

                                  // first, realize all elements.
        while (k.hasMoreElements()) {
            name = (String) k.nextElement();
            ((DataElement)data.get(name)).maybeRealize();
        }

                                  // next, print out all element values.
        k = data.keys();
        while (k.hasMoreElements()) {
            name = (String) k.nextElement();
            if (pspdash.Filter.matchesFilter(filt, name)) {
                try {
                    de = (DataElement)data.get(name);
                    if (de.datafile != null) {
                        sd = de.getValue();
                        if (sd instanceof DateData) {
                            value = ((DateData)sd).formatDate();
                        } else if (sd instanceof StringData) {
                            value = ((StringData)sd).getString();
                        } else
                            value = de.getSimpleValue().toString();
                        if (value != null)
                            out.println(name + "," + value);
                    }
                } catch (Exception e) {
//        System.err.println("Data error:"+e.toString()+" for:"+name);
                }
            }
            Thread.yield();
        }
    }


    public synchronized void dumpRepository () {
        Enumeration k = data.keys();
        String name;
        DataElement element;

                                  // first, realize all elements.
        while (k.hasMoreElements()) {
            name = (String) k.nextElement();
            ((DataElement)data.get(name)).maybeRealize();
        }

                                  // next, print out all element values.
        k = data.keys();
        while (k.hasMoreElements()) {
            name = (String) k.nextElement();
            element = (DataElement)data.get(name);
            System.out.print(name);
            System.out.print("=" + element.getValue());
            if (element.dataListenerList != null)
                System.out.print(", listeners=" + element.dataListenerList);
            System.out.println();
        }
    }

    public synchronized void closeDatafile (String prefix) {
        //System.out.println("closeDatafile("+prefix+")");

        try {
            DataFile datafile = null;

                                    // find the datafile associated with 'prefix'
            Enumeration datafileList = datafiles.elements();
            while (datafileList.hasMoreElements()) {
                DataFile file = (DataFile) datafileList.nextElement();
                if (file.prefix.equals(prefix)) {
                    datafile = file;
                    break;
                }
            }


            if (datafile != null) {

                remapIDs(prefix, "///deleted//" + prefix);

                                      // save previous changes to the datafile.
                if (datafile.dirtyCount > 0)
                    saveDatafile(datafile);

                Enumeration k = data.keys();
                String name;
                DataElement element;
                DataListener dl;
                Vector elementsToRemove = new Vector();
                Hashtable affectedServerThreads = new Hashtable();

                                      // build a list of all the data elements of
                                      // this datafile.
                while (k.hasMoreElements()) {
                    name = (String) k.nextElement();
                    element = (DataElement)data.get(name);
                    if (element != null && element.datafile == datafile) {
                        elementsToRemove.addElement(name);
                        elementsToRemove.addElement(element);
                    }
                }

                                      // call the dispose() method on all the data
                                      // elements' values.
                for (int i = elementsToRemove.size();  i > 0; ) {
                    element = (DataElement) elementsToRemove.elementAt(--i);
                    name    = (String) elementsToRemove.elementAt(--i);
                    if (element.getValue() != null)
                        element.getValue().dispose();
                    element.datafile = null;
                }
                                      // remove the data elements.
                for (int i = elementsToRemove.size();  i > 0; ) {
                    element = (DataElement) elementsToRemove.elementAt(--i);
                    name    = (String) elementsToRemove.elementAt(--i);
                    removeValue(name);
                }
                                      // remove 'datafile' from the list of
                                      // datafiles in this repository.
                datafiles.removeElement(datafile);
            }

        } catch (Exception e) {
            printError(e);
        }
    }

    private DataElement add(String name, SaveableData value, DataFile f,
                            boolean notify) {

                                // Add the element to the table
        DataElement d = new DataElement();
        d.setValue(value);
        d.datafile = f;
        data.put(name, d);
        // System.out.println("DataRepository adding " + name + "=" +
        //                    (value == null ? "null" : value.saveString()));

        if (notify && !name.startsWith(anonymousPrefix))
            repositoryListenerList.dispatch
                (new DataEvent(this, name, DataEvent.DATA_ADDED, null));

        return d;
    }


    /** remove the named data element.
     * @param name             the name of the element to remove.
     */
    public synchronized void removeValue(String name) {

        DataElement removedElement = (DataElement)data.get(name);

        // if the named object existed in the repository,
        if (removedElement != null) {

            SimpleData oldValue;

            if (removedElement.getValue() == null)
                oldValue = null;
            else {
                oldValue = removedElement.getSimpleValue();
                removedElement.getValue().dispose();
            }

                                    // notify any repository listeners
            if (!name.startsWith(anonymousPrefix))
                repositoryListenerList.dispatch
                    (new DataEvent(this, name, DataEvent.DATA_REMOVED, oldValue));

                        // flag the element's datafile as having been modified
            if (removedElement.datafile != null)
                datafileModified(removedElement.datafile);

                                      // disown the element from its datafile,
            removedElement.datafile = null;
            if (removedElement.getValue() != null)
                removedElement.getValue().dispose();
            removedElement.setValue(null);     // erase its previous value,
            maybeDelete(name, removedElement); // and discard if appropriate.
        }
    }



    private DataFile guessDataFile(String name) {

        DataFile datafile;
        DataFile result = null;

        if (name.indexOf("//") == -1)
            for (int i = datafiles.size();   i-- != 0; ) {
                datafile = (DataFile)datafiles.elementAt(i);
                if (!datafile.file.canWrite()) continue;
                if (name.startsWith(datafile.prefix + "/") &&
                    ((result == null) ||
                     (datafile.prefix.length() > result.prefix.length())))
                    result = datafile;
            }

        return result;
    }



    public void maybeCreateValue(String name, String value, String prefix) {

        DataElement d = (DataElement)data.get(name);

        if (d == null || d.getValue() == null) try {
            SaveableData v = ValueFactory.create(name, value, this, prefix);
            if (d == null) {
                DataFile f = guessDataFile(name);
                d = add(name, v, f, true);
                datafileModified(f);
            } else
                putValue(name, v);
        } catch (MalformedValueException e) {
            printError(e);
            d.setValue(null);
        }
    }


//     private void maybeRealize(DataElement d) {
//
//       if ((d != null) && (d.value instanceof DeferredData))
//      synchronized (d) {
//        try {
//          d.value = ((DeferredData) d.value).realize();
//        } catch (ClassCastException e) {
//          // d.value isn't a DeferredData anymore .. someone beat us to it.
//        } catch (MalformedValueException e) {
//          printError(e);
//          d.value = null;
//        }
//      }
//     }


    public SaveableData getValue(String name) {

        DataElement d = (DataElement)data.get(name);
        if (d == null)
            return null;

        return d.getValue();
    }




    public final SimpleData getSimpleValue(String name) {
        DataElement d = (DataElement)data.get(name);
        if (d == null || d.getValue() == null)
            return null;
        else
            return d.getSimpleValue();
    }



    private static final int MAX_RECURSION_DEPTH = 100;
    private int recursion_depth = 0;

    public void putValue(String name, SaveableData value) {


        if (recursion_depth < MAX_RECURSION_DEPTH) {
            recursion_depth++;
            DataElement d = (DataElement)data.get(name);

            if (d != null) {


                                    // change the value of the data element.
                SaveableData oldValue = d.getValue();
                d.setValue(value);

                                      // possibly mark the datafile as modified.
                if (d.datafile != null &&
                    value != oldValue &&
                    (oldValue == null || value == null ||
                     !value.saveString().equals(oldValue.saveString())))
                    datafileModified(d.datafile);

                                        // possibly throw away the old value.
                if (oldValue != null && oldValue != value)
                    oldValue.dispose();

                                        // notify any listeners registed for the change
                dataNotifier.dataChanged(name, d);

                                      // check if this element is no longer needed.
                maybeDelete(name, d);

            } else {
                //  if the value was not already in the repository, add it.
                DataFile f = guessDataFile(name);
                add(name, value, f, true);
                datafileModified(f);
            }

            recursion_depth--;
        } else {
            System.err.println
                ("DataRepository detected circular dependency in data,\n" +
                 "    bailed out after " + MAX_RECURSION_DEPTH + " iterations.");
            new Exception().printStackTrace(System.err);
        }
    }


    private static final String includeTag = "#include ";
    private final Hashtable includedFileCache = new Hashtable();


    private InputStream findDatafile(String path, File currentFile) throws
        FileNotFoundException {
        InputStream result = null;
        File file = null;

                                  // find file in search path?
        if (path.startsWith("<")) {
                                          // strip <> chars
            path = path.substring(1, path.length()-1);

                                    // look in each search directory
                                    // until we find the named file
            for (Enumeration t = templateDirs.elements(); t.hasMoreElements(); )
                if ((file = new File(((String)t.nextElement() + path))).exists())
                    return new FileInputStream(file);

                                        // try locating the file in the classpath
            result = DataRepository.class.getResourceAsStream
                ("/Templates/" + path);
            if (result != null) return result;

                                    // couldn't find the file in any search
                                    // directory - give up.
            throw new FileNotFoundException("<" + path + ">");
        }

        if (path.startsWith("\""))
            path = path.substring(1, path.length()-1);

                                    // try opening the path as given.
        if ((file = new File(path)).exists()) return new FileInputStream(file);

                                  // if that fails, try opening it in the
                                  // same directory as currentFile.
        if (currentFile != null &&
            (file = new File(currentFile.getParent(), path)).exists())
            return new FileInputStream(file);

        throw new FileNotFoundException(path);    // fail.
    }


    // loadDatafile - opens the file passed to it and looks for "x = y" type
    // statements.  If one is found it associates x with y in the Hashtable
    // dest.  If an include statement is found on the first line, a recursive
    // call to loadDatafile is made, using the same Hashtable.  Return the
    // name of the include file, if one was found.

    private String loadDatafile(InputStream datafile, Map dest, boolean close)
        throws FileNotFoundException, IOException, InvalidDatafileFormat {

        // Initialize data, file, and read buffer.
        String inheritedDatafile = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(datafile));
        String line, name, value;
        int equalsPosition;

        try {
            line = in.readLine();

            // if the first line is an include statement, load the data from
            // the file specified in the include statement.
            if (line != null && line.startsWith(includeTag)) {
                inheritedDatafile = line.substring(includeTag.length()).trim();

                // Add proper exception handling in case someone is somehow using
                // the deprecated include syntax.
                if (inheritedDatafile.startsWith("\"")) {
                    System.err.println("datafile #include directives with relative" +
                                       " paths are no longer supported.");
                    throw new InvalidDatafileFormat();
                }

                Map cachedIncludeFile =
                    (Map) includedFileCache.get(inheritedDatafile);

                if (cachedIncludeFile == null) {
                    cachedIncludeFile = new HashMap();
                    // the null in the next line is a bug! it has no effect on
                    // #include <> statements, but effectively prevents #include ""
                    // statements from working (in other words, include directives
                    // relative to the current file.  Such directives are not
                    // currently used by the dashboard, so nothing will break.)
                    loadDatafile(findDatafile(inheritedDatafile, null),
                                 cachedIncludeFile, true);
                    cachedIncludeFile = Collections.unmodifiableMap(cachedIncludeFile);
                    includedFileCache.put(inheritedDatafile, cachedIncludeFile);
                }
                dest.putAll(cachedIncludeFile);
                line = in.readLine();
            }

            // find a line with a valid = assignment and load its data into
            // the destination Hashtable
            for( ;  line != null;  line = in.readLine()) {
                if (line.startsWith("=") || line.trim().length() == 0)
                    continue;

                if ((equalsPosition = line.indexOf('=', 0)) == -1)
                    throw new InvalidDatafileFormat();

                name = line.substring(0, equalsPosition);
                value = line.substring(equalsPosition+1);
                dest.put(name, value);
            }
        }
        finally {
            if (close) in.close();
        }

        return inheritedDatafile;
    }

    private Hashtable globalDataDefinitions = new Hashtable();

    public void addGlobalDefinitions(InputStream datafile, boolean close)
        throws FileNotFoundException, IOException, InvalidDatafileFormat {
        loadDatafile(datafile, globalDataDefinitions, close);
    }



    public void openDatafile(String dataPrefix, String datafilePath)
        throws FileNotFoundException, IOException, InvalidDatafileFormat {
        // debug("openDatafile");

        Hashtable values = new Hashtable();

        DataFile dataFile = new DataFile();
        dataFile.prefix = dataPrefix;
        dataFile.file = new File(datafilePath);
        dataFile.inheritsFrom =
            loadDatafile(new FileInputStream(dataFile.file), values, true);

                                // only add the datafile element if the
                                // loadDatafile process was successful
        datafiles.addElement(dataFile);

        boolean fileEditable = dataFile.file.canWrite();
        boolean dataEditable = true;
        boolean dataModified = false;

        String name, value;
        SaveableData o;
        DataElement d;

        Enumeration dataNames = values.keys();
        while (dataNames.hasMoreElements()) {
            name =  (String) dataNames.nextElement();
            value = (String) values.get(name);
            name = dataPrefix + "/" + name;

            if (value.startsWith("=")) {
                dataEditable = false;
                value = value.substring(1);
            } else
                dataEditable = true;

            if (value.equalsIgnoreCase("@now"))
                dataModified = true;
            try {
                o = ValueFactory.createQuickly(name, value, this, dataPrefix);
            } catch (MalformedValueException mfe) {
                System.err.println("Data value for '"+dataPrefix+"/"+name+
                                   "' in file '"+datafilePath+"' is malformed.");
                continue;
            }
            if (!fileEditable || !dataEditable)
                o.setEditable(false);
            d = (DataElement)data.get(name);
            if (d == null)
                d = add(name, o, dataFile, true);
            else {
                putValue(name, o);
                d.datafile = dataFile;
            }
        }

        if (dataModified)
            datafileModified(dataFile);

        // make a call to getID.  We don't need the resulting value, but
        // having made the call will cause an ID to be mapped for this
        // prefix.  This is necessary to allow users to bring up HTML pages
        // from their browser's history or bookmark list.
        //
        getID(dataPrefix);
        // debug("openDatafile done");
        }

    private static final int MAX_DIRTY = 10;


    private void datafileModified(DataFile datafile) {
        if (datafile != null && ++datafile.dirtyCount > MAX_DIRTY)
            saveDatafile(datafile);
    }

    // saveDataFile - saves a set of data to the appropriate data file.  In
    // order to minimize data loss, data is first written to two temporary
    // files, out and backup.  Once this is successful, out is renamed to
    // the actual datafile.  Once the rename is successful, backup is
    // deleted.
    private void saveDatafile(DataFile datafile) {
        synchronized(datafile) {
            // debug("saveDatafile");

            String fileSep = System.getProperty("file.separator");

            // Create temporary files
            File tempFile = new File(datafile.file.getParent() +
                                     fileSep + "tttt_" + datafile.file.getName());
            File backupFile = new File(datafile.file.getParent() + fileSep +
                                       "tttt" + datafile.file.getName() );
            BufferedWriter out;
            BufferedWriter backup;

            try {
                out = new BufferedWriter(new FileWriter(tempFile));
                backup = new BufferedWriter(new FileWriter(backupFile));

            } catch (IOException e) {
                System.err.println("IOException " + e + " while opening " +
                                   datafile.file.getPath() + "; save aborted");
                return;
            }

            Map defaultValues;

            // if the data file has an include statement, write it to the
            // the two temporary output files.
            if (datafile.inheritsFrom != null) {
                defaultValues = (Map) includedFileCache.get(datafile.inheritsFrom);
                try {
                    out.write(includeTag + datafile.inheritsFrom);
                    out.newLine();
                    backup.write(includeTag + datafile.inheritsFrom);
                    backup.newLine();
                } catch (IOException e) {}
            } else {
                defaultValues = new HashMap();
            }

            datafile.dirtyCount = 0;

            Enumeration k = data.keys();
            String name, valStr, defaultValStr;
            DataElement element;
            SaveableData value;
            int prefixLength = datafile.prefix.length() + 1;

            // write the data elements to the two temporary output files.
            while (k.hasMoreElements()) {
                name = (String)k.nextElement();
                element = (DataElement)data.get(name);

                // Make a quick check on the element and datafile validity
                // before taking the time to get the value
                if ((element != null) && (element.datafile == datafile)) {
                    value = element.getValue();
                    if (value != null) {
                        try {
                            name = name.substring(prefixLength);

                            valStr = (value.isEditable()? "" : "=") + value.saveString();
                            defaultValStr = (String) defaultValues.get(name);
                            if (valStr.equals(defaultValStr))
                                continue;

                            out.write(name);
                            out.write('=');
                            out.write(valStr);
                            out.newLine();

                            backup.write(name);
                            backup.write('=');
                            backup.write(valStr);
                            backup.newLine();
                        } catch (IOException e) {
                            System.err.println("IOException " + e + " while writing " +
                                               name + " to " + datafile.file.getPath());
                        }
                    }
                }
            }

            try {
                // Close the temporary output files
                out.flush();
                out.close();

                backup.flush();
                backup.close();

                // rename out to the real datafile
                datafile.file.delete();
                tempFile.renameTo(datafile.file);

                // delete the backup
                backupFile.delete();

            } catch (IOException e) {
                System.err.println("IOException " + e + " while closing " +
                                   datafile.file.getPath());
            }

            System.err.println("Saved " + datafile.file.getPath());
            // debug("saveDatafile done");
        }
    }



    public String makeUniqueName(String baseName) {
        // debug("makeUniqueName");
            int id = 0;

            if (baseName == null) baseName = "///Internal_Name";

            while (data.get(baseName + id) != null) id++;

            putValue(baseName + id, null);

    // debug("makeUniqueName done");
        return (baseName + id);
    }




    public void addDataListener(String name, DataListener dl) {
        DataElement d;
        synchronized (data) {
            d = (DataElement)data.get(name);
            if (d == null)
                d = add(name, null, guessDataFile(name), true);
        }

        if (d.dataListenerList == null) d.dataListenerList = new Vector();
        if (!d.dataListenerList.contains(dl))
            d.dataListenerList.addElement(dl);

        dataNotifier.addEvent(name, d, dl);
    }

    public void addActiveDataListener
        (String name, DataListener dl, String dataListenerName) {
        addDataListener(name, dl);
        activeData.put(dl, dataListenerName);
    }


    private void maybeDelete(String name, DataElement d) {

        if (d.dataListenerList == null) {
            if (d.getValue() == null)
                data.remove(name);            // throw it away.

        } else if (d.dataListenerList.isEmpty())

                                          // if no one cares about this element,
            if (d.getValue() == null)       // and it has no value,
                data.remove(name);            // throw it away.

                                              // if no one cares about this element
            else if (d.datafile == null) {  // and it has no datafile,
                data.remove(name);            // throw it away and
                d.getValue().dispose();       // dispose of its value.
            }

    }


    public void removeDataListener(String name, DataListener dl) {
        // debug("removeDataListener");
            DataElement d = (DataElement)data.get(name);

            if (d != null)
                if (d.dataListenerList != null) {

                    // remove the specified data listener from the list of data
                    // listeners for this element.  NOTE! We do not delete the
                    // dataListenerList here if it becomes empty...see the comments on
                    // the dataListenerList element of the DataElement construct.

                    d.dataListenerList.removeElement(dl);
                    dataNotifier.removeDataListener(name, dl);
                    maybeDelete(name, d);
                }
            // debug("removeDataListener done");
        }


    public void deleteDataListener(DataListener dl) {
        // debug("deleteDataListener");

        Enumeration dataElements = data.keys();
        String name = null;
        DataElement element = null;
        Vector listenerList = null;

                  // walk the hashtable, removing this datalistener.
        while (dataElements.hasMoreElements()) {
            name = (String) dataElements.nextElement();
            element = (DataElement) data.get(name);
            listenerList = element.dataListenerList;
            if (listenerList != null && listenerList.removeElement(dl))
                maybeDelete(name, element);
        }
        dataNotifier.deleteDataListener(dl);
        activeData.remove(dl);
        // debug("deleteDataListener done");
    }



    public void addRepositoryListener(RepositoryListener rl, String prefix) {
        // debug("addRepositoryListener");

                                  // add the listener to our repository list.
            repositoryListenerList.addListener(rl, prefix);

                                    // notify the listener of all the elements
                                    // already in the repository.
            Enumeration k = data.keys();
            String name;


            if (prefix != null && prefix.length() != 0)

                                    // if they have specified a prefix, notify them
                                    // of all the data beginning with that prefix.
                while (k.hasMoreElements()) {
                    if ((name = (String) k.nextElement()).startsWith(prefix))
                        rl.dataAdded(new DataEvent(this, name,
                                                   DataEvent.DATA_ADDED, null));
                }

            else                    // if they have specified no prefix, only
                                    // notify them of data that is NOT anonymous.
                while (k.hasMoreElements())
                    if (!(name = (String) k.nextElement()).startsWith(anonymousPrefix))
                        rl.dataAdded(new DataEvent(this, name,
                                                   DataEvent.DATA_ADDED, null));

            // debug("addRepositoryListener done");
    }



    public void removeRepositoryListener(RepositoryListener rl) {
        // debug("removeRepositoryListener");
        repositoryListenerList.removeListener(rl);
        // debug("removeRepositoryListener done");
    }

    public Enumeration keys() {
        return data.keys();
    }

    public Vector listDataNames(String prefix) {
        Vector result = new Vector();
        Enumeration names = keys();
        String name;
        while (names.hasMoreElements()) {
            name = (String) names.nextElement();
            if (name.startsWith(prefix))
                result.addElement(name);
        }
        return result;
    }

    private void debug(String msg) {
        System.out.println(msg);
    }

    private void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }

    public String getID(String prefix) {
        // if we already have a mapping for this prefix, return it.
        String ID = (String) PathIDMap.get(prefix);
        if (ID != null) return ID;

        // try to come up with a good ID Number for this prefix.  As a first
        // guess, use the hashCode of the path to the datafile for this prefix.
        // This way, with any luck, the same project will map to the same ID
        // Number each time the program runs (since the name of the datafile
        // will most likely never change after the project is created).

                                  // find the datafile associated with 'prefix'
        String datafileName = "null";
        for (int index = datafiles.size();  index-- > 0; ) {
            DataFile datafile = (DataFile) datafiles.elementAt(index);
            if (datafile.prefix.equals(prefix)) {
                datafileName = datafile.file.getPath();
                break;
            }
        }
                                  // compute the hash of the datafileName.
        int IDNum = datafileName.hashCode();
        ID = Integer.toString(IDNum);

                  // if that ID Number is taken,  increment and try again.
        while (IDPathMap.containsKey(ID))
            ID = Integer.toString(++IDNum);

                    // store the ID-path pair in the hashtables.
        PathIDMap.put(prefix, ID);
        IDPathMap.put(ID, prefix);
        return ID;
    }

    public String getPath(String ID) {
        return (String) IDPathMap.get(ID);
    }

    private void remapIDs(String oldPrefix, String newPrefix) {
        String ID = (String) PathIDMap.remove(oldPrefix);

        if (ID != null) {
            PathIDMap.put(newPrefix, ID);
            IDPathMap.put(ID, newPrefix);
        }

        dataServer.deletePrefix(oldPrefix);
    }

}
