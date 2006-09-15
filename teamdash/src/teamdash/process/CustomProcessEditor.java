package teamdash.process;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.net.http.WebServer;

public class CustomProcessEditor extends AbstractCustomProcessEditor {

    public static void main(String[] args) {
        try {
            new CustomProcessEditor(null, GenerateProcess.getTinyWebServer());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    WebServer webServer;

    public CustomProcessEditor(String prefix, WebServer webServer) {
        super(prefix);
        this.webServer = webServer;
        frame.setVisible(true);
    }

    protected void publishProcess(CustomProcess process, File destFile)
            throws IOException {
        CustomProcessPublisher.publish(process, destFile, webServer);
    }
}
