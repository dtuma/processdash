// Copyright (C) 2000-2006 Tuma Solutions, LLC
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


import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Properties;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.repository.*;
import net.sourceforge.processdash.util.FormatUtil;

public class RepositoryClient extends Thread implements Repository {

                                // key=name;  value=vector of datalisteners
    Hashtable dataListenerList = new Hashtable();
    Hashtable dataEvents = new Hashtable();
    Socket clientSocket = null;
    PrintWriter out = null;
    ObjectInputStream in = null;
    private String dataPath = null;
    private volatile boolean isRunning = false;
    private Vector repositoryClientListeners = new Vector();

    public RepositoryClient(URL url, String requiredTag)
        throws RemoteException, ForbiddenException {
        boolean tagExists = true;

        try {
            setName(getName() + "(RepositoryClient)");
            String ID, urlFile = url.getFile();
            int pos = urlFile.indexOf("//");
            if (pos == -1) pos = urlFile.indexOf("/+/");
            if (pos == -1)
                ID = (new StringTokenizer(urlFile, "/")).nextToken();
            else
                ID = urlFile.substring(0, pos);

            // debug("creating socket...");
            clientSocket = new Socket(url.getHost(), url.getPort() - 1);

            // debug("got socket, getting output stream...");
            out = new PrintWriter(clientSocket.getOutputStream(), false);
            // debug("writing to output stream...");
            out.println(ID);
            out.println(requiredTag == null ? "" : requiredTag);
            out.flush();

            // debug("getting input stream...");
            in = new ObjectInputStream(clientSocket.getInputStream());
            // debug("done. got both input streams. reading settings from input.");

            dataPath = (String) in.readObject();
            tagExists = in.readBoolean();
            try {
                Settings.initialize((Properties) in.readObject());
                FormatUtil.setDateFormats(Settings.getVal("dateFormat"),
                        Settings.getVal("dateTimeFormat"));
            } catch (Exception e) {}

            if (!tagExists)
                throw new ForbiddenException();

            isRunning = true;
            this.start();

            //if (Settings.getBool("dataApplet.debug", false))
            //  RCL.addRepositoryClient(this);

        } catch (ForbiddenException e) { cleanup(); throw e;
        } catch (ConnectException e)   { cleanup(); throw new RemoteException();
        } catch (Exception e) {
            cleanup();
            printError(e);
            throw new RemoteException();
        }
    }

    private synchronized void cleanup() {
        //RCL.removeRepositoryClient(this);

        if (out != null) try { out.close(); out = null; } catch (Exception ex) {}
        if (in  != null) try { in.close();  in  = null; } catch (Exception ex) {}
        if (clientSocket != null) try {
            clientSocket.close(); clientSocket = null; } catch (IOException ex) {}

        dataListenerList = dataEvents = null;
        dataPath = null;
    }

//  private void debug(String msg) {
//    System.out.println("RepositoryClient: " + msg);
//  }

    private void printError(Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace(System.err);
    }

    public void run() {

        Object o = null;
        DataEvent e = null;
        Enumeration enumeration = null;

        while (isRunning) try {
            o = null;
            try {
                o = in.readObject();
                e = (DataEvent) o;
            } catch (Exception ex) { isRunning = false; break; }

            // debug("got dataValueChanged on " + e.getName());

            synchronized (dataListenerList) {
                dataEvents.put(e.getName(), e);

                Vector listeners = (Vector) dataListenerList.get(e.getName());
                if (listeners == null || listeners.isEmpty()) {
                    // debug("no listeners for " + e.getName());
                    continue;
                }

                // debug(listeners.size() + " listeners to notify.");
                enumeration = listeners.elements();
                while (enumeration.hasMoreElements() && isRunning) try {
                    ((DataListener)enumeration.nextElement()).dataValueChanged(e);
                } catch (RemoteException ex) { printError(ex); }
            }

        } catch (Exception exception) { printError(exception); }

        synchronized (this) {
            cleanup();
            fireRepositoryClientStopping();
        }
    }

    public synchronized void quit() {
        // debug("stopping...");
        isRunning = false;
        interrupt();

        cleanup();

        // debug("notifyAllListeners.");
        fireRepositoryClientStopping();

        // debug("quit done.");
    }

    public void addRepositoryClientListener(RepositoryClientListener l) {
        repositoryClientListeners.addElement(l);
    }

    public void removeRepositoryClientListener(RepositoryClientListener l) {
        repositoryClientListeners.removeElement(l);
    }

    private void fireRepositoryClientStopping() {
        Vector l = repositoryClientListeners;
        repositoryClientListeners = new Vector();

        for (int i = l.size();  i-- > 0; ) {
            try {
                ((RepositoryClientListener) l.elementAt(i)).repositoryClientStopping();
            } catch (Throwable t) {}
        }
    }

    private void fireRepositoryClientStoredData(String name) {
        Vector l = repositoryClientListeners;

        for (int i = l.size();  i-- > 0; ) {
            try {
                RepositoryClientListener cl =
                    (RepositoryClientListener) l.elementAt(i);
                cl.repositoryClientStoredData(name);
            } catch (Throwable t) {}
        }
    }


    public void putValue(String name, SaveableData value)
        throws RemoteException {
            PrintWriter out = null;
            synchronized (this) { out = this.out; }
            if (out == null)
                throw new RemoteException();

            try {
                synchronized (out) {
                    out.println("putValue");
                    out.println(name);
                    out.println(value == null ? "null" : value.saveString());
                    out.flush();
                }
            } catch (Exception e) {
                printError(e);
                throw new RemoteException();
            }
            fireRepositoryClientStoredData(name);
    }

    public void removeValue(String name) throws RemoteException {
        PrintWriter out = null;
        synchronized (this) { out = this.out; }
        if (out == null)
            throw new RemoteException();

        try {
            synchronized (out) {
                out.println("removeValue");
                out.println(name);
                out.flush();
            }
        } catch (Exception e) {
            printError(e);
            throw new RemoteException();
        }
    }


    public void maybeCreateValue(String name, String value, String prefix)
        throws RemoteException {
        PrintWriter out = null;
        synchronized (this) { out = this.out; }
        if (out == null)
            throw new RemoteException();

        try {
            synchronized (out) {
                out.println("maybeCreateValue");
                out.println(name);
                out.println(value);
                out.println(prefix);
                out.flush();
            }
        } catch (Exception e) {
            printError(e);
            throw new RemoteException();
        }
    }


    public void addDataListener(String name, DataListener dl)
        throws RemoteException {
        PrintWriter out = null;
        synchronized (this) { out = this.out; }
        if (out == null)
            throw new RemoteException();

        // get the list of DataListeners that are interested in name
        Vector dataListeners = (Vector) dataListenerList.get(name);

        synchronized (dataListenerList) {
            if (dataListeners != null) {
                // add dl to the list of interested DataListeners.
                dataListeners.addElement(dl);

                // send dl the most recent DataEvent if there has been one.
                DataEvent e = (DataEvent) dataEvents.get(name);
                if (e != null)
                    dl.dataValueChanged(e);

            } else {
                // No one was previously interested in name. So we create the list of
                // DataListeners interested in name, and add dl to it.
                dataListeners = new Vector();
                dataListenerList.put(name, dataListeners);
                dataListeners.addElement(dl);

                // also, tell the RepositoryServer that we are interested in name.
                try {
                    synchronized (out)  {
                        out.println("addDataListener");
                        out.println(name);
                        out.flush();
                    }
                } catch (Exception e) {
                    printError(e);
                    throw new RemoteException();
                }
            }
        }
    }


    public void removeDataListener(String name, DataListener dl)
        throws RemoteException {
        PrintWriter out = null;
        synchronized (this) { out = this.out; }
        if (out == null)
            throw new RemoteException();

        synchronized (dataListenerList) {
            // get the list of DataListeners that are interested in name
            Vector dataListeners = (Vector) dataListenerList.get(name);
            if (dataListeners == null) return;

            // remove dl from the list of interested DataListeners.
            dataListeners.removeElement(dl);

            // if there are now no DataListeners interested in name,
            if (dataListeners.isEmpty()) {

                // drop this (empty) list from our internal hashtable.
                dataListenerList.remove(name);

                // tell the RepositoryServer that we are no longer interested in name.
                try {
                    synchronized (out) {
                        out.println("removeDataListener");
                        out.println(name);
                        out.flush();
                    }
                } catch (Exception e) {
                    printError(e);
                    throw new RemoteException();
                }
            }
        }
    }

    boolean writeLogMessage(String message) {
        PrintWriter out = null;
        synchronized (this) { out = this.out; }
        if (out == null) return false;
        try {
            message = message.replace('\n', '\u0001');
            message = message.replace('\r', ' ');
            synchronized (out) {
                out.println("logMessage");
                out.println(message);
                out.flush();
            }
            return true;
        } catch (Exception e) {
            printError(e);
        }
        return false;
    }


    public String getDataPath() {
        return dataPath;
    }

}
