package net.sourceforge.processdash.log.time;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.EventHandler;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableModel;

import sun.security.validator.SimpleValidator;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.SimulationHarness;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyEditor;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.ui.TimeLogEditor;
import net.sourceforge.processdash.ui.ConfigureButton;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeLoggingSimulation extends WindowAdapter {

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        if (args.length > 0)
            seed = Long.parseLong(args[0]);
        TimeLoggingSimulation sim = new TimeLoggingSimulation();
        try {
            sim.run(seed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File dataDir;

    private SimulationHarness harness;

    private long millisPerMinute = 60 * 1000;

    Random random;

    private Robot robot;

    private void run(long seed) throws Exception {
        random = new Random(seed);
        print("Seed is " + seed);
        dataDir = createAndPopulateDataDir();
        harness = new SimulationHarness(dataDir);
        ProcessDashboard dashboard = harness.getDashboard();
        dashboard.removeWindowListener(dashboard);
        dashboard.addWindowListener(this);

        robot = new Robot();

        user = new RandomUserActivityGenerator();
        user.start();

        userMeta = new RandomUserMetaActivityGenerator();
        userMeta.start();
    }

    public void windowClosing(WindowEvent e) {
        finishSimulation();
    }

    private void print(String msg) {
        printIt(System.out, msg);
    }

    private void printErr(String msg) {
        printIt(System.err, msg);
    }

    private synchronized void printIt(PrintStream ps, String msg) {
        ps.print(System.currentTimeMillis());
        ps.print(": ");
        ps.println(msg);
        ps.flush();
    }

    private void finishSimulation() {
        print("Waiting for current user activity to finish...");
        user.terminate();

        if (isTiming)
            pressPlayPauseButton();

        print("Starting final data check.");
        checkValues();
        print("Final data check finished.");

        harness.getDashboard().exitProgram();
    }

    private boolean checkValues() {
        boolean foundErr = false;

        boolean loggingModelIsTiming = !getTimeLoggingModel().isPaused();
        if (loggingModelIsTiming != isTiming) {
            printErr("We disagree with the logging model on whether or not the timer is running.");
            foundErr = true;
        }

        DashHierarchy hier = harness.getDashboard().getHierarchy();
        for (int a = 0; a < 3; a++) {
            PropertyKey aKey = hier.getChildKey(PropertyKey.ROOT, a);
            for (int b = 0; b < 3; b++) {
                PropertyKey bKey = hier.getChildKey(aKey, b);
                for (int c = 0; c < 3; c++) {
                    PropertyKey cKey = hier.getChildKey(bKey, c);
                    if (!checkTimeData(cKey.path(), expectedTimes[a][b][c]))
                        foundErr = true;
                }
            }
        }
        if (foundErr)
            printErr("data check complete");
        return !foundErr;
    }

    private boolean checkTimeData(String path, long expectedValue) {
        String dataName = path + "/Time";
        SimpleData value = harness.getDashboard().getData().getSimpleValue(
                dataName);

        if (value == null) {
            if (expectedValue == 0)
                return true;
        } else {
            double actualValue = ((DoubleData) value).getDouble();
            if (Math.abs(actualValue - expectedValue) < 0.001) {
                return true;
            }
        }

        printErr("Time mismatch for " + path + ": expected " + expectedValue
                + ", got " + (value == null ? null : value.format()));
        return false;
    }

    protected File createAndPopulateDataDir() throws IOException {
        File result = createTempDir();
        print("Starting directory is " + result);

        File stateFile = new File(result, "state");
        Writer out = new FileWriter(stateFile);
        out.write(createStartingHierarchy());
        out.close();

        File iniFile = new File(result, "pspdash.ini");
        out = new FileWriter(iniFile);
        out.write("pauseButton.quiet=true\n");
        out.write("timer.multiplier=1\n");
//		millisPerMinute /= 10;
        out.close();

        return result;
    }

    protected File createTempDir() throws IOException {
        File tempDir = File.createTempFile("test", ".tmp");
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }

    private static final String createStartingHierarchy() {
        String result = createStartingHierarchy("");
        result = createStartingHierarchy(result);
        result = createStartingHierarchy(result);
        result = "<?xml version='1.0' encoding='UTF-8'?>" + //
                "<node name='top'>" + result + "</node>";
        return result;
    }

    private static final String createStartingHierarchy(String nestedContent) {
        StringBuffer result = new StringBuffer();
        for (char c = 'A'; c < 'D'; c++)
            result.append("<node name='").append(c).append("'>").append(
                    nestedContent).append("</node>");
        return result.toString();
    }

    Timer checkValuesAfterUserActivityTimer;

    public void checkValuesAfterUserActivity() {
        print("checkValuesAfterUserActivity");
        if (checkValues() == true)
            consecutiveErrors = 0;
        else if (++consecutiveErrors > 5) {
            abortSimulation();
        }
    }

    private RandomUserActivityGenerator user;

    private class RandomUserActivityGenerator extends Thread implements
            ActionListener {

        private volatile boolean isRunning = true;

        public RandomUserActivityGenerator() {
            checkValuesAfterUserActivityTimer = new Timer(Integer.MAX_VALUE,
                    this);
            checkValuesAfterUserActivityTimer.setRepeats(false);
            checkValuesAfterUserActivityTimer.setInitialDelay(100);
        }

        public void run() {
            while (isRunning)
                synchronized (this) {
                    performAUserActivity();
                }
        }

        public void terminate() {
            isRunning = false;
            try {
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void actionPerformed(ActionEvent e) {
            checkValuesAfterUserActivity();
        }
    }

    public long[][][] expectedTimes = new long[3][3][3];

    int[] currentPath = { 0, 0, 0 };

    boolean isTiming = false;

    volatile int consecutiveErrors = 0;

    private void performAUserActivity() {
        drainAWTQueue();
        int rand = random.nextInt(100);

        if (rand < 10) {
            switchActiveNode();

        } else if (rand < 40) {
            pressPlayPauseButton();

        } else {
            // do nothing.
        }

        long start = System.currentTimeMillis();
        shouldNotAllowTimerTick = false;
        // let some time elapse.
        try {
            Thread.sleep(millisPerMinute);
        } catch (InterruptedException e) {
        }
        long elapsed = System.currentTimeMillis() - start;

        if (isTiming && (shouldNotAllowTimerTick == false)) {
            boolean timeLogEntryExists = currentEntryExists();
            long threshhold = (timeLogEntryExists ? millisPerMinute / 2
                    : millisPerMinute);
            if (elapsed >= threshhold) {
                print("timer tick for " + getPathStr(currentPath));
                changeExpectedTime(currentPath, 1);
            }
        }

        checkValuesAfterUserActivityTimer.restart();
    }

    private void abortSimulation() {
        printErr("There seems to be a problem.  Halting to preserve files for inspection.");
        printErr("See files in    " + this.dataDir);
        for (int i = 0; i < 5; i++)

            Toolkit.getDefaultToolkit().beep();
        System.exit(0);
    }

    private synchronized void changeExpectedTime(int[] path, int delta) {
        int a = path[0];
        int b = path[1];
        int c = path[2];
        expectedTimes[a][b][c] += delta;
    }

    private void switchActiveNode() {
        int a = random.nextInt(3);
        int b = random.nextInt(3);
        int c = random.nextInt(3);
        final int[] newPath = new int[] { a, b, c };
        print("Switching to node " + getPathStr(newPath));
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    harness.selectNode(newPath);
                }
            });
            currentPath = newPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void pressPlayPauseButton() {
        print((isTiming ? "Stopping" : "Starting") + " timer");
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    harness.getPauseButton().actionPerformed(
                            new ActionEvent(this, 0, null));
                }
            });
            isTiming = !isTiming;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RandomUserMetaActivityGenerator userMeta;

    private volatile boolean shouldNotAllowTimerTick = false;

    public class RandomUserMetaActivityGenerator extends Thread {

        private volatile boolean isRunning = true;

        public void run() {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }
            while (isRunning)
                synchronized (this) {
                    performAUserMetaActivity();
                }
        }

        public void terminate() {
            isRunning = false;
            try {
                join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void performAUserMetaActivity() {
        Toolkit.getDefaultToolkit().beep();
        sleep(3000);

        int rand = random.nextInt(100);

        // don't make meta changes while we're in an error state - we want
        // to preserve the problem for viewing.
        while (consecutiveErrors > 0)

            sleep(millisPerMinute);

        print(isTiming ? "Timer is running" : "Timer is paused");
        if (rand < 30)
            performHierarchyRename();
        else
            performTimeLogEdit();

        // let some time elapse: 5 - 15 minutes. Keep this very random, so
        // meta activities are happening on a different schedule than the
        // regular activities above.
        try {
            double minutesToWait = 10 * (0.5 + random.nextDouble());
            Thread.sleep((long) (millisPerMinute * minutesToWait));
        } catch (InterruptedException e) {
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }

    private void performHierarchyRename() {
        JMenu cMenu = harness.getConfigureButton().getMenu(0);
        JMenuItem hierarchyMenuItem = cMenu.getItem(0);
        hierarchyMenuItem.doClick();

        Frame f = findFrame(Hierarchy_Resources.getString("HierarchyEditor"));
        boolean currentPathAltered = false;

        // look at each top-level item in the hierarchy.
        for (int i = 0; i < 3; i++)
            // randomly decide whether to rename it.
            if (random.nextBoolean()) {
                // rename it to something arbitrary.
                click(f, 49, 85 + 18 * i);
                type(KeyEvent.VK_F2);
                String newName = "";
                for (int j = random.nextInt(5) + 2; j-- > 0;) {
                    char c = (char) (KeyEvent.VK_A | random.nextInt(25));
                    type(c);
                    newName += c;
                }
                type(KeyEvent.VK_ENTER);
                String oldName = getPathStr(new int[] { i });

                if (i == currentPath[0]) {
                    currentPathAltered = true;
                    print("Renaming hierarchy of current task from " + oldName
                            + " to /" + newName);
                } else {
                    print("Renaming hierarchy from " + oldName + " to /"
                            + newName);
                }
            }

        WindowCloseWatcher wcw = new WindowCloseWatcher(f);

        // save the changes, then close the hierarchy editor.
        print("about to save hierarchy changes");
        click(f, 20, 35); // click "File" menu
        click(f, 30, 60); // click "Close" menu item
        // "save changes?" window will appear, and offer to save changes.
        type(KeyEvent.VK_ENTER); // type "Enter" to accept the save offer.
        wcw.waitForClose();
        print("hierarchy changes saved");

        if (currentPathAltered)
            user.interrupt();
    }

    private class WindowCloseWatcher extends WindowAdapter {
        private Window w;

        private volatile boolean closed;

        public WindowCloseWatcher(Window w) {
            this.w = w;
            this.closed = false;
            w.addWindowListener(this);
        }

        public synchronized void waitForClose() {
            while (closed == false) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public synchronized void windowClosed(WindowEvent e) {
            w.removeWindowListener(this);
            closed = true;
            notify();
        }

    }

    // private void waitForHierarchyEditorClose() {
    // // some time will go by, not on the AWT thread, while the changes are
    // // applied. We'll know it's done when the hierarchy editor frame is
    // // no longer visible.
    // while (true) {
    // Frame f = findFrame(Hierarchy_Resources
    // .getString("HierarchyEditor"));
    // if (f == null || f.isShowing() == false)
    // break;
    // sleep(20);
    // }
    // }

    private void performTimeLogEdit() {
        int[] currentPath = this.currentPath;

        JMenu cMenu = harness.getConfigureButton().getMenu(0);
        JMenuItem hierarchyMenuItem = cMenu.getItem(1);
        hierarchyMenuItem.doClick();

        Frame f = findFrame(Time_resources
                .getString("Time_Log_Editor_Window_Title"));
        JTable dataTable = (JTable) findEmbeddedComponent(f, JTable.class);
        TableModel timeData = dataTable.getModel();
        boolean currentEntryExists = currentEntryExists();
        boolean currentEntryAltered = editSomeEntries(f, timeData, currentPath,
                currentEntryExists);
        click(f, 590, 380); // click the "Close" button
        print("about to save time log edits");
        // "save changes?" window will appear, and offer to save changes.
        type(KeyEvent.VK_ENTER); // type "Enter" to accept the save offer.
        print("time log edits saved.");

        if (currentEntryAltered)
            user.interrupt();
    }

    private boolean currentEntryExists() {
        return getTimeLoggingModel().isDirty();
    }

    private TimeLoggingModel getTimeLoggingModel() {
        return ((DashboardTimeLog) harness.getDashboard().getTimeLog())
                .getTimeLoggingModel();
    }

    /**
     * @param f
     * @param timeData
     * @param editPath
     * @return true if the "current time log entry" has been deleted or
     *         modified.
     */
    private boolean editSomeEntries(Frame f, TableModel timeData,
            int[] editPath, boolean entryExists) {
        String path = getPathStr(editPath);

        if (random.nextInt(25) == 0) {
            // every once in a while, click the summarize button.
            print("Summarizing time log for path " + path);
            click(f, 630, 340); // click the sumamrize button
            type(KeyEvent.VK_ENTER); // click OK
            return entryExists;
        }

        boolean result = false;
        int currentEntryRow = (entryExists ? (timeData.getRowCount() - 1) : -1);

        while (random.nextBoolean() && timeData.getRowCount() > 1) {
            // delete one of the first ten or so rows. (Don't go higher, just
            // because we might not be able to click on that row without
            // scrolling.)
            int rowToDelete = random.nextInt(Math.min(10, timeData
                    .getRowCount()));
            boolean deletingCurrentEntry = (rowToDelete == currentEntryRow);
            int timeDeleted = (int) FormatUtil
                    .parseTime((String) getValueInTableModel(timeData,
                            rowToDelete, COL_ELAPSED));
            click(f, 380, 83 + rowToDelete * 16); // highlight the row
            click(f, 530, 340); // click the delete button
            changeExpectedTime(editPath, -timeDeleted);
            if (deletingCurrentEntry) {
                result = true;
                currentEntryRow = -1;
                print("Deleting current time log entry with time "
                        + timeDeleted + " for path " + path);

            } else {
                currentEntryRow--;
                print("Deleting other time log entry with time " + timeDeleted
                        + " for path " + path);
            }
            reallyDrainAWTQueue();
        }

        if (random.nextBoolean()) {
            // add an entry.
            click(f, 470, 340);
            int newRow = timeData.getRowCount() - 1;
            int newTime = random.nextInt(30);
            setValueInTableModel(timeData, FormatUtil.formatTime(newTime),
                    newRow, COL_ELAPSED);
            changeExpectedTime(editPath, newTime);
            print("Adding time log entry for " + path + " with elapsed "
                    + newTime);
            reallyDrainAWTQueue();
        }

        // edit some entries.
        // give a bias toward editing the current time log entry.
        int rowToEdit = timeData.getRowCount() - 1;
        for (int numEdits = timeData.getRowCount(); numEdits-- > 0;) {
            path = (String) getValueInTableModel(timeData, rowToEdit, COL_PATH);
            editPath = getPathFor(path);

            boolean editingCurrentRow;
            if (rowToEdit == currentEntryRow) {
                editingCurrentRow = true;
                print("Editing current time log entry for path " + path);
            } else {
                editingCurrentRow = false;
                print("Editing other time log entry (row " + rowToEdit
                        + ") for path " + path);
            }

            int action = random.nextInt(10);
            if (action < 6) {
                // edit the elapsed column.
                int oldTime = (int) FormatUtil
                        .parseTime((String) getValueInTableModel(timeData,
                                rowToEdit, COL_ELAPSED));
                int newTime = random.nextInt(30);
                print("...changing time from " + oldTime + " to " + newTime);
                setValueInTableModel(timeData, FormatUtil.formatTime(newTime),
                        rowToEdit, COL_ELAPSED);
                changeExpectedTime(editPath, newTime - oldTime);
                if (editingCurrentRow) {
                    shouldNotAllowTimerTick  = true;
                    result = true;
                }

            } else if (action == 6) {
                // edit the interrupt column (should have no effect)
                setValueInTableModel(timeData, FormatUtil.formatTime(random
                        .nextInt(10)), rowToEdit, COL_INTERRUPT);
                print("...changing interrupt time");

            } else if (action == 7) {
                // edit the comment column (should have no effect)
                setValueInTableModel(timeData, "comment #"
                        + System.currentTimeMillis(), rowToEdit, COL_COMMENT);
                print("...changing comment");

            } else {
                // edit the "logged to" column
                int[] newPath = new int[] { random.nextInt(3),
                        random.nextInt(3), random.nextInt(3) };
                String newPathStr = getPathStr(newPath);
                int elapsedMinutes = (int) FormatUtil
                        .parseTime((String) getValueInTableModel(timeData,
                                rowToEdit, COL_ELAPSED));
                setValueInTableModel(timeData, newPathStr, rowToEdit, COL_PATH);
                changeExpectedTime(editPath, -elapsedMinutes);
                changeExpectedTime(newPath, elapsedMinutes);
                print("...changing path from " + path + " to " + newPathStr
                        + ", elapsed=" + elapsedMinutes);
                if (editingCurrentRow) result = true;
            }

            rowToEdit = random.nextInt(timeData.getRowCount());
            reallyDrainAWTQueue();
        }

        return result;
    }

    private void setValueInTableModel(final TableModel timeData,
            final String value, final int row, final int col) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    timeData.setValueAt(value, row, col);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object gvitm;

    private Object getValueInTableModel(final TableModel m, final int row,
            final int col) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    gvitm = m.getValueAt(row, col);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gvitm;
    }

    private String getPathStr(int[] newPath) {
        DashHierarchy hier = harness.getDashboard().getHierarchy();
        PropertyKey key = PropertyKey.ROOT;
        for (int i = 0; i < newPath.length; i++)
            key = hier.getChildKey(key, newPath[i]);
        return key.path();
    }

    private int[] getPathFor(String path) {
        DashHierarchy hier = harness.getDashboard().getHierarchy();
        for (int a = 0; a < 3; a++) {
            PropertyKey aKey = hier.getChildKey(PropertyKey.ROOT, a);
            for (int b = 0; b < 3; b++) {
                PropertyKey bKey = hier.getChildKey(aKey, b);
                for (int c = 0; c < 3; c++) {
                    PropertyKey cKey = hier.getChildKey(bKey, c);
                    if (path.equals(cKey.path()))
                        return new int[] { a, b, c };
                }
            }
        }
        new Exception("no node found for path " + path).printStackTrace();
        System.exit(0);
        return null; // unreachable
    }

    private static final int COL_PATH = 0;

    private static final int COL_START_TIME = 1;

    private static final int COL_ELAPSED = 2;

    private static final int COL_INTERRUPT = 3;

    private static final int COL_COMMENT = 4;

    private Frame findFrame(String title) {
        for (int tries = 0; tries < 10; tries++) {
            drainAWTQueue();
            Frame[] allFrames = Frame.getFrames();
            for (int i = 0; i < allFrames.length; i++) {
                if (title.equals(allFrames[i].getTitle())
                        && allFrames[i].isVisible())
                    return allFrames[i];
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private Component findEmbeddedComponent(Container parent, Class clz) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component child = parent.getComponent(i);
            if (clz.isInstance(child))
                return child;
            else if (child instanceof Container) {
                Container container = (Container) child;
                Component result = findEmbeddedComponent(container, clz);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private void click(Frame f, int x, int y) {
        Point p = new Point(x, y);
        SwingUtilities.convertPointToScreen(p, f);
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        drainAWTQueue();
    }

    private void type(int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        drainAWTQueue();
    }

    private void reallyDrainAWTQueue() {
        // try { Thread.sleep(100); } catch (Exception e) {}
        drainAWTQueue();
    }

    private void drainAWTQueue() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // do nothing.
                }
            });
        } catch (Exception e) {
        }
    }

    /**
     * type the letters in the given string.
     * 
     * @param str
     *            the string to type. Can only contain alphanumeric chars.
     */
    private void type(String str) {
        str = str.toUpperCase();
        for (int i = 0; i < str.length(); i++)
            type(str.charAt(i));
    }

    static final Resources Hierarchy_Resources = Resources
            .getDashBundle("HierarchyEditor");

    static final Resources Time_resources = Resources.getDashBundle("Time");
}
