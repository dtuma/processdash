
package pspdash.data;

import com.ms.wfc.ui.MessageBox;

public class OLEDBAlertWindow  {

    private static boolean haveDisplayedDialog = false;

    public static void display() {
        System.out.println("OLEDBAlertWindow.display()");
        if (haveDisplayedDialog) return;

        MessageBox.show(ERROR_MESSAGE, "Problem with configuration");
        haveDisplayedDialog = true;
    }

    private static final String ERROR_MESSAGE =
        "A configuration problem is preventing this html page from " +
        "communicating with the dashboard.  For instructions on how " +
        "to solve this problem, point your web browser at " +
        "http://localhost:2468/help/trustlib.htm";
}
