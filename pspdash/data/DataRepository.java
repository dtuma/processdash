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

import pspdash.ErrorReporter;
import pspdash.PerlPool;
import pspdash.EscapeString;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Stack;
import com.oroinc.text.perl.Perl5Util;
import com.oroinc.text.MalformedCachePatternException;

import pspdash.RobustFileWriter;

import pspdash.data.compiler.CompilationException;
import pspdash.data.compiler.CompiledScript;
import pspdash.data.compiler.Compiler;
import pspdash.data.compiler.ExpressionContext;
import pspdash.data.compiler.ExecutionException;
import pspdash.data.compiler.ListStack;
import pspdash.data.compiler.analysis.DepthFirstAdapter;
import pspdash.data.compiler.lexer.Lexer;
import pspdash.data.compiler.lexer.LexerException;
import pspdash.data.compiler.node.ASearchDeclaration;
import pspdash.data.compiler.node.ASimpleSearchDeclaration;
import pspdash.data.compiler.node.AIncludeDeclaration;
import pspdash.data.compiler.node.ANewStyleDeclaration;
import pspdash.data.compiler.node.AOldStyleDeclaration;
import pspdash.data.compiler.node.AReadOnlyAssignop;
import pspdash.data.compiler.node.AUndefineDeclaration;
import pspdash.data.compiler.node.Start;
import pspdash.data.compiler.node.TIdentifier;
import pspdash.data.compiler.node.TStringLiteral;
import pspdash.data.compiler.parser.Parser;
import pspdash.data.compiler.parser.ParserException;

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
        RepositoryServer secondaryDataServer = null;

        Hashtable PathIDMap = new Hashtable(20);
        Hashtable IDPathMap = new Hashtable(20);

        HashSet dataElementNameSet = new HashSet();
        Set dataElementNameSet_ext =
            Collections.unmodifiableSet(dataElementNameSet);

        /** Sets the policy for auto-realization of deferred data. Possible values:
         *  Boolean.TRUE - auto realize all data
         *  Boolean.FALSE - don't auto realize any data
         *  a DataFile object - only autorealize data for this file. */
        Object realizeDeferredDataFor = Boolean.FALSE;

        private class DataRealizer extends Thread {
            Stack dataElements = null;
            boolean terminate = false;

            public DataRealizer() {
                super("DataRealizer");
                dataElements = new Stack();
                setPriority(MIN_PRIORITY);
            }

            // when adding an element to the data Realizer, also restart it.
            public void addElement(DataElement e) {
                dataElements.push(e);
                interrupt();
            }

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

        public void setRealizationPolicy(String policy) {
            if ("full".equalsIgnoreCase(policy))
                realizeDeferredDataFor = Boolean.TRUE;
            else if ("min".equalsIgnoreCase(policy))
                realizeDeferredDataFor = "";
            else
                realizeDeferredDataFor = Boolean.FALSE;
        }


        private class DataSaver extends Thread {
            public DataSaver() { start(); }
            public void run() {
                while (true) try {
                    sleep(120000);         // save dirty datafiles every 2 minutes
                    saveAllDatafiles();
                    System.gc();
                } catch (InterruptedException ie) {}
            }
        }

        DataSaver dataSaver = new DataSaver();


        private class DataFile {
            String prefix = null;
            String inheritsFrom = null;
            File file = null;
            int dirtyCount = 0;

            public void invalidate() { file = null; }
        }


        // The DataElement class tracks the state of a single piece of data.
        private class DataElement {

            // The name of this element.
            private String name;

            // the value of this element.  When data elements are created but not
            // initialized, their value is set to null.  Elements with null values
            // will not be saved out to any datafile.
            //
            private SaveableData value = null;
            private volatile SimpleData simpleValue = null;
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

            // a preconstructed event for dispatching to listeners (so a new event
            // need not be constructed each time).
            //
            private volatile DataEvent event = null;

            public DataElement(String name) {
                this.name = name;
            }

            public SaveableData getValue() {
                if (deferred) realize();
                return value;
            }

            public SimpleData getSimpleValue() {
                if (value == null) return null;
                if (simpleValue != null) return simpleValue;
                if (deferred) realize();
                return (simpleValue = value.getSimpleValue());
            }

            public SaveableData getImmediateValue() {
                return value;
            }

            public synchronized void setValue(SaveableData d) {
                event = null;
                simpleValue = null;
                if (deferred = ((value = d) instanceof DeferredData))
                    if (realizeDeferredDataFor == datafile ||
                        realizeDeferredDataFor == Boolean.TRUE)
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
                        value = new MalformedData(value.saveString());
                    }
                }
            }

            public synchronized void disposeValue() {
                if (value != null) value.dispose();
                deferred = false;
            }

            public void maybeRealize() { if (deferred) realize(); }

            public DataEvent getDataChangedEvent() {
                DataEvent result = event;
                if (result == null || result.getID() != DataEvent.VALUE_CHANGED)
                    event = result = new DataEvent(DataRepository.this, name,
                                                   DataEvent.VALUE_CHANGED,
                                                   getSimpleValue());
                return result;
            }

            public DataEvent getDataAddedEvent() {
                DataEvent result = event;
                if (result == null || result.getID() != DataEvent.DATA_ADDED)
                    event = result = new DataEvent(DataRepository.this, name,
                                                   DataEvent.DATA_ADDED, null);
                return result;
            }
        }

        private class DataNotifier extends Thread {

            /** A list of the notifications we need to perform.
             *
             * the <B>keys</B> in the hashtable are DataListeners that need to be
             * notified of changes in data.
             *
             * the <b>values</b> are separate hashtables.  The keys of these
             * subhashtables name data elements that have changed, which the
             * listener is interested in.  The values in these subhashtables
             * are the named DataElements.
             */
            Hashtable notifications = null;

            /** A list of active listeners.  (An active listener is one that is going
             * to perform a recalculation as soon as it is notified of a data change.
             * That recalculation will probably trigger other data notifications.)
             *
             * The <b>keys</b> in the hashtable are the names of the data elements
             * which will be recalculated when we notify the DataListener which
             * is stored as the <b>value</b> in the hashtable.
             *
             * This data structure is basically a backward mapping of the
             * DataRepository's <code>activeData</code> structure, for only those
             * DataListeners which appear in the <code>notifications</code> list
             * above.
             */
            Hashtable activeListeners = null;

            /** a list of misbehaved data which appears to be circularly defined. */
            Hashtable circularData = new Hashtable();

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

            /** Determine all the notifications that will need to be made as
             * a result of a change to given <code>DataElement</code> with
             * the given <code>name</code>, and add those notifications to
             * our internal data structures.
             */
            public void dataChanged(String name, DataElement d) {
                if (name == null) return;
                if (d == null) d = (DataElement) data.get(name);
                if (d == null) return;
                if (circularData.get(name) != null) return;

                Vector dataListenerList = d.dataListenerList;

                if (dataListenerList == null ||
                    dataListenerList.size() == 0)
                    return;

                DataListener dl;
                String listenerName;
                boolean notifyActiveListener;
                for (int i = dataListenerList.size();  i > 0; ) try {
                    dl = ((DataListener) dataListenerList.elementAt(--i));
                    listenerName = (String) activeData.get(dl);
                    if (listenerName == null)
                        notifyActiveListener = false;
                    else if (activeListeners.put(listenerName, dl) != null)
                        notifyActiveListener = false;
                    else
                        notifyActiveListener = true;
                    getElementsForDataListener(dl).put(name, d);
                    if (notifyActiveListener) dataChanged(listenerName, null);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Someone has been messing with dataListenerList while we're
                    // iterating through it.  No matter...the worst that can happen
                    // is that we will notify someone who doesn't care anymore, and
                    // that is harmless.
                }

                if (suspended) synchronized(this) { notify(); }
            }

            private Hashtable getElementsForDataListener(DataListener dl) {
                Hashtable elements = null;
                synchronized (notifications) {
                    elements = ((Hashtable) notifications.get(dl));
                    if (elements == null) {
                        notifications.put(dl, elements = new Hashtable(2));
                        checkConsistency();
                    }
                }
                return elements;
            }

            public void addEvent(String name, DataElement d, DataListener dl) {
                if (name == null || dl == null) return;

                String listenerName = (String) activeData.get(dl);
                if (listenerName != null)
                    activeListeners.put(listenerName, dl);

                getElementsForDataListener(dl).put(name, d);

                fireEvent(dl);
            }

            public void removeDataListener(String name, DataListener dl) {
                Hashtable h = (Hashtable) notifications.get(dl);
                if (h != null)
                    h.remove(name);
            }

            public void deleteDataListener(DataListener dl) {
                synchronized (notifications) {
                    notifications.remove(dl);
                    checkConsistency();
                }
                String listenerName = (String) activeData.get(dl);
                if (listenerName != null)
                    activeListeners.remove(listenerName);
            }

            private void fireEvent(DataListener dl) {
                if (dl == null) return;

                Hashtable elements = ((Hashtable) notifications.get(dl));
                if (elements == null) return;

                String listenerName = (String) activeData.get(dl);

                synchronized (elements) {
                    if (notifications.get(dl) == null) return;

                    Thread t = (Thread) elements.get(CIRCULARITY_TOKEN);

                    if (t == null)
                        elements.put(CIRCULARITY_TOKEN, Thread.currentThread());
                    else if (t != Thread.currentThread()) {
                        //System.out.println("waiting for other thread...");
                        try { elements.wait(1000); } catch (InterruptedException ie) {}
                        //System.out.println("waiting done.");
                        return;
                    } else {
                        if (listenerName != null) {
                            System.err.println("Infinite recursion encountered while " +
                                               "recalculating " + listenerName +
                                               " - ABORTING");
                            circularData.put(listenerName, Boolean.TRUE);
                        }
                        return;
                    }
                }

                String name;
                DataElement d;
                DataListener activeListener;

                                        // run through the elements to see if any are
                                        // also expected to change, and do those first.
                Enumeration names = elements.keys();
                while (names.hasMoreElements()) {
                    name = (String) names.nextElement();
                    if (name == CIRCULARITY_TOKEN) continue;
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

                try {
                    elements.remove(CIRCULARITY_TOKEN);
                    Vector dataEvents = new Vector();
                    names = elements.keys();
                    while (names.hasMoreElements()) {
                        name = (String) names.nextElement();
                        d    = (DataElement) elements.get(name);
                        dataEvents.addElement(d.getDataChangedEvent());
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
                } finally {
                    synchronized (elements) { elements.notifyAll(); }
                    checkConsistency();
                }
            }

            private volatile boolean notifierIsInconsistent = false;
            private final boolean ENABLE_NOTIFICATION_BASED_INCONSISTENCY = false;
            private void checkConsistency() {
                if (ENABLE_NOTIFICATION_BASED_INCONSISTENCY)
                    synchronized (notifications) {
                        boolean isInconsistent = !notifications.isEmpty();
                        if (isInconsistent == notifierIsInconsistent) return;
                        notifierIsInconsistent = isInconsistent;
                        if (notifierIsInconsistent)
                            startInconsistency();
                        else
                            finishInconsistency();
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
                while (true) try {
                    if (fireEvent())
                        yield();
                    else
                        doWait();
                } catch (Exception e) {}
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
        private static final String CIRCULARITY_TOKEN = "CIRCULARITY_TOKEN";

        DataNotifier dataNotifier;


        private static Perl5Util perl = PerlPool.get();
        private class DataFreezer extends Thread implements RepositoryListener,
                                                            DataConsistencyObserver
        {

            /** Keys in this hashtable are the String names of freeze tag
             * data elements.  Values are the FrozenDataSets to which they
             * refer. */
            private Hashtable frozenDataSets;

            /** A list of names of data elements which need to be frozen. */
            private SortedSet itemsToFreeze;

            /** A list of names of data elements which need to be thawed. */
            private SortedSet itemsToThaw;

            /** Flag indicating that we've received a request to terminate. */
            private volatile boolean terminate = false;

            public DataFreezer() {
                frozenDataSets = new Hashtable();
                itemsToFreeze = Collections.synchronizedSortedSet(new TreeSet());
                itemsToThaw = Collections.synchronizedSortedSet(new TreeSet());
                addRepositoryListener(this, "");
            }

            public void run() {
                // run this thread until ordered to terminate
                while (!terminate) {
                    // Wait until the data is consistent - don't freeze or thaw anything
                    // while files are being opened and closed.
                    addDataConsistencyObserver(this);

                    // Sleep until we're needed again.
                    if (!terminate)
                        try { sleep(Long.MAX_VALUE); } catch (InterruptedException i) {}
                }

                // On termination, make one last sweep for data to freeze.
                dataIsConsistent();
            }

            public void dataIsConsistent() {
                // Perform all requested work.
                MAX_DIRTY = Integer.MAX_VALUE;
                freezeAll();
                thawAll();
                MAX_DIRTY = 10;
                saveAllDatafiles();
            }

            /** Freeze all waiting items. */
            private void freezeAll() {
                String item;
                while ((item = pop(itemsToFreeze)) != null)
                    performFreeze(item);
            }

            /** Thaw all waiting items. */
            private void thawAll() {
                String item;
                while ((item = pop(itemsToThaw)) != null)
                    performThaw(item);
            }

            /** Pop the first item off a sorted set, in a thread-safe fashion.
             * @return a item which has been removed from the set, or null if
             *  the set is empty.
             */
            private String pop(SortedSet set) {
                synchronized(set) {
                    if (set.isEmpty())
                        return null;
                    else {
                        String result = (String) set.first();
                        set.remove(result);
                        return result;
                    }
                }
            }

            public void terminate() {
                // Stop listening for events.
                removeRepositoryListener(this);

                // stop this thread (if the thread is currently awake, this will
                // not have an immediate effect.)
                terminate = true;
                interrupt();
            }

            public void dataAdded(DataEvent e) {
                String dataName = e.getName();
                if (isFreezeFlagElement(dataName) &&
                    !frozenDataSets.containsKey(dataName))
                    frozenDataSets.put(dataName, new FrozenDataSet(dataName));
            }

            public void dataRemoved(DataEvent e) {
                String dataName = e.getName();
                if (!isFreezeFlagElement(dataName)) return;
                FrozenDataSet set = (FrozenDataSet) frozenDataSets.remove(dataName);
                if (set != null)
                    set.dispose();
            }

            private boolean isFreezeFlagElement(String dataName) {
                return (dataName.indexOf(FREEZE_FLAG_TAG) != -1);
            }

            /** Perform the work required to freeze a data value. */
            private void performFreeze(String dataName) {
                DataElement element = (DataElement) data.get(dataName);
                if (element == null) return;

                // Make certain no data values are currently in a state of flux
                dataNotifier.flush();

                // This will realize the value if it is deferred
                SaveableData value = element.getValue();

                // For now, lets add this in - don't doubly freeze data.  Supporting
                // double freezing of data might make it easier for the people who
                // write freeze flag expressions, but it makes things more confusing
                // for end users:
                //  * data items that are frozen by multiple freeze flags perplex
                //    the user: they toggle some boolean value and can't figure out
                //    why the data isn't thawing
                //  * sometimes it is possible for data accidentally to become doubly
                //    frozen by the SAME freeze flag.  Then users toggle the flag and
                //    their data toggles between frozen and doubly frozen.
                if (value instanceof FrozenData)
                    return;

                //System.out.println("freezing " + dataName);

                // Determine the prefix of the data element.
                String prefix = "";
                if (element.datafile != null)
                    prefix = element.datafile.prefix;

                // Lookup the default value of this data element.
                String defVal = lookupDefaultValue(dataName, element);

                // Don't freeze null data elements when there is no default value.
                if (value == null && defVal == null) return;

                // Create the frozen version of the value.
                SaveableData frozenValue = new FrozenData
                    (dataName, value, DataRepository.this, prefix, defVal);

                // Save the frozen value to the repository.
                putValue(dataName, frozenValue);
            }

            /** Perform the work required to thaw a data value. */
            private void performThaw(String dataName) {
                DataElement element = (DataElement) data.get(dataName);
                if (element == null) return;

                SaveableData value = element.getImmediateValue(), thawedValue;
                if (value instanceof FrozenData) {
                    //System.out.println("thawing " + dataName);
                    // Thaw the value.
                    FrozenData fd = (FrozenData) value;
                    thawedValue = fd.thaw();
                    if (thawedValue == FrozenData.DEFAULT)
                        thawedValue = instantiateValue
                            (dataName, fd.getPrefix(),
                             lookupDefaultValueObject(dataName, element), false);

                    // Save the thawed value to the repository.
                    putValue(dataName, thawedValue);
                }
            }

            /** Register the named data element for freezing.
             *
             * The element is not frozen immediately, but rather added to a
             * queue for freezing sometime in the future.
             */
            public synchronized void freeze(String dataName) {
                if (itemsToThaw.remove(dataName) == false)
                    itemsToFreeze.add(dataName);
            }

            /** Register the named data element for thawing.
             *
             * The element is not thawed immediately, but rather added to a
             * queue for thawing sometime in the future.
             */
            public synchronized void thaw(String dataName) {
                if (itemsToFreeze.remove(dataName) == false)
                    itemsToThaw.add(dataName);
            }

            private class FrozenDataSet implements DataListener,
                                                   RepositoryListener,
                                                   DataConsistencyObserver {

                String freezeFlagName;
                String freezeRegexp;
                Set dataItems;
                int currentState = FDS_GRANDFATHERED;
                boolean observedFlagValue;
                volatile boolean initializing;
                Set tentativeFreezables;
                char[] buffer = null;

                public FrozenDataSet(String freezeFlagName) {
                    this.freezeFlagName = freezeFlagName;

                    //System.out.println("creating FrozenDataSet for " + freezeFlagName);

                    // Fetch the prefix and the regular expression.
                    int pos = freezeFlagName.indexOf(FREEZE_FLAG_TAG);
                    if (pos == -1) return; // shouldn't happen!

                    String prefix = freezeFlagName.substring(0, pos+1);
                    this.freezeRegexp = "m\n^" + ValueFactory.regexpQuote(prefix) +
                        freezeFlagName.substring(pos+FREEZE_FLAG_TAG.length()) + "$\n";

                    this.initializing = true;
                    this.tentativeFreezables = new HashSet();
                    this.dataItems = Collections.synchronizedSet(new HashSet());

                    addDataListener(freezeFlagName, this);

                    addRepositoryListener(this, prefix);
                }

                public synchronized void dispose() {
                    removeRepositoryListener(this);
                    dataItems.clear();
                    deleteDataListener(this);
                }

                private void freeze(String itemName) {
                    if (initializing) tentativeFreezables.add(itemName);
                    else DataFreezer.this.freeze(itemName);
                }

                private void freezeAll(Set dataItems) {
                    synchronized (dataItems) {
                        Iterator i = dataItems.iterator();
                        String itemName;
                        while (i.hasNext()) {
                            itemName = (String) i.next();
                            freeze(itemName);
                        }
                    }
                    interrupt();          // this interrupts the DataFreezer thread.
                }

                private void thawAll(Set dataItems) {
                    synchronized (dataItems) {
                        Iterator i = dataItems.iterator();
                        String itemName;
                        while (i.hasNext()) {
                            itemName = (String) i.next();
                            thaw(itemName);
                        }
                    }
                    interrupt();          // this interrupts the DataFreezer thread.
                }

                // The next two methods implement the DataListener interface.

                public void dataValueChanged(DataEvent e) {
                    if (! freezeFlagName.equals(e.getName())) return;
                    observedFlagValue = (e.getValue() != null && e.getValue().test());
                    addDataConsistencyObserver(this);
                }

                public void dataValuesChanged(Vector v) {
                    if (v == null || v.size() == 0) return;
                    for (int i = v.size();  i > 0; )
                        dataValueChanged((DataEvent) v.elementAt(--i));
                }

                /** Respond to a change in the value of the freeze flag.
                 *  The state transition diagram is: <PRE>
                 *
                 *     current
                 *     state     freeze flag = TRUE         freeze flag = FALSE
                 *     -------   ------------------         -----------------------
                 *     FROZEN    no change                  set to thawed; thaw all
                 *     GRAND     no change                  set to thawed
                 *     THAWED    set to frozen; freeze all  no change
                 *
                 * </PRE>
                 */
                public void dataIsConsistent() {
                    //System.out.println(freezeFlagName + " = "+ observedFlagValue);
                    synchronized (this) {
                        if (observedFlagValue == true) {
                            // data should be frozen or grandfathered.
                            if (currentState == FDS_THAWED) {
                                currentState = FDS_FROZEN;
                                freezeAll(dataItems);
                            }

                        } else {            // data should be thawed.
                            if (currentState == FDS_FROZEN && !initializing)
                                thawAll(dataItems);
                            currentState = FDS_THAWED;
                        }

                        if (initializing) {
                            initializing = false;
                            if (currentState == FDS_FROZEN)
                                freezeAll(tentativeFreezables);
                            tentativeFreezables = null;
                        }
                    }
                }

                /** Respond to a notification about a data element that has been
                 *  added to the repository.
                 *
                 *  (Note that this happens during initial opening of
                 *  datafiles as well as on an ongoing basis as new elements
                 *  are created.) The state transition diagram is: <PRE>
                 *
                 *     current
                 *     state     item = THAWED        item = FROZEN
                 *     -------   -------------------  -------------
                 *     FROZEN    freeze the item (1)  no action
                 *     GRAND     no action            set to frozen; freeze all
                 *     THAWED    no action            no action (2)
                 *
                 * </PRE>
                 * Notes:<P>
                 * (1) This situation would most likely occur as the result of
                 *     freezing a project, then installing a new definition for its
                 *     process. If the new process definition defines a new data
                 *     element, then this situation would be triggered; the best
                 *     course of action is to freeze it along with its colleagues.<P>
                 *
                 * (2) A single data item might belong to two distinct FreezeSets.
                 *     If both sets were frozen, it would be <b>doubly</b> frozen.
                 *     On the other hand, it might be frozen by one but not the
                 *     other, triggering this scenario.
                 */
                public void dataAdded(DataEvent e) {
                    String dataName = e.getName();
                    try {
                        if (isFreezeFlagElement(dataName))
                            return;           // don't freeze freeze flags!
                        if (!perl.match(freezeRegexp, e.getNameCA()))
                            return;           // only freeze data which matches the regexp.
                    } catch (MalformedCachePatternException m) {
                        //The user has given a bogus pattern!
                        System.out.println("The regular expression for " + freezeFlagName +
                                           " is malformed.");
                        dispose();
                        return;
                    }
                    SaveableData value = getValue(dataName);
                    boolean valueIsFrozen = (value instanceof FrozenData);

                    synchronized (this) {
                        if (currentState == FDS_GRANDFATHERED && valueIsFrozen) {
                            freezeAll(dataItems);
                            currentState = FDS_FROZEN;
                        } else if (currentState == FDS_FROZEN && !valueIsFrozen) {
                            freeze(dataName);
                            interrupt();
                        }

                        dataItems.add(dataName);
                    }
                }

                public void dataRemoved(DataEvent e) {
                    dataItems.remove(e.getName());
                }
            }
        }
        private static final String FREEZE_FLAG_TAG = "/FreezeFlag/";
        private static final int FDS_FROZEN = 0;
        private static final int FDS_GRANDFATHERED = 1;
        private static final int FDS_THAWED = 2;

        DataFreezer dataFreezer;

        public void disableFreezing() {
            if (dataFreezer != null) {
                dataFreezer.terminate();
                dataFreezer = null;
            }
        }



        URL [] templateURLs = null;


        public DataRepository() {
            includedFileCache.put("<dataFile.txt>", globalDataDefinitions);
            dataRealizer = new DataRealizer();
            dataNotifier = new DataNotifier();
            dataFreezer  = new DataFreezer();
            dataRealizer.start();
            dataNotifier.start();
            dataFreezer.start();
        }

        public void startServer(ServerSocket socket) {
            if (dataServer == null) {
                dataServer = new RepositoryServer(this, socket);
                dataServer.start();
            }
        }

        public void startSecondServer(ServerSocket socket) {
            if (secondaryDataServer == null) {
                secondaryDataServer = new RepositoryServer(this, socket);
                secondaryDataServer.start();
            }
        }

        public void saveAllDatafiles() {
            DataFile datafile;

            for (int i = datafiles.size();   i-- != 0; ) try {
                datafile = (DataFile)datafiles.elementAt(i);
                if (datafile.dirtyCount > 0)
                    saveDatafile(datafile);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        public void finalize() {
            // Command the data freezer to terminate.
            if (dataFreezer != null) dataFreezer.terminate();
            // Command data realizer to terminate
            dataRealizer.terminate();
            try {
                long start = System.currentTimeMillis();
                // wait up to 6 seconds total for both of the threads to die.
                dataFreezer.join(4000);
                long elapsed = System.currentTimeMillis() - start;
                long wait = 6000 - elapsed;
                if (wait < 0) wait = 1000;
                dataRealizer.join(wait);
            } catch (InterruptedException e) {}

            saveAllDatafiles();
            if (dataServer != null)
                dataServer.quit();
            if (secondaryDataServer != null)
                secondaryDataServer.quit();
        }


        public void setDatafileSearchURLs(URL[] templateURLs) {
            this.templateURLs = templateURLs;
        }


        public synchronized void renameData (String oldPrefix, String newPrefix) {

            DataFile datafile = null;
            String datafileName = null;

                                      // find the datafile associated with 'prefix'
            for (int index = datafiles.size();  index-- > 0; ) {
                datafile = (DataFile) datafiles.elementAt(index);
                if (datafile.prefix.equals(oldPrefix) && datafile.file != null) {
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

        /** this renames data values in the global datafile. */
        private void remapDataNames(String oldPrefix, String newPrefix) {

            String name, newName;
            DataElement element;
            SaveableData value;

            oldPrefix = oldPrefix + "/";
            newPrefix = newPrefix + "/";
            int oldPrefixLen = oldPrefix.length();
            Iterator k = getKeys();
            while (k.hasNext()) {
                name = (String) k.next();
                if (!name.startsWith(oldPrefix))
                    continue;

                element = (DataElement) data.get(name);
                if (element.datafile == null ||
                    element.datafile.prefix == null ||
                    element.datafile.prefix.length() > 0)
                    // only remap data which lives in the global datafile.
                    continue;

                value = element.getImmediateValue();

                // At this point, we will not rename data elements unless they
                // are SimpleData.  Non-simple data (e.g., functions, etc) needs
                // to know its name and prefix, so it would be more complicated to
                // move - but none of that stuff should be moving.
                if (value instanceof SimpleData) {
                    newName = newPrefix + name.substring(oldPrefixLen);
                    newName = newName.intern();
                    //System.out.println("renaming " + name + " to " + newName);
                    putValue(newName, value.getSimpleValue());
                    putValue(name, null);
                }
            }
        }


        private static final boolean disableSerialization = true;
        private boolean definitionsDirty = true;
        public void maybeSaveDefinitions(File out) throws IOException {
            if (definitionsDirty)
                saveDefinitions(new FileOutputStream(out));
        }
        public void saveDefinitions(OutputStream out) throws IOException {
            if (disableSerialization) return;
            ObjectOutputStream o = new ObjectOutputStream(out);
            o.writeObject(includedFileCache);
            o.writeObject(defineDeclarations);
            o.writeObject(defaultDefinitions);
            o.writeObject(globalDataDefinitions);
            o.writeObject(mountedPhantomData);
            o.close();
            definitionsDirty = false;
        }
        public void loadDefinitions(InputStream in) {
            if (disableSerialization) return;
            try {
                ObjectInputStream i = new ObjectInputStream(in);
                Hashtable a, b, c, d, e;
                a = (Hashtable) i.readObject();
                b = (Hashtable) i.readObject();
                c = (Hashtable) i.readObject();
                d = (Hashtable) i.readObject();
                e = (Hashtable) i.readObject();
                remountPhantomData(e);
                includedFileCache.putAll(a);
                defineDeclarations.putAll(b);
                defaultDefinitions.putAll(c);
                globalDataDefinitions.putAll(d);
                System.out.println("loaded serialized definitions.");
                in.close();
                definitionsDirty = false;
            } catch (Throwable t) {}
        }


        public synchronized void dumpRepository (PrintWriter out, Vector filt) {
            Iterator k = getKeys();
            String name, value;
            DataElement  de;
            SimpleData sd;

                                      // first, realize all elements.
            while (k.hasNext()) {
                name = (String) k.next();
                ((DataElement)data.get(name)).maybeRealize();
            }

                                      // next, print out all element values.
            k = getKeys();
            while (k.hasNext()) {
                name = (String) k.next();
                if (pspdash.Filter.matchesFilter(filt, name)) {
                    try {
                        de = (DataElement)data.get(name);
                        if (de.datafile != null) {
                            value = null;
                            sd = de.getSimpleValue();
                            if (sd instanceof DateData) {
                                value = ((DateData)sd).formatDate();
                            } else if (sd instanceof StringData) {
                                value = StringData.escapeString(((StringData)sd).getString());
                                // } else if (sd instanceof DoubleData) {
                                // value = ((DoubleData)sd).formatNumber(3);
                            } else if (sd != null)
                                value = sd.toString();
                            if (value != null) {
                                if (name.indexOf(',') != -1)
                                    name = EscapeString.escape(name, '\\', ",", "c");
                                out.println(name + "," + value);
                            }
                        }
                    } catch (Exception e) {
//          System.err.println("Data error:"+e.toString()+" for:"+name);
                    }
                }
                Thread.yield();
            }
        }


        public synchronized void dumpRepository () {
            Iterator k = getKeys();
            String name;
            DataElement element;

                                      // first, realize all elements.
            while (k.hasNext()) {
                name = (String) k.next();
                ((DataElement)data.get(name)).maybeRealize();
            }

                                      // next, print out all element values.
            k = getKeys();
            while (k.hasNext()) {
                name = (String) k.next();
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

            startInconsistency();

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

                    Iterator k = getKeys();
                    String name;
                    DataElement element;
                    DataListener dl;
                    Vector elementsToRemove = new Vector();
                    Hashtable affectedServerThreads = new Hashtable();

                                          // build a list of all the data elements of
                                          // this datafile.
                    while (k.hasNext()) {
                        name = (String) k.next();
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
                        element.disposeValue();
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
            } finally {
                finishInconsistency();
            }
        }

        private DataElement add(String name, SaveableData value, DataFile f,
                                boolean notify) {

                                    // Add the element to the table
            DataElement d = new DataElement(name);
            d.setValue(value);
            d.datafile = f;
            data.put(name, d);
            // System.out.println("DataRepository adding " + name + "=" +
            //                    (value == null ? "null" : value.saveString()));

            if (notify && !name.startsWith(anonymousPrefix))
                repositoryListenerList.dispatch(d.getDataAddedEvent());

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

                if (removedElement.getImmediateValue() == null)
                    oldValue = null;
                else if (removedElement.getImmediateValue() instanceof DeferredData)
                    oldValue = null;
                else {
                    oldValue = removedElement.getSimpleValue();
                    removedElement.getValue().dispose();
                }
                                        // notify any data listeners
                removedElement.setValue(null);
                dataNotifier.dataChanged(name, removedElement);

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
                    if (datafile.file == null) continue;
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
                d.setValue(new MalformedData(value));
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
            if (d != null)
                return d.getValue();
            else
                return maybeCreatePercentage(name);
        }

        /** only call this routine if the item doesn't already exist.
         * If the item looks like a percentage, automatically creates the
         * percentage on the fly and returns it.
         */
        private SaveableData maybeCreatePercentage(String name) {

            if (name == null) return null;
            if (name.indexOf(PercentageFunction.PERCENTAGE_FLAG) == -1)
                return null;

            try {
                SaveableData result = new PercentageFunction(name, this);
                add(name, result, null, false);
                return result;
            } catch (MalformedValueException mve) {
                return null;
            }
        }


        public final SimpleData getSimpleValue(String name) {
            SaveableData value = getValue(name);
            if (value == null)
                return null;
            else
                return value.getSimpleValue();
        }



        public SaveableData getInheritableValue(String prefix, String name) {
            return getInheritableValue(new StringBuffer(prefix), name);
        }

        public SaveableData getInheritableValue(StringBuffer prefix_, String name)
        {
            String prefix = prefix_.toString();
            String dataName = prefix + "/" + name;
            SaveableData result = getValue(dataName);
            int pos;
            while (result == null && prefix.length() > 0) {
                pos = prefix.lastIndexOf('/');
                if (pos == -1)
                    prefix = "";
                else
                    prefix = prefix.substring(0, pos);
                dataName = prefix + "/" + name;
                result = getValue(dataName);
            }
            if (result != null) prefix_.setLength(prefix.length());
            return result;
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
                         !value.saveString().equals(oldValue.saveString()))) {

                        // This data element has been changed and should be saved.

                        if (d.datafile == PHANTOM_DATAFILE)
                            // move the item OUT of the phantom datafile so it will be saved.
                            d.datafile = guessDataFile(name);

                        datafileModified(d.datafile);
                    }

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

        public void valueRecalculated(String name, SaveableData value) {

            if (recursion_depth < MAX_RECURSION_DEPTH) {
                DataElement d = (DataElement)data.get(name);
                if (d == null || d.getValue() != value) return;

                recursion_depth++;

                // let the data element know that it is changing.
                d.setValue(value);
                // notify any listeners registed for the change
                dataNotifier.dataChanged(name, d);

                recursion_depth--;

            } else {
                System.err.println
                    ("DataRepository detected circular dependency in data,\n" +
                     "    bailed out after " + MAX_RECURSION_DEPTH + " iterations.");
                new Exception().printStackTrace(System.err);
            }
        }

        public void userPutValue(String name, SaveableData value) {
            String aliasName = getAliasedName(name);
            putValue(aliasName, value);
        }

        public String getAliasedName(String name) {
            DataElement d = (DataElement) data.get(name);
            String aliasName = null;
            if (d != null && d.getValue() instanceof AliasedData)
                aliasName = ((AliasedData) d.getValue()).getAliasedDataName();

            if (aliasName != null)
                return getAliasedName(aliasName);
            else
                return name;
        }

        public void restoreDefaultValue(String name) {

            DataElement d = (DataElement) data.get(name);
            Object defaultValue = lookupDefaultValueObject(name, d);

            SaveableData value = null;
            if (defaultValue != null) {
                String prefix = (d.datafile == null ? "" : d.datafile.prefix);
                value = instantiateValue(name, prefix, defaultValue, false);
            }
            putValue(name, value);
        }


        public SimpleData evaluate(String expression)
            throws CompilationException, ExecutionException {
            return evaluate(expression, "");
        }

        public SimpleData evaluate(String expression, String prefix)
            throws CompilationException, ExecutionException {
            return evaluate(Compiler.compile(expression), prefix);
        }

        public SimpleData evaluate(CompiledScript script, String prefix)
            throws ExecutionException
        {
            ListStack stack = new ListStack();
            ExpressionContext context = new SimpleExpressionContext(prefix);
            script.run(stack, context);
            SimpleData value = (SimpleData) stack.pop();
            if (value != null)
                value = (SimpleData) value.getEditable(false);
            return value;
        }

        private class SimpleExpressionContext implements ExpressionContext {
            private String prefix;
            public SimpleExpressionContext(String p) { prefix = p; }
            public SimpleData get(String dataName) {
                return getSimpleValue(createDataName(prefix, dataName)); }
            public String resolveName(String dataName) {
                return createDataName(prefix, dataName); }
        }


        public void putExpression(String name, String prefix, String expression)
            throws MalformedValueException
        {
            try {
                CompiledFunction f = new CompiledFunction
                    (name, Compiler.compile(expression), this, prefix);
                putValue(name, f);
            } catch (CompilationException e) {
                throw new MalformedValueException();
            }
        }


        private static final String includeTag = "#include ";
        private final Hashtable includedFileCache = new Hashtable();

        private Map getIncludedFileDefinitions(String datafile) {
            //debug("getIncludedFileDefinitions("+datafile+")");
            datafile = followDatafileRedirections(datafile);
            Object definitions = includedFileCache.get(datafile);
            if (definitions instanceof DefinitionFactory) {
                definitions = ((DefinitionFactory) definitions).getDefinitions(this);
                definitions = Collections.unmodifiableMap((Map) definitions);
                includedFileCache.put(datafile, definitions);
                definitionsDirty = true;
            }
            return (Map) definitions;
        }

        /** Check in the defaultDefinitions map for any requested redirections.
         */
        private String followDatafileRedirections(String datafile) {
            Object def = datafile;
            while (def instanceof String) {
                datafile = (String) def;
                def = defaultDefinitions.get(datafile);
            }
            return datafile;
        }

        /** Get the definitions for the given includable datafile, loading
         *  them if necessary.
         */
        public Map loadIncludedFileDefinitions(String datafile)
            throws FileNotFoundException, IOException, InvalidDatafileFormat
        {
            //debug("loadIncludedFileDefinitions("+datafile+")");
            datafile = bracket(datafile);

            // Check in the defaultDefinitions map for any requested redirections.
            datafile = followDatafileRedirections(datafile);

            Map result = getIncludedFileDefinitions(datafile);
            if (result == null) {
                result = new HashMap();

                // Lookup any applicable default data definitions.
                DefinitionFactory defaultDefns =
                    (DefinitionFactory) defaultDefinitions.get(datafile);
                if (defaultDefns != null)
                    result.putAll(defaultDefns.getDefinitions(DataRepository.this));

                // the null in the next line is a bug! it has no effect on
                // #include <> statements, but effectively prevents #include ""
                // statements from working (in other words, include directives
                // relative to the current file.  Such directives are not
                // currently used by the dashboard, so nothing will break.)
                loadDatafile(datafile, findDatafile(datafile, null), result, true);

                // Although we aren't technically done creating this datafile,
                // we need to store it in the cache before calling
                // insertRollupDefinitions to avoid entering an infinite loop.
                includedFileCache.put(datafile, result);

                // check to see if the datafile requests a rollup
                Object rollupIDval = result.get("Use_Rollup");
                if (rollupIDval instanceof StringData) {
                    String rollupID = ((StringData) rollupIDval).getString();
                    insertRollupDefinitions(result, rollupID);
                }

                result = Collections.unmodifiableMap(result);
                includedFileCache.put(datafile, result);
                definitionsDirty = true;
            }

            return result;
        }

        private void insertRollupDefinitions(Map definitions, String rollupID) {
            // FIXME: handle lists
            try {
                String aliasDatafile = getAliasDatafileName(rollupID);

                // Get the set of alias definitions
                Map aliasDefinitions = loadIncludedFileDefinitions(aliasDatafile);

                if (aliasDefinitions != null) {
                    Map result = new HashMap();
                    result.putAll(aliasDefinitions);
                    result.putAll(definitions);
                    definitions.putAll(result);
                }
            } catch (Exception e) {}
        }


        Object lookupDefaultValueObject(String dataName, DataElement element) {
            // if the user didn't bother to look up the data element, look
            // it up for them.
            if (element == null) element = (DataElement)data.get(dataName);

            if (element == null ||                   // if there is no such element,
                element.datafile == null ||   // the element has no datafile, or its
                element.datafile.inheritsFrom == null)  // datafile doesn't inherit,
                return null;                        // then the default value is null.

            DataFile datafile = element.datafile;
            Map defaultValues = getIncludedFileDefinitions(datafile.inheritsFrom);
            if (defaultValues == null)
                return null;

            int prefixLength = datafile.prefix.length() + 1;
            String nameWithinDataFile = dataName.substring(prefixLength);
            Object defaultVal = defaultValues.get(nameWithinDataFile);
            return defaultVal;
        }
        String lookupDefaultValue(String dataName, DataElement element) {
            Object defaultVal = lookupDefaultValueObject(dataName, element);
            if (defaultVal == null) return null;
            if (defaultVal instanceof String) return (String) defaultVal;
            if (defaultVal instanceof SimpleData)
                return ((SimpleData) defaultVal).saveString();
            if (defaultVal instanceof CompiledScript)
                return ((CompiledScript) defaultVal).saveString();
            return null;
        }



        private InputStream findDatafile(String path, File currentFile) throws
            FileNotFoundException {
            InputStream result = null;
            File file = null;

                                      // find file in search path?
            if (path.startsWith("<")) {
                                              // strip <> chars
                path = path.substring(1, path.length()-1);

                URL u;
                URLConnection conn;
                                        // look in each template URL until we
                                        // find the named file
                for (int i = 0;  i < templateURLs.length;  i++) try {
                    u = new URL(templateURLs[i], path);
                    conn = u.openConnection();
                    conn.connect();
                    result = conn.getInputStream();
                    return result;
                } catch (IOException ioe) { }

                                        // couldn't find the file in any template
                                        // URL - give up.
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


        private class LoadingException extends RuntimeException {
            Exception root;
            public LoadingException(Exception e) { root = e; }
            public Exception getRoot() { return root; }
        }
        private class FileLoader extends DepthFirstAdapter {
            private String inheritedDatafile = null;
            private Map dest;
            public FileLoader(Map dest) { this.dest = dest; }
            public String getInheritedDatafile() { return inheritedDatafile; }

            private void putVal(String name, Object value) {
                if (name.startsWith("/"))
                    putGlobalValue(name, value);
                else if (value == null ||
                         value.equals("null") || value.equals("=null"))
                    dest.remove(name);
                else
                    dest.put(name, value);
            }

            /** Process a new style declaration. */
            public void caseANewStyleDeclaration(ANewStyleDeclaration node) {
                String name = Compiler.trimDelim(node.getIdentifier());
                CompiledScript script = null;
                try {
                    script = Compiler.compile(node.getValue());
                } catch (CompilationException ce) {
                    throw new LoadingException
                        (new InvalidDatafileFormat(ce.getMessage()));
                }
                if (!script.isConstant())
                    putVal(name, script);
                else {
                    SimpleData constant = script.getConstant();
                    if (constant != null &&
                        node.getAssignop() instanceof AReadOnlyAssignop)
                        constant = (SimpleData) constant.getEditable(false);
                    putVal(name, constant);
                }
            }

            /** Process an old style declaration. */
            public void caseAOldStyleDeclaration(AOldStyleDeclaration node) {
                String line = node.getOldStyleDeclaration().getText(), name, value;
                int equalsPosition = line.indexOf('=');
                if (equalsPosition == -1)
                    throw new LoadingException
                        (new InvalidDatafileFormat
                            ("There is no '=' character on the line: '" + line + "'."));

                name = line.substring(0, equalsPosition);
                value = line.substring(equalsPosition+1);
                putVal(name, value);
            }

            public void caseASearchDeclaration(ASearchDeclaration node) {
                putVal(Compiler.trimDelim(node.getIdentifier()),
                       new SearchFactory(node));
            }

            public void caseASimpleSearchDeclaration(ASimpleSearchDeclaration node) {
                putVal(Compiler.trimDelim(node.getIdentifier()),
                       new SearchFactory(node));
            }

            /** Process an include directive. */
            public void caseAIncludeDeclaration(AIncludeDeclaration node) {
                String line = node.getIncludeDirective().getText();
                inheritedDatafile = line.substring(includeTag.length()).trim();

                // Add proper exception handling in case someone is somehow using
                // the deprecated include syntax.
                if (inheritedDatafile.startsWith("\"")) {
                    throw new LoadingException
                        (new InvalidDatafileFormat
                            ("datafile #include directives with relative" +
                             " paths are no longer supported."));
                }

                try {
                    Map cachedIncludeFile =
                        loadIncludedFileDefinitions(inheritedDatafile);
                    Map filteredIncludeFile = cachedIncludeFile;

                    if (node.getExcludeClause() != null) {
                        IdentifierLister filter = new IdentifierLister();
                        node.getExcludeClause().apply(filter);
                        filteredIncludeFile = filterDefinitions
                            (cachedIncludeFile, filter.identifiers, filter.strings);
                    }

                    dest.putAll(filteredIncludeFile);
                } catch (Exception e) {
                    throw new LoadingException(e);
                }
            }

            public void caseAUndefineDeclaration(AUndefineDeclaration node) {
                IdentifierLister list = new IdentifierLister();
                node.getIdentifierList().apply(list);
                Iterator i = list.identifiers.iterator();
                while (i.hasNext())
                    dest.remove(i.next());
            }
        }

        private class IdentifierLister extends DepthFirstAdapter {
            public ArrayList identifiers = new ArrayList();
            public ArrayList strings = new ArrayList();
            public IdentifierLister() {}
            public void caseTIdentifier(TIdentifier node) {
                identifiers.add(Compiler.trimDelim(node)); }
            public void caseTStringLiteral(TStringLiteral node) {
                strings.add(Compiler.trimDelim(node)); }
        }

        // loadDatafile - opens the file passed to it and looks for "x = y" type
        // statements.  If one is found it associates x with y in the Hashtable
        // dest.  If an include statement is found on the first line, a recursive
        // call to loadDatafile is made, using the same Hashtable.  Return the
        // name of the include file, if one was found.

        private String loadDatafile(String file, InputStream datafile,
                                    Map dest, boolean close)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {
            return loadDatafile(file, new InputStreamReader(datafile), dest, close);
        }
        private String loadDatafile(String filename, Reader datafile,
                                    Map dest, boolean close)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {

            //debug("loadDatafile("+filename+")");
            // Initialize data, file, and read buffer.
            String inheritedDatafile = null;
            BufferedReader in = new BufferedReader(datafile);
            String line, name, value;
            int equalsPosition;
            FileLoader loader = new FileLoader(dest);
            String defineDecls = null;
            if (filename != null)
                defineDecls = (String) defineDeclarations.get(filename);

            try {
                CppFilterReader readIn = new CppFilterReader(in, defineDecls);
                Parser p = new Parser(new Lexer(new PushbackReader(readIn, 1024)));

                // Parse the file.
                Start tree = p.parse();

                // Apply the file loader.
                tree.apply(loader);

            } catch (ParserException pe) {
                String message = "Could not parse " +filename+ "; " + pe.getMessage();
                ErrorReporter.templates.logError(message);
                throw new InvalidDatafileFormat(message);
            } catch (LexerException le) {
                String message = "Could not parse " +filename+ "; " + le.getMessage();
                ErrorReporter.templates.logError(message);
                throw new InvalidDatafileFormat(message);
            } catch (LoadingException load) {
                Exception root = load.getRoot();
                if (root instanceof FileNotFoundException)
                    throw (FileNotFoundException) root;
                if (root instanceof IOException)
                    throw (IOException) root;
                if (root instanceof InvalidDatafileFormat)
                    throw (InvalidDatafileFormat) root;
                System.err.println("Unusual exception when loading file: " + root);
                root.printStackTrace();
                throw new IOException(root.getMessage());
            } finally {
                if (close) in.close();
            }

            return loader.getInheritedDatafile();
        }

        public void parseDatafile(String contents, Map dest)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {
            loadDatafile(null, new StringReader(contents), dest, true);
        }

        private final Hashtable defineDeclarations = new Hashtable();
        public void putDefineDeclarations(String datafile, String decls) {
            defineDeclarations.put(bracket(datafile), decls);
            definitionsDirty = true;
        }

        private final Hashtable defaultDefinitions = new Hashtable();

        public void registerDefaultData(DefinitionFactory d,
                                        String datafile,
                                        String imaginaryFilename) {
            if (datafile != null && datafile.length() > 0) {
                defaultDefinitions.put(bracket(datafile), d);
                defaultDefinitions.put(bracket(imaginaryFilename), bracket(datafile));
            } else
                includedFileCache.put(bracket(imaginaryFilename), d);
            definitionsDirty = true;
        }
        private String bracket(String filename) {
            if (filename == null || filename.startsWith("<")) return filename;
            return "<" + filename + ">";
        }

        public String getRollupDatafileName(String rollupID) {
            return "ROLLUP:" + rollupID;
        }
        public String isRollupDatafileName(String dataFile) {
            if (dataFile != null && dataFile.startsWith("ROLLUP:"))
                return dataFile.substring("ROLLUP:".length());
            else
                return null;
        }

        public String getAliasDatafileName(String rollupID) {
            return "ROLLUP-ALIAS:" + rollupID;
        }

        private Hashtable globalDataDefinitions = new Hashtable();

        public void addGlobalDefinitions(InputStream datafile, boolean close)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {
            loadDatafile(null, datafile, globalDataDefinitions, close);
        }

        private SaveableData instantiateValue(String name, String dataPrefix,
                                              Object valueObj, boolean readOnly) {

            SaveableData o = null;

            if (valueObj instanceof SimpleData) {
                o = (SimpleData) valueObj;
                if (readOnly) o = o.getEditable(false);

            } else if (valueObj instanceof CompiledScript) {
                o = new CompiledFunction(name, (CompiledScript) valueObj,
                                         this, dataPrefix);

            } else if (valueObj instanceof SearchFactory) {
                o = ((SearchFactory) valueObj).buildFor(name, this, dataPrefix);

            } else if (valueObj instanceof String) {
                String value = (String) valueObj;
                if (value.startsWith("=")) {
                    readOnly = true;
                    value = value.substring(1);
                }

                try {
                    o = ValueFactory.createQuickly(name, value, this, dataPrefix);
                } catch (MalformedValueException mfe) {
                    o = new MalformedData(value);
                }
                if (readOnly && o != null) o.setEditable(false);
            }

            return o;
        }

        private void putGlobalValue(String name, Object valueObj) {
            DataElement e = (DataElement) data.get(name);
            if (e != null && e.getImmediateValue() != null)
                return;                 // don't overwrite existing values?

            SaveableData o = instantiateValue(name, "", valueObj, false);

            if (o != null) {
                globalDataDefinitions.put(name.substring(1), valueObj);
                definitionsDirty = true;
                putValue(name, o);
            }
        }

        /** Perform renaming operations found in the values map.
         *
         * A simple renaming operation is a mapping whose value begins
         * with "<=".  The key is the new name for the data, and the rest
         * of the value is the original name.  So the following lines in a
         * datafile: <pre>
         *    foo="bar
         *    baz=<=foo
         * </pre> would be equivalent to the single line `baz="bar'.
         * Simple renaming operations are correctly transitive, so <pre>
         *   foo=1
         *   bar=<=foo
         *   baz=<=bar
         * </pre> is equivalent to `baz=1'. This will work correctly, no matter
         * what order the lines appear in.
         *
         * Pattern match renaming operations are mappings whose value
         * begins with >~.  The key is a pattern to match, and the value
         * is the substitution expression.  So <pre>
         *    foo 1="one
         *    foo 2="two
         *    foo ([0-9])+=>~$1/foo
         * </pre> would be equivalent to the lines <pre>
         *    1/foo="one
         *    2/foo="two
         * </pre> The pattern must match the original name of the element - not
         * any renamed variant.  Therefore, pattern match renaming operations
         * <b>cannot</b> be chained.  A pattern match operation <b>can</b> be
         * the <b>first</b> renaming operation in a transitive chain, but will
         * neverbe used as the second or subsequent operations in a chain.
         *
         * Finally, renaming operations can influence dataFiles below them in
         * the datafile inheritance chain.  This is, in fact, the #1 reason for
         * the renaming mechanism.  It allows a process datafile to rename
         * elements that appear in end-user project datafiles.
         *
         * @return true if any renames took place.
         */
        private boolean performRenames(Hashtable values)
         throws InvalidDatafileFormat {
            boolean dataWasRenamed = false;
            Hashtable renamingOperations = new Hashtable(),
                patternRenamingOperations = new Hashtable();

            // Perform a pass through the value map looking for renaming operations.
            Iterator i = values.entrySet().iterator();
            String name, value;
            Map.Entry e;
            while (i.hasNext()) {
                e = (Map.Entry) i.next();
                name = (String) e.getKey();
                if (!(e.getValue() instanceof String)) continue;
                value = (String) e.getValue();

                if (value.startsWith(SIMPLE_RENAME_PREFIX)) {
                    renamingOperations.put
                        (name, value.substring(SIMPLE_RENAME_PREFIX.length()));
                    i.remove();
                } else if (value.startsWith(PATTERN_RENAME_PREFIX)) {
                    patternRenamingOperations.put
                        (name, value.substring(PATTERN_RENAME_PREFIX.length()));
                    i.remove();
                }
            }

            // For each pattern-style renaming operation, find data names that
            // match the pattern and add the corresponding renaming operation to
            // the regular naming operation list.
            i = patternRenamingOperations.entrySet().iterator();
            String re;
            while (i.hasNext()) {
                e = (Map.Entry) i.next();
                name = (String) e.getKey();
                value = (String) e.getValue();

                re = "s\n^" + name + "$\n" + value + "\n";
                // scan the value map for matching names.
                Enumeration valueNames = values.keys();
                String valueName, valueRename;
                while (valueNames.hasMoreElements()) {
                    valueName = (String) valueNames.nextElement();
                    try {
                        valueRename = perl.substitute(re, valueName);
                        if (!valueName.equals(valueRename))
                            renamingOperations.put(valueRename, valueName);
                    } catch (MalformedCachePatternException mpe) {
                        throw new InvalidDatafileFormat
                            ("Malformed renaming operation '" + name +
                             "=" + PATTERN_RENAME_PREFIX + value + "'");
                    }
                }
            }

            // Now perform the renaming operations.
            String oldName, newName;
            Object val;
            i = renamingOperations.entrySet().iterator();
            while (!renamingOperations.isEmpty()) {
                newName = (String) renamingOperations.keySet().iterator().next();
                oldName = (String) renamingOperations.remove(newName);
                val     = values.remove(oldName);
                while (val == null &&
                       (oldName = (String) renamingOperations.remove(oldName)) != null)
                    val = values.remove(oldName);

                if (val != null) {
                    values.put(newName, val);
                    dataWasRenamed = true;
                }
            }
            return dataWasRenamed;
        }
        public static final String SIMPLE_RENAME_PREFIX = "<=";
        public static final String PATTERN_RENAME_PREFIX = ">~";


        private Map filterDefinitions(Map definitions,
                                       List identifiers,
                                       List regularExpressions) {
            Map result = new HashMap(definitions);

            // delete all the specified identifiers from the map.
            Iterator i = identifiers.iterator();
            String identifier;
            while (i.hasNext()) {
                identifier = (String) i.next();
                result.remove(identifier);
            }

            // remove data elements which match any of the regular expressions.
            Iterator r = regularExpressions.iterator();
            String regExp;
            while (r.hasNext()) {
                regExp = "m\n^" + r.next() + "$\n";
                i = result.keySet().iterator();
                while (i.hasNext()) {
                    identifier = (String) i.next();
                    if (perl.match(regExp, identifier))
                        i.remove();
                }
            }

            return result;
        }

        public void openDatafile(String dataPrefix, String datafilePath)
            throws FileNotFoundException, IOException, InvalidDatafileFormat {

            // debug("openDatafile");

            Hashtable values = new Hashtable();

            DataFile dataFile = new DataFile();
            dataFile.prefix = dataPrefix;
            dataFile.file = new File(datafilePath);
            dataFile.inheritsFrom =
                loadDatafile(null, new FileInputStream(dataFile.file),
                             values, true);

            // perform any renaming operations that were requested in the datafile
            boolean dataModified = performRenames(values);

                                    // only add the datafile element if the
                                    // loadDatafile process was successful
            datafiles.addElement(dataFile);

                                    // mount the data in the repository.
            mountData(dataFile, dataPrefix, values);

            if (dataModified)       // possibly mark the file as modified.
                datafileModified(dataFile);
        }

        void mountData(DataFile dataFile, String dataPrefix, Map values)
            throws InvalidDatafileFormat
        {
            try {
                startInconsistency();

                // register the names of data elements in this file IF it is a
                // regular datafile and is not global data.
                boolean registerDataNames =
                    (dataFile!=null && dataFile.file!=null && dataPrefix.length()>0);

                boolean dataModified = false, successful = false;
                String datafilePath = "internal data";
                boolean fileEditable = true;

                if (dataFile != null && dataFile.file != null) {
                    datafilePath = dataFile.file.getPath();
                    fileEditable = dataFile.file.canWrite();
                }
                if (dataPrefix.equals(realizeDeferredDataFor))
                    realizeDeferredDataFor = dataFile;

                int retryCount = 10;
                while (!successful && retryCount-- > 0) try {
                    boolean dataEditable;

                    Map.Entry defn;
                    String localName, name, value;
                    Object valueObj;
                    SaveableData o;
                    DataElement d;

                    Iterator dataDefinitions = values.entrySet().iterator();
                    while (dataDefinitions.hasNext()) {
                        defn = (Map.Entry) dataDefinitions.next();
                        localName = (String) defn.getKey();
                        valueObj = defn.getValue();
                        name = createDataName(dataPrefix, localName);
                        o = instantiateValue(name, dataPrefix, valueObj, !fileEditable);

                        // is anyone still using this functionality???
                        if (valueObj instanceof String &&
                            "@now".equalsIgnoreCase((String) valueObj))
                            dataModified = true;

                        if (o instanceof MalformedData)
                            System.err.println("Data value for '"+name+"' in file '"+
                                               datafilePath+"' is malformed.");

                        d = (DataElement)data.get(name);
                        if (d == null) {
                            if (o != null) d = add(name, o, dataFile, true);
                        } else {
                                              // this prevents the putValue logic from
                            d.datafile = null;  // marking the datafile as modified
                            putValue(name, o);
                            d.datafile = dataFile;
                        }
                        // this is necessary because the mechanisms above which set the
                        // value of a DataElement do so AFTER setting the datafile.
                        if (dataFile==realizeDeferredDataFor && o instanceof DeferredData)
                            dataRealizer.addElement(d);

                        if (registerDataNames &&
                            (o instanceof DoubleData || o instanceof DeferredData ||
                             o instanceof CompiledFunction))
                            dataElementNameSet.add(localName);
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
                    successful = true;

                } catch (Throwable e) {
                    if (retryCount > 0) {
                        // Try again to open this datafile. Most errors are transient,
                        // caused by incredibly infrequent thread-related problems.
                        debug("when opening "+datafilePath+" caught error "+e+
                              ", retrying.");
                        e.printStackTrace();
                    } else {
                        // We've done our best, but after 10 tries, we still can't open
                        // this datafile.  Give up and throw an exception.
                        dataFile.file = null;
                        closeDatafile(dataPrefix);
                        throw new InvalidDatafileFormat("Caught unexpected exception "+e);
                    }
                }

            } finally {
                finishInconsistency();
            }
        }

        private Hashtable mountedPhantomData = new Hashtable();

        private void remountPhantomData(Hashtable h) throws InvalidDatafileFormat {
            Iterator i = h.entrySet().iterator();
            Map.Entry e;
            while (i.hasNext()) {
                e = (Map.Entry) i.next();
                mountPhantomData((String) e.getKey(), (Map) e.getValue());
            }
        }

        public void mountPhantomData(String dataPrefix, Map values)
            throws InvalidDatafileFormat
        {
            // It is important to mount the data with *some* datafile - if a
            // data element's datafile is null, it is considered transient and
            // can be deleted at any time if no one is listening to its value.
            mountData(getPhantomDataFile(), dataPrefix, values);

            if (mountedPhantomData != null) {
                mountedPhantomData.put(dataPrefix, values);
                definitionsDirty = true;
            }
        }

        void mountImportedData(String dataPrefix, Map values)
            throws InvalidDatafileFormat
        {
            String prefixToDiscard = null;
            for (int i = datafiles.size();   i-- != 0; )
                if (dataPrefix.equals(((DataFile)datafiles.elementAt(i)).prefix)) {
                    DataFile previousDataFile = (DataFile)datafiles.elementAt(i);
                    prefixToDiscard = previousDataFile.prefix;
                    prefixToDiscard = prefixToDiscard.replace('/', '\\');
                    prefixToDiscard = '\u0001' + prefixToDiscard.substring(1);
                    previousDataFile.prefix = prefixToDiscard;
                    break;
                }

            DataFile dataFile = new DataFile();
            dataFile.prefix = dataPrefix;
            datafiles.addElement(dataFile);
            mountData(dataFile, dataPrefix, values);

            if (prefixToDiscard != null)
                closeDatafile(prefixToDiscard);
        }

        private DataFile getPhantomDataFile() {
            if (PHANTOM_DATAFILE == null) {
                DataFile d = new DataFile();
                d.prefix = "";
                PHANTOM_DATAFILE = d;
            }
            return PHANTOM_DATAFILE;
        }
        private DataFile PHANTOM_DATAFILE = null;

        private final Object OPENDATAFILE_ERROR_DEPTH_LOCK = new Object();
        private volatile int OPENDATAFILE_ERROR_DEPTH = 0;

        private static volatile int MAX_DIRTY = 10;


        private void datafileModified(DataFile datafile) {
            if (datafile != null && ++datafile.dirtyCount > MAX_DIRTY)
                saveDatafile(datafile);
        }

        public Iterator getKeys() {
            ArrayList l = new ArrayList();
            synchronized (data) {
                l.addAll(data.keySet());
            }
            return l.iterator();
        }

        // saveDataFile - saves a set of data to the appropriate data file.  In
        // order to minimize data loss, data is first written to two temporary
        // files, out and backup.  Once this is successful, out is renamed to
        // the actual datafile.  Once the rename is successful, backup is
        // deleted.
        private void saveDatafile(DataFile datafile) {
            if (datafile == null || datafile.file == null) return;

            // this flag should stay false until we are absolutely certain
            // that we have successfully saved the datafile.
            boolean saveSuccessful = false;

            // synchronize to prevent two different threads from trying to save
            // the same datafile concurrently.
            synchronized(datafile) { try {
                // debug("saveDatafile");

                Set valuesToSave = new TreeSet();
                Map defaultValues = null;

                // if the data file has an include statement, lookup the associated
                // default values defined by the included file.
                if (datafile.inheritsFrom != null) {
                    defaultValues = getIncludedFileDefinitions(datafile.inheritsFrom);
                    if (defaultValues == null)
                        System.err.println("No inherited definitions for " +
                                           datafile.inheritsFrom);
                }
                if (defaultValues == null)
                    defaultValues = new HashMap();

                // make a set of all the data element names defined in defaultValues
                Set defaultValueNames = new HashSet(defaultValues.keySet());

                // optimistically mark the datafile as "clean" at the beginning of
                // the save operation.  This way, if the datafile is modified
                // during the save operation, the dirty changes will take effect,
                // and the datafile will be saved again in the future.
                datafile.dirtyCount = 0;

                Iterator k = getKeys();
                String name = null, valStr, defaultValStr;
                Object defaultVal;
                DataElement element;
                SaveableData value;
                int prefixLength = datafile.prefix.length() + 1;

                // write the data elements to the two temporary output files.
                while (k.hasNext()) {
                    name = (String)k.next();
                    element = (DataElement)data.get(name);

                    // Make a quick check on the element and datafile validity
                    // before taking the time to get the value
                    if ((element != null) && (element.datafile == datafile)) {
                        // don't realize the data if it is still deferred.
                        value = element.getImmediateValue();
                        if (value != null) {
                            name = name.substring(prefixLength);
                            defaultValueNames.remove(name);

                            valStr = value.saveString();
                            if (valStr == null || valStr.length() == 0) continue;
                            if (!value.isEditable()) valStr = "=" + valStr;
                            defaultVal = defaultValues.get(name);
                            defaultValStr = null;
                            if (defaultVal instanceof String)
                                defaultValStr = (String) defaultVal;
                            else if (defaultVal instanceof SimpleData) {
                                defaultValStr = ((SimpleData) defaultVal).saveString();
                                if (!((SimpleData) defaultVal).isEditable())
                                    defaultValStr = "=" + defaultValStr;
                            } else if (defaultVal instanceof SearchFactory)
                                continue;
                            else if
                                (defaultVal instanceof CompiledScript &&
                                 value instanceof CompiledFunction &&
                                 ((CompiledFunction) value).getScript() == defaultVal)
                                continue;

                            if (valStr.equals(defaultValStr))
                                continue;

                            valuesToSave.add(name + "=" + valStr);
                        }
                    }
                }
                k = defaultValueNames.iterator();
                while (k.hasNext()) {
                    name = (String) k.next();
                    defaultVal = defaultValues.get(name);

                    if (defaultVal == null) continue;
                    if (defaultVal instanceof String) {
                        defaultValStr = (String) defaultVal;
                        if (defaultValStr.equals("null") ||
                            defaultValStr.equals("=null") ||
                            defaultValStr.startsWith(SIMPLE_RENAME_PREFIX) ||
                            defaultValStr.startsWith(PATTERN_RENAME_PREFIX))
                            continue;
                    }

                    valuesToSave.add(name+"=null");
                }


                // Create temporary files
                BufferedWriter out;

                try {
                    out = new BufferedWriter(new RobustFileWriter(datafile.file));
                } catch (IOException e) {
                    System.err.println("IOException " + e + " while opening " +
                                       datafile.file.getPath() + "; save aborted");
                    return;
                }

                try {
                    // if the data file has an include statement, write it to the
                    // the two temporary output files.
                    if (datafile.inheritsFrom != null) {
                        out.write(includeTag + datafile.inheritsFrom);
                        out.newLine();
                    }

                    // If the data file has a prefix, write it as a comment to the
                    // two temporary output files.
                    if (datafile.prefix != null && datafile.prefix.length() > 0) {
                        out.write("= Data for " + datafile.prefix);
                        out.newLine();
                    }

                    k = valuesToSave.iterator();
                    while (k.hasNext()) {
                        out.write((String) k.next());
                        out.newLine();
                    }

                } catch (IOException e) {
                    System.err.println("IOException " + e + " while writing to " +
                                       datafile.file.getPath() + "; save aborted");
                    return;
                }

                try {
                    // Close output file
                    out.flush();
                    out.close();

                    saveSuccessful = true;
                    System.err.println("Saved " + datafile.file.getPath());
                } catch (IOException e) {
                    System.err.println("IOException " + e + " while closing " +
                                       datafile.file.getPath());
                }

                // debug("saveDatafile done");
            } finally {
                if (!saveSuccessful)               // if we couldn't successfully save
                    datafile.dirtyCount = MAX_DIRTY; // the datafile, mark it as dirty.
            } }
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
            addDataListener(name, dl, true);
        }

        public void addDataListener(String name, DataListener dl, boolean notify) {
            DataElement d;
            synchronized (data) {
                // lookup the element.
                d = (DataElement)data.get(name);

                // if we didn't find the element, try autocreating a percentage.
                if (d == null && maybeCreatePercentage(name) != null)
                    d = (DataElement)data.get(name);

                // the item doesn't exist. Create an entry for it with the value null.
                if (d == null)
                    d = add(name, null, guessDataFile(name), true);
            }

            if (d.dataListenerList == null) d.dataListenerList = new Vector();
            if (!d.dataListenerList.contains(dl))
                d.dataListenerList.addElement(dl);

            if (notify)
                dataNotifier.addEvent(name, d, dl);
        }

        public void addActiveDataListener
            (String name, DataListener dl, String dataListenerName) {
            addActiveDataListener(name, dl, dataListenerName, true);
        }

        public void addActiveDataListener
            (String name, DataListener dl, String dataListenerName, boolean notify) {
            addDataListener(name, dl, notify);
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

            Iterator dataElements = getKeys();
            String name = null;
            DataElement element = null;
            Vector listenerList = null;

                      // walk the hashtable, removing this datalistener.
            while (dataElements.hasNext()) {
                name = (String) dataElements.next();
                element = (DataElement) data.get(name);
                if (element != null) {
                    listenerList = element.dataListenerList;
                    if (listenerList != null && listenerList.removeElement(dl))
                        maybeDelete(name, element);
                }
            }
            dataNotifier.deleteDataListener(dl);
            activeData.remove(dl);
            // debug("deleteDataListener done");
        }



        public void addRepositoryListener(RepositoryListener rl, String prefix) {
            //debug("addRepositoryListener:" + prefix);

                                    // add the listener to our repository list.
            repositoryListenerList.addListener(rl, prefix);

                                    // notify the listener of all the elements
                                    // already in the repository.
            Iterator k = getKeys();
            String name;


            if (prefix != null && prefix.length() != 0)

                                    // if they have specified a prefix, notify them
                                    // of all the data beginning with that prefix.
                while (k.hasNext()) {
                    if ((name = (String) k.next()).startsWith(prefix)) {
                        DataElement d = (DataElement) data.get(name);
                        if (d != null) rl.dataAdded(d.getDataAddedEvent());
                    }
                }

            else                    // if they have specified no prefix, only
                                    // notify them of data that is NOT anonymous.
                while (k.hasNext())
                    if (!(name = (String) k.next()).startsWith(anonymousPrefix)) {
                        DataElement d = (DataElement) data.get(name);
                        if (d != null) rl.dataAdded(d.getDataAddedEvent());
                    }

            // debug("addRepositoryListener done");
        }



        public void removeRepositoryListener(RepositoryListener rl) {
            // debug("removeRepositoryListener");
            repositoryListenerList.removeListener(rl);
            // debug("removeRepositoryListener done");
        }

        private volatile int inconsistencyDepth = 0;
        private Set consistencyListeners =
            Collections.synchronizedSet(new HashSet());

        public void addDataConsistencyObserver(DataConsistencyObserver o) {
            boolean callbackImmediately = false;
            synchronized (consistencyListeners) {
                if (inconsistencyDepth == 0)
                    callbackImmediately = true;
                else
                    consistencyListeners.add(o);
            }
            if (callbackImmediately) o.dataIsConsistent();
        }

        public void startInconsistency() {
            synchronized (consistencyListeners) { inconsistencyDepth++; }
        }

        public void finishInconsistency() {
            synchronized (consistencyListeners) {
                if (--inconsistencyDepth == 0 &&
                    !consistencyListeners.isEmpty()) {
                    ConsistencyNotifier notifier =
                        new ConsistencyNotifier(consistencyListeners);
                    consistencyListeners.clear();
                    notifier.start();
                }
            }
        }

        private class ConsistencyNotifier extends Thread {
            private Set listenersToNotify;

            public ConsistencyNotifier(Set listeners) {
                listenersToNotify = new HashSet(listeners);
            }

            public void run() {
                // give things a chance to settle down.
                //System.out.println("waiting for notifier at " +new java.util.Date());
                dataNotifier.flush();
                //System.out.println("notifier done at " + new java.util.Date());

                Iterator i = listenersToNotify.iterator();
                DataConsistencyObserver o;
                while (i.hasNext()) {
                    o = (DataConsistencyObserver) i.next();
                    o.dataIsConsistent();
                }
            }
        }

        public Vector listDataNames(String prefix) {
            Vector result = new Vector();
            Iterator names = getKeys();
            String name;
            while (names.hasNext()) {
                name = (String) names.next();
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
                    if (datafile.file == null)
                        datafileName = "";
                    else
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

            if (dataServer != null)
                dataServer.deletePrefix(oldPrefix);
        }

        public Set getDataElementNameSet() { return dataElementNameSet_ext; }

        public static final String PARENT_PREFIX = "../";

        public static String chopPath(String path) {
            if (path == null) return null;
            int slashPos = path.lastIndexOf('/');
            if (slashPos == path.length() - 1)
                slashPos = path.lastIndexOf('/', slashPos);
            if (slashPos == -1)
                return null;
            else
                return path.substring(0, slashPos);
        }

        public static String createDataName(String prefix, String name) {
            if (name == null) return null;
            if (name.startsWith("/")) return name.intern();
            while (name.startsWith(PARENT_PREFIX)) {
                prefix = chopPath(prefix);
                if (prefix == null) {
                    prefix = "";
                    break;
                }
                name = name.substring(PARENT_PREFIX.length());
            }
            StringBuffer buf = new StringBuffer(prefix.length() + name.length() + 1);
            buf.append(prefix);
            if (!prefix.endsWith("/")) buf.append("/");
            buf.append(name);
            return buf.toString().intern();
        }

        private Comparator nodeComparator = null;
        public void setNodeComparator(Comparator c) { nodeComparator = c; }

        public int compareNames(String name1, String name2) {
            int result = 0;
            if (nodeComparator != null)
                result = nodeComparator.compare(name1, name2);

            if (result == 0)
                result = name1.compareTo(name2);

            return result;
        }
}
