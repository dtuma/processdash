
package teamdash.templates.tools;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import net.sourceforge.processdash.ui.web.TinyCGIBase;
import teamdash.process.CustomProcess;
import teamdash.process.CustomProcessPublisher;

public class GenerateTestProcess extends TinyCGIBase {

    protected void writeContents() throws IOException {
        CustomProcess process = new CustomProcess();
        process.setName("Test");
        process.setVersion("2");
        String dir = getParameter("dir");
        File dest = new File(dir);
        dest = new File(dest, process.getJarName());
        CustomProcessPublisher.publish(process, dest, getTinyWebServer());
    }

    private static final String THIS_URL =
        "http://localhost:2468/team/tools/GenerateTestProcess.class";
    public static void main(String[] args) {
        try {
            URL u = new URL(THIS_URL + "?dir="
                        + URLEncoder.encode(args[0], "UTF-8"));
            URLConnection conn = u.openConnection();
            conn.connect();
            conn.getInputStream();
        } catch (Exception e) {}
    }

}
