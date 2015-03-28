// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class SyncWorkerLogger implements InvocationHandler {

    SyncWorker delegate;

    List<String> logInfo;

    private SyncWorkerLogger(SyncWorker delegate, List<String> logInfo) {
        this.delegate = delegate;
        this.logInfo = logInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        Object result = method.invoke(delegate, args);

        StringBuilder logData = new StringBuilder();
        logData.append(getCallLocation());
        logData.append(method.getName()).append("(");
        if (args != null) {
            for (Object o : args)
                logData.append(o).append(", ");

            logData.setLength(logData.length() - 2);
        }
        logData.append(")");
        if (method.getReturnType() != Void.TYPE)
            logData.append(" == '").append(result).append("'");

        logInfo.add(logData.toString());

        return result;
    }

    private String getCallLocation() {
        StringWriter w = new StringWriter();
        Exception e = new Exception();
        e.printStackTrace(new PrintWriter(w));
        BufferedReader r = new BufferedReader(new StringReader(w.toString()));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                if (line.indexOf("HierarchySynchronizer") != -1)
                    return line.trim() + " ===> ";
            }
        } catch (IOException ioe) {}
        return "";
    }

    public static SyncWorker wrapWorker(SyncWorker w, List<String> info) {
        ClassLoader loader = SyncWorkerLogger.class.getClassLoader();
        Class[] interfaces = new Class[] { SyncWorker.class };
        SyncWorkerLogger logger = new SyncWorkerLogger(w, info);
        return (SyncWorker) Proxy.newProxyInstance(loader, interfaces, logger);
    }

}
