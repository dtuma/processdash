// Copyright (C) 2002-2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.team.mcf.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.team.mcf.CustomProcess;

public class CustomProcessEditorJNLP extends AbstractCustomProcessEditor {

    private URL servletURL;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("You must supply the servlet url");
            System.exit(1);
        }

        new CustomProcessEditorJNLP(args[0]);
    }

    public CustomProcessEditorJNLP(String servletURL) {
        super(null);
        try {
            this.servletURL = new URL(servletURL);
            URL baseURL = new URL(this.servletURL, "/");
            URLConnection conn = baseURL.openConnection();
            conn.connect();
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, CANNOT_CONNECT_MESSAGE,
                    "Unable to Open Metrics Framework Editor",
                    JOptionPane.ERROR_MESSAGE);

            System.err.print("Could not connect to server");
            ioe.printStackTrace();
            System.exit(1);
        }
        frame.setVisible(true);
    }


    private static final String[] CANNOT_CONNECT_MESSAGE = {
        "To generate a custom metrics framework, this editor",
        "must connect to the Internet.  Unfortunately, this",
        "connection was not successful.  Make certain you",
        "are connected to the Internet, and try again. If you",
        "are connected to the Internet, but you still see",
        "this error message, contact",
        "        support@tuma-solutions.com",
        "for help."
    };


    protected void publishProcess(CustomProcess process, File destFile)
            throws IOException {
        boolean connected = false;

        try {
            // connect to servlet
            URLConnection con = servletURL.openConnection();
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
            connected = true;
            FileOutputStream fos = new FileOutputStream(destFile);

            while ((bytesRead = in.read(buf)) != -1)
                fos.write(buf, 0, bytesRead);

            fos.flush();
            fos.close();
        } finally {
            if (!connected)
                JOptionPane.showMessageDialog(frame, CANNOT_CONNECT_MESSAGE,
                    "Unable to Save Metrics Framework",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

}
