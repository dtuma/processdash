
import java.io.IOException;

import pspdash.DashController;
import pspdash.TinyCGIBase;


public class CustomProcessEditor extends TinyCGIBase {

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        new teamdash.process.CustomProcessEditor(getPrefix(), getTinyWebServer());
        DashController.printNullDocument(out);
    }
}
