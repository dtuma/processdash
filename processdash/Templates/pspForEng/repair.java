
import pspdash.*;
import pspdash.data.*;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Enumeration;

public class repair extends TinyCGIBase {

    private DataRepository data;
    private String prefix;
    private TimeLog tl;
    private ListData subprojects;
    private int LAST_STEP;
    private StringBuffer buf;


    protected void doGet() throws IOException {
        writeHeader();


        int step = 0;
        String stepParam = (String) parameters.get("step");
        if (stepParam != null)
            step = Integer.parseInt(stepParam);

        boolean finished = doStep(step);
        writeTopPart(step, finished);
        out.write(buf.toString());

        if (finished)
            out.write("<p><b>Finished!</b>");

        out.write("</body></html>");
    }


    private void writeTopPart(int step, boolean finished) throws IOException {
        out.write("<html><head><title>Recalc/Repair To Date Data</title>");


        if (!finished) {

            int delay = (step == 0 ? 10 : 7);

            out.print("<meta http-equiv=\"Refresh\" CONTENT=\""+
                      delay+";URL=repair.class?step=" + (step+1) + "\">");
            out.print("</head><body>" +
                      "<h1>Recalculating...</h1>"+
                      "Recalculating To Date Data...please wait.<ul>");
        } else {
            out.print("</head><body>" +
                      "<h1>Finished Recalculating</h1>"+
                      "Recalculated To Date Data:<ul>");
        }
    }


    public boolean doStep(int step) throws IOException {
        init();

        if (writeStep("Clearing completion flags", 0, step))
            clearCompletionFlags();
        if (writeStep("Repairing overwritten calculations", 1, step))
            redefaultCalculations();

        int stepNum = 2;
        for (int i = 0;   i < subprojects.size();   i++) {
            String projectName = (String) subprojects.get(i);
            Date projectCompletion = getCompletionDate(projectName);
            if (projectCompletion == null) {
                LAST_STEP = stepNum;
                break;
            }

            if (writeStep("Marking "+projectName+"/Planning complete",
                          stepNum++, step))
                completePlanning(projectName, projectCompletion);

            if (writeStep("Marking "+projectName+" complete",
                          stepNum++, step))
                completeProject(projectName, projectCompletion);
        }

        out.println("</ul>");
        return step == LAST_STEP;
    }
    private boolean writeStep(String message, int num, int currentStep) {
        if (num > currentStep)
            return false;

        buf.append("<li>");
        buf.append(message);
        if (num < currentStep) {
            buf.append("...done.");
            return false;

        } else {
            buf.append("...<b>please wait.</b>");
            return true;
        }
    }

    private void init() throws IOException {
        data = getDataRepository();
        prefix = getPrefix();

        tl = new TimeLog();
        tl.readDefault();

        subprojects = (ListData) getValue("Project_List");
        LAST_STEP = 2 + (subprojects.size() * 2);

        buf = new StringBuffer();
    }

    private SimpleData getValue(String name) {
        return getValue(prefix, name);
    }
    private SimpleData getValue(String prefix, String name) {
        String dataName = DataRepository.createDataName(prefix, name);
        return data.getSimpleValue(dataName);
    }


    private void clearCompletionFlags() {
        try {
            data.startInconsistency();
            for (int idx = 0;   idx < subprojects.size();   idx++) {
                String projectName = (String) subprojects.get(idx);
                clearCompletions(projectName);
            }
        } finally {
            data.finishInconsistency();
        }
    }
    private void clearCompletions(String project) {
        String dataName = DataRepository.createDataName
            (project, "Planning/Completed");
        System.out.println("nulling "+dataName);
        data.putValue(dataName, null);
        dataName = DataRepository.createDataName(project, "Completed");
        System.out.println("nulling "+dataName);
        data.putValue(dataName, null);
    }

    private void redefaultCalculations() {
        Iterator i = data.getKeys();
        while (i.hasNext()) {
            String dataName = (String) i.next();
            maybeRepair(dataName);
        }
    }
    private void maybeRepair(String dataName) {
        if (!dataName.startsWith(prefix)) return;
        if (!dataName.endsWith(" To Date")) return;

        SaveableData value = data.getValue(dataName);
        if (!isPlainValue(value)) return;

        data.restoreDefaultValue(dataName);
        SaveableData restoredValue = data.getValue(dataName);
        if (isPlainValue(restoredValue))
            data.putValue(dataName, value);
        else
            System.out.println("repaired "+dataName);
    }
    private boolean isPlainValue(SaveableData value) {
        return (value == null ||
                value instanceof DoubleData ||
                value instanceof FrozenData);
    }

    private Date getCompletionDate(String projectName) {
        return getLatestDate(projectName + "/Postmortem");
    }
    private void completePlanning(String projectName, Date date) {
        complete(projectName, date, "Planning/Completed");
    }
    private void completeProject(String projectName, Date date) {
        complete(projectName, date, "Completed");
    }
    private void complete(String projectName, Date date, String elem) {
        String dataName = DataRepository.createDataName(projectName, elem);
        System.out.println("Completing "+dataName);
        DateData d = new DateData(date, true);
        data.putValue(dataName, d);
    }

    private Date getLatestDate(String fullPath) {

        PSPProperties props = getPSPProperties();
        Enumeration rows = tl.filter
            (props.findExistingKey(fullPath), null, null);
        TimeLogEntry tle;
        String entryPath;
        Date result = null;
        while (rows.hasMoreElements()) {
            tle = (TimeLogEntry) rows.nextElement();
            entryPath = tle.getPath();
            if (fullPath.equals(entryPath))
                result = tle.getStartTime();
        }
        return result;
    }

}
