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

import pspdash.InternalSettings;
import java.net.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

public class RepositoryServer extends Thread {

    int port = 2467;
    DataRepository data = null;
    ServerSocket serverSocket = null;
    Vector serverThreads = new Vector();

    private static final String RESTORE_DEFAULT_TOKEN =
        DataInterpreter.RESTORE_DEFAULT_TOKEN.saveString();

    private class RepositoryServerThread extends Thread implements DataListener {

        DataRepository data = null;
        Socket clientSocket = null;
        BufferedReader in = null;
        ObjectOutputStream out = null;
        public String dataPath = null;

        public RepositoryServerThread(DataRepository data, Socket clientSocket) {
            this.data = data;
            this.clientSocket = clientSocket;

            try {
                // debug("getting output stream...");
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                // send some ignored data through the streams to test connectivity
                // debug("writing to output stream...");
                out.flush();

                // debug("getting input stream...");
                in = new BufferedReader
                    (new InputStreamReader(clientSocket.getInputStream()));
                // debug("reading from input stream...");
                String ID = in.readLine();
                String requiredTag = in.readLine();
                if (ID.startsWith("/"))
                    dataPath = URLDecoder.decode(ID);
                else
                    dataPath = data.getPath(ID);
                if (dataPath == null)
                    dataPath = "//anonymous//";

                // debug("Done. got both streams.  Writing settings information.");

                out.writeObject(dataPath);
                if (requiredTag.length() == 0)
                    out.writeBoolean(true);
                else {
                    SimpleData d =
                        data.getSimpleValue(data.createDataName(dataPath,requiredTag));
                    out.writeBoolean(d != null && d.test());
                }
                out.writeObject(InternalSettings.getSettings());
                out.flush();

            } catch (IOException e) { printError(e); }

            try {
                setName(getName() + "(RepositoryServerThread"+dataPath+")");
            } catch (Exception e) {}
        }

        private void debug(String msg) {
            // System.out.println("RepositoryServerThread: " + msg);
        }

        private void printError(Exception e) {
            if (threadIsRunning) {
                System.err.println("Exception: " + e);
                e.printStackTrace(System.err);
            }
        }

        private volatile boolean threadIsRunning = true;

        public void run() {
            String methodName = null;
            String dataName = null;
            String value = null;
            String prefix = null;

            while (threadIsRunning) try {
                // debug("reading from socket...");
                try {
                    methodName = null;
                    methodName = in.readLine();
                } catch (SocketException se) {
                    if (threadIsRunning) {
                        System.err.println ("socket error, dying...");
                        printError (se);
                    }
                    threadIsRunning = false;
                }
                // debug("method is "+methodName);

                                        // quit
                if (methodName == null || methodName.equals("quit"))
                    threadIsRunning = false;

                                          // putValue
                else if (methodName.equals("putValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    value = in.readLine();        // debug("    arg is "+value);
                    if (RESTORE_DEFAULT_TOKEN.equals(value))
                        data.restoreDefaultValue(dataName);
                    else
                        data.userPutValue(dataName,
                                          ValueFactory.create(null, value, null, null));
                }
                                        // removeValue
                else if (methodName.equals("removeValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.removeValue(dataName);
                }
                                        // addDataListener
                else if (methodName.equals("addDataListener")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.addDataListener(dataName, this);
                }
                                        // removeDataListener
                else if (methodName.equals("removeDataListener")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    data.removeDataListener(dataName, this);
                }

                else if (methodName.equals("maybeCreateValue")) {
                    dataName = in.readLine();     // debug("    arg is "+dataName);
                    value = in.readLine();        // debug("    arg is "+value);
                    prefix = in.readLine();       // debug("    arg is "+prefix);
                    data.maybeCreateValue(dataName, value, prefix);
                }

                else if (methodName.equals("listDataNames")) {
                    prefix = in.readLine();       // debug("    arg is "+prefix);
                    Vector dataNames = data.listDataNames(prefix);
                    synchronized (out) {
                        out.writeObject(dataNames);
                    }
                }

                else
                    System.err.println("RepositoryServerThread: I don't understand " +
                                       methodName);

            } catch (Exception e) { printError(e); }

            cleanup();
        }

        public void dataValueChanged(DataEvent e) {
            try {
                synchronized (out) {
                    sendDataEvent(e);
                    out.flush();
                }
            } catch (Exception ex) { printError(ex); }
        }

        public void dataValuesChanged(Vector v) {
            if (v == null || !threadIsRunning) return;

            synchronized (out) {
                for (int i = v.size();  i > 0; )
                    try {
                        sendDataEvent((DataEvent) v.elementAt(--i));
                    } catch (Exception ex) { printError(ex); }

                try {
                    out.flush();
                } catch (Exception ex) { printError(ex); }
            }
        }

        // WARNING - you must manually synchronize on the <code>out</code>
        // object before calling this method.
        private void sendDataEvent(DataEvent e) throws IOException {
            if (e.getValue() == null) {
                SaveableData d = data.getValue(e.getName());
                if (d != null && !d.isEditable())
                    e = new DataEvent((Repository) e.getSource(), e.getName(),
                                      e.getID(), ImmutableStringData.EMPTY_STRING);
            }
            out.writeObject(e);
        }

        private void cleanup() {
            try {
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException e) { printError(e); }

            data.deleteDataListener(this);
        }

        public void quit() {
            threadIsRunning = false;
            interrupt();
            cleanup();
        }
    }


    private void debug(String msg) {
        // System.out.println("RepositoryServer: " + msg);
    }

    private void printError(Exception e) {
        if (serverIsRunning) {
            System.err.println("Exception: " + e);
            e.printStackTrace(System.err);
        }
    }

    public RepositoryServer(DataRepository r, ServerSocket socket) {
        data = r;
        serverSocket = socket;
        if (socket != null)
            port = socket.getLocalPort();
        else
            port = -1;
    }

    public int getPort() {
        return port;
    }

    private volatile boolean serverIsRunning = true;

    public void handle(Socket clientSocket) {
        RepositoryServerThread newServerThread =
            new RepositoryServerThread(data, clientSocket);
        newServerThread.start();
        serverThreads.addElement(newServerThread);
    }

    public void run() {
        Socket clientSocket = null;

        if (serverSocket == null) return;

        while (serverIsRunning) try {
            // debug("accepting...");
            clientSocket = serverSocket.accept();
            // debug("got a connection.");
            handle(clientSocket);
        } catch (IOException e) {
            printError(e);
        }
    }

    public void deletePrefix(String prefix) {
        for (int i = serverThreads.size();   i-- > 0; ) {
            RepositoryServerThread thread =
                (RepositoryServerThread) serverThreads.elementAt(i);
            if (thread.dataPath.equals(prefix)) {
                thread.quit();
                serverThreads.removeElement(thread);
            }
        }
    }

    public synchronized void quit() {
        serverIsRunning = false;
        interrupt();

        for (int i = serverThreads.size();   i-- > 0; )
            ((RepositoryServerThread) serverThreads.elementAt(i)).quit();
    }

}
