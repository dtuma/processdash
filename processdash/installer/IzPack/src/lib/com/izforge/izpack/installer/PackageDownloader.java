
package com.izforge.izpack.installer;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

import com.izforge.izpack.LocaleDatabase;

public class PackageDownloader extends Thread {

    /** We want the user to be aware that something was downloaded.
     * Therefore, even if the download is instantaneous, we will display
     * the download progress dialog for at least this many milliseconds.
     */
    private static final long MIN_DELAY = 1000;

    private static final int MIN_DELAY_OFFSET = 1000;

    private String packageName;

    private JDialog dialog;

    private JProgressBar progressBar;

    private long totalSize;

    private InputStream inputStream;

    private LocaleDatabase langpack;

    public PackageDownloader(String packageName, String packageLocation, LocaleDatabase langpack)
    {
        this.packageName = packageName;
        this.langpack = langpack;

        try {
            URL url = new URL(packageLocation);
            URLConnection conn = url.openConnection();
            conn.setAllowUserInteraction(true);
            conn.setRequestProperty
                ("X-Process-Dashboard-Installer", "1.6");
            totalSize = conn.getContentLength();
            inputStream = conn.getInputStream();

            buildDialog();
            start();
            dialog.show();
        } catch (IOException ioe) {
            inputStream = null;
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    private void buildDialog() {
        dialog = new JDialog((Frame) null, langpack.getString("PackageDownloader.title"), true);
        dialog.getContentPane().setLayout(new GridLayout(2, 1, 10, 10));

        String label = langpack.getString("PackageDownloader.info1")
            + packageName + langpack.getString("PackageDownloader.info2");
        dialog.getContentPane().add(new JLabel(label));

        if (totalSize > 0) {
            progressBar = new JProgressBar(-MIN_DELAY_OFFSET, (int) totalSize);
            dialog.getContentPane().add(progressBar);
        }

        dialog.pack();
        Dimension dialogSize = dialog.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screenSize.width - dialogSize.width) / 2,
                           (screenSize.height - dialogSize.height) / 2 - 10);
    }

    public void run() {
        try {
            long startTime = System.currentTimeMillis();

            ByteArrayOutputStream slurpBuffer = null;
            if (totalSize <= 0)
                slurpBuffer = new ByteArrayOutputStream();
            else
                slurpBuffer = new ByteArrayOutputStream
                    ((int) (totalSize + 100));

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                slurpBuffer.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (progressBar != null)
                    progressBar.setValue(totalBytesRead - MIN_DELAY_OFFSET);
            }

            long endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;
            long remaining = MIN_DELAY - elapsed;
            if (remaining > 0)
                Thread.sleep(remaining);
            if (progressBar != null)
                progressBar.setValue(totalBytesRead);

            inputStream = new ByteArrayInputStream(slurpBuffer.toByteArray());
        } catch (Exception e) {
            inputStream = null;
        } finally {
            dialog.dispose();
        }
    }


}
