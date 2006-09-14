package teamdash.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class CustomProcessEditorJNLP extends AbstractCustomProcessEditor {

    private static String servletURL;

    public static void main(String[] args) {
        new CustomProcessEditorJNLP(null);

        if (args.length == 0) {
            System.err.println("You must supply the servlet url");
            System.exit(1);
        }

        servletURL = args[0];
    }

    public CustomProcessEditorJNLP(String prefix) {
        super(prefix);
    }

    protected void publishProcess(CustomProcess process, File destFile)
            throws IOException {
        // connect to servlet
        URL url = new URL(servletURL);
        URLConnection con = url.openConnection();
        con.setDoOutput(true);
        con.connect();

        // pass process data to servlet
        OutputStreamWriter writer = new OutputStreamWriter(con
                .getOutputStream());
        process.writeXMLSettings(writer);
        writer.flush();
        writer.close();

        // read response
        byte[] buf = new byte[1024];
        int bytesRead;
        InputStream in = con.getInputStream();
        FileOutputStream fos = new FileOutputStream(destFile);

        while ((bytesRead = in.read(buf)) != -1)
            fos.write(buf, 0, bytesRead);

        fos.flush();
        fos.close();
    }

}
