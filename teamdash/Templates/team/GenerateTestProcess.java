
import pspdash.*;

import java.net.*;
import java.io.*;
/*
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
*/

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
        "http://localhost:2468/team/GenerateTestProcess.class";
    public static void main(String[] args) {
        try {
            URL u = new URL(THIS_URL + "?dir=" + URLEncoder.encode(args[0]));
            URLConnection conn = u.openConnection();
            conn.connect();
            InputStream result = conn.getInputStream();
        } catch (Exception e) {}
    }

}
