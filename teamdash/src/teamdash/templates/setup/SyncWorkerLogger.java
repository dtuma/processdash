package teamdash.templates.setup;

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
