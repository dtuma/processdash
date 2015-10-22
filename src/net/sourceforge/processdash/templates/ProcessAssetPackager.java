// Copyright (C) 2015 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.templates;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;

public class ProcessAssetPackager {

    private static File destFile;

    private static String packageId;

    private static String packageVersion;

    private static String packageName;

    private static Map<String, File> assets;



    public static void main(String[] args) {
        loadCommandLineArgs(args);

        try {
            writeJarFile();
        } catch (IOException e) {
            System.out.println("Could not write " + destFile + "; "
                    + e.getMessage());
        }
    }



    private static void loadCommandLineArgs(String[] args) {
        if (args.length < 5)
            showUsage();

        // read the name of the dest file
        String destFilename = args[0];
        if (!destFilename.endsWith(".zip") && !destFilename.endsWith(".jar"))
            destFilename += ".zip";
        destFile = new File(destFilename);

        // read and validate the package ID
        packageId = args[1].trim();
        if (!PKG_ID_PAT.matcher(packageId).matches())
            showUsage();

        // read and validate the package version
        packageVersion = args[2].trim();
        if (!PKG_VERSION_PAT.matcher(packageVersion).matches())
            showUsage();
        if (packageVersion.endsWith("..."))
            // replace a final "..." with a build timestamp
            packageVersion = packageVersion.substring(0,
                packageVersion.length() - 2)
                    + VERSION_DATE_FMT.format(new Date());

        // read and validate the package name
        packageName = args[3].trim();
        if (packageName.length() == 0)
            showUsage();

        // read and validate the list of asset files
        assets = new LinkedHashMap<String, File>();
        for (int i = 4; i < args.length; i++) {

            // make certain this is a recognized asset format
            String filename = args[i];
            if (!isValidAssetFileExtension(filename))
                showUsage();

            // make certain this file exists
            File oneAsset = new File(filename);
            if (!oneAsset.isFile())
                err("Asset not found: " + filename);

            // add this file to the list of assets
            String fileName = getUniqueEntryName(oneAsset.getName());
            assets.put(packageId + "/" + fileName, oneAsset);
        }
    }

    private static String getUniqueEntryName(String baseName) {
        // if the base name is already unique, return it.
        if (!assets.containsKey(baseName))
            return baseName;

        // try prepending numbers to make the base name unique.
        int num = 1;
        while (true) {
            String uniqueName = num + "-" + baseName;
            if (!assets.containsKey(uniqueName))
                return uniqueName;
            else
                num++;
        }
    }

    private static void showUsage() {
        try {
            FileUtils.copyFile(ProcessAssetPackager.class
                    .getResourceAsStream("ProcessAssetPackager.txt"),
                System.out);
            System.exit(1);
        } catch (IOException e) {
        }
    }

    private static void err(String message) {
        System.err.println(message);
        System.exit(1);
    }



    private static void writeJarFile() throws IOException {
        JarOutputStream jarOut = openJarOutputStream();
        writeTemplateXmlFile(jarOut);
        writeAssets(jarOut);
        jarOut.close();
        System.out.println("Wrote " + destFile);
    }

    private static JarOutputStream openJarOutputStream() throws IOException {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        attrs.putValue("Dash-Pkg-ID", packageId);
        attrs.putValue("Dash-Pkg-Version", packageVersion);
        attrs.putValue("Dash-Pkg-Name", packageName);

        return new JarOutputStream(new FileOutputStream(destFile), mf);
    }

    private static void writeTemplateXmlFile(JarOutputStream jarOut)
            throws IOException {
        jarOut.putNextEntry(new ZipEntry("Templates/assets-template.xml"));

        Writer out = new OutputStreamWriter(jarOut, "UTF-8");
        out.write("<?xml version='1.0'?>\n\n");
        out.write("<dashboard-process-template>\n\n");

        for (String filename : assets.keySet()) {
            String[] format = getAssetFormat(filename);
            out.write("    <");
            out.write(format[1]);
            out.write(" ");
            out.write(format[2]);
            out.write("=\"");
            out.write(HTMLUtils.escapeEntities(filename));
            out.write("\"/>\n\n");
        }

        out.write("</dashboard-process-template>\n");
        out.flush();
        jarOut.closeEntry();
    }

    private static void writeAssets(JarOutputStream jarOut) throws IOException {
        for (Entry<String, File> e : assets.entrySet()) {
            String fileName = e.getKey();
            jarOut.putNextEntry(new ZipEntry("Templates/" + fileName));
            FileUtils.copyFile(e.getValue(), jarOut);
            jarOut.closeEntry();
        }
    }

    private static boolean isValidAssetFileExtension(String filename) {
        return getAssetFormat(filename) != null;
    }

    private static String[] getAssetFormat(String filename) {
        for (String[] assetFormat : ASSET_FORMATS) {
            if (filename.endsWith(assetFormat[0]))
                return assetFormat;
        }
        return null;
    }



    private static final Pattern PKG_ID_PAT = Pattern
            .compile("[a-zA-Z_0-9.-]+");

    private static final Pattern PKG_VERSION_PAT = Pattern.compile("[0-9.]+");

    private static final DateFormat VERSION_DATE_FMT = new SimpleDateFormat(
            "yyyyMMddHHmm");

    private static final String[][] ASSET_FORMATS = { //
            { ".wfxml", "org-workflows", "uri" }, //
            { ".tabxml", "customWbsTabs", "specFile" }, //
            { ".colxml", "customWbsColumns", "specFile" }, //
            { ".estxml", "org-proxies", "uri" }, //
            { ".dtsxml", "defect-types", "uri" }, //
    };
}
