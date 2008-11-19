package teamdash.templates.setup;

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

    public static SyncWorker wrapWorker(SyncWorker w, List<String> info) {
        ClassLoader loader = SyncWorkerLogger.class.getClassLoader();
        Class[] interfaces = new Class[] { SyncWorker.class };
        SyncWorkerLogger logger = new SyncWorkerLogger(w, info);
        return (SyncWorker) Proxy.newProxyInstance(loader, interfaces, logger);
    }

}
