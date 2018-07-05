package net.sourceforge.processdash;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import net.sourceforge.processdash.log.ui.PauseButton;
import net.sourceforge.processdash.ui.ConfigureButton;

public class SimulationHarness {

    ProcessDashboard dashboard;

    File dataDirectory;

    private int userInterfaceDelay = 0;

    /**
     * Start a real dashboard instance, pointing to the given data directory.
     * 
     * By the time this method finishes, the dashboard instance will have
     * successfully started, and will be displaying its main window.
     * 
     * @param dataDirectory
     *            The working directory where the dashboard instance should
     *            read/write its data files. The client of this method can
     *            prepopulate that directory with files to simulate a dashboard
     *            instance in a particular stage of historical data entry.
     */
    public SimulationHarness(File dataDirectory) {
        this.dataDirectory = dataDirectory;
        ProcessDashboard.main(new String[] { "-location=" + dataDirectory });
        dashboard = DashController.dash;
    }

    /**
     * Shut down the dashboard instance that is owned by this simulation.
     * 
     * This is roughly equivalent to choosing the "Exit" option from the "C"
     * menu. However, it will not run the FileBackupManager's backup process.
     * 
     * <b>Note:</b> this will hang the current thread (and therefore, the
     * caller of this method) if any dashboard windows have unsaved data (e.g.
     * hierarchy editor, time log editor, task & schedule dialog, etc.)
     */
    public void terminate() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    dashboard.quit(null);
                    dashboard.dispose();
                }});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getUserInterfaceDelay() {
        return userInterfaceDelay;
    }

    public void setUserInterfaceDelay(int userInterfaceDelay) {
        this.userInterfaceDelay = userInterfaceDelay;
    }

    public ProcessDashboard getDashboard() {
        return dashboard;
    }

    public ConfigureButton getConfigureButton() {
        return dashboard.configure_button;
    }

    public PauseButton getPauseButton() {
        return dashboard.pause_button;
    }

    public JMenuBar getHierarchyMenuBar() {
        return dashboard.hierarchy_menubar;
    }

    public void selectNode(String node) {
        String[] path = node.split("/");
        JMenuBar menuBar = getHierarchyMenuBar();
        for (int i = 0; i < path.length; i++) {
            JMenu menu = menuBar.getMenu(i);
            int j = 0;
            for (; j < menu.getMenuComponentCount(); j++) {
                JMenuItem menuItem = menu.getItem(i);
                if (path[i].equals(menuItem.getText())) {
                    menuItem.doClick();
                    uiDelay();
                    break;
                }
            }
            if (j == menu.getMenuComponentCount())
                throw new IllegalStateException("Could not select node " + node);
        }
    }

    public void selectNode(int[] nodePathIndexes) {
        JMenuBar menuBar = getHierarchyMenuBar();
        for (int i = 0; i < nodePathIndexes.length; i++) {
            JMenu menu = menuBar.getMenu(i);
            JMenuItem menuItem = menu.getItem(nodePathIndexes[i]);
            menuItem.doClick();
            uiDelay();
        }
    }

    private void uiDelay() {
        try {
            Thread.sleep(userInterfaceDelay);
        } catch (InterruptedException e) {
        }
    }
}
