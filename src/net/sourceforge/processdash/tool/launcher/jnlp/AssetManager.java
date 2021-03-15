// Copyright (C) 2015-2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import static com.tuma_solutions.teamserver.jnlp.client.JnlpClientConstants.PROCESS_ASSET_DELIM;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XorOutputStream;

import com.tuma_solutions.teamserver.jnlp.client.JarVerifier;
import com.tuma_solutions.teamserver.jnlp.client.JnlpClientConstants;

class AssetManager {

    private boolean silent;

    private File distribDir;

    private List<Asset> assets;

    private String launchProfileJnlpUrl;

    private String launchProfileName;

    private File launchProfileDir;

    private SecureRandom random;

    private Signature signature;

    private DownloadSplashScreen splashScreen;

    AssetManager(boolean silent) {
        this.silent = silent;
        distribDir = DistributionManager.getDistribDirectory(true);
        assets = new ArrayList<Asset>();
    }

    void addAsset(String arg) {
        assets.add(new Asset(arg));
    }

    void addLaunchProfile(String jnlpUrl) {
        launchProfileJnlpUrl = jnlpUrl;
    }

    File getDownloadedLaunchProfileDir() {
        return launchProfileDir;
    }

    void downloadMissingAssets(String dataUrl) {
        try {
            downloadMissingAssetsImpl(dataUrl);
        } catch (IOException e) {
            if (!silent)
                displayDownloadErrorMessage(e);
        }
    }

    List<String> getAssetDirectoryList() {
        List<String> result = new ArrayList<String>(assets.size());
        for (Asset asset : assets) {
            if (asset.contentDir != null)
                result.add(asset.contentDir.getPath());
        }
        return result;
    }

    String getUsageFileList() {
        StringBuilder result = new StringBuilder();
        for (Asset asset : assets) {
            if (asset.contentDir != null) {
                File usageFile = new File(asset.contentDir.getParentFile(),
                        DistributionManager.USAGE_LOG_FILENAME);
                result.append("!!!").append(usageFile.getPath());
            }
        }
        return result.toString();
    }

    private void downloadMissingAssetsImpl(String dataUrl) throws IOException {
        // see if any assets need downloading, and add up their total size
        int totalDownloadSize = 0;
        for (Asset asset : assets) {
            if (asset.contentDir == null && asset.size > 0)
                totalDownloadSize += asset.size;
        }

        // if a launch profile needs downloading, retrieve its size as well
        URLConnection launchProfile = openLaunchProfile();
        if (launchProfile != null) {
            totalDownloadSize += launchProfile.getContentLength()
                    + VERIFICATION_EFFORT;
        }

        // if nothing needs downloading, exit
        if (totalDownloadSize == 0)
            return;

        // calculate the base URL for process asset downloads
        int pos = dataUrl.lastIndexOf(DATA_BRIDGE_URI_PATTERN);
        if (pos == -1)
            throw new IOException("Bad/unexpected data URL format");
        String downloadUrl = dataUrl.substring(0, pos) + PROCESS_ASSET_URI;
        random = new SecureRandom();
        splashScreen = new DownloadSplashScreen(totalDownloadSize);
        if (!silent)
            splashScreen.display();

        // download the launch profile if applicable
        IOException ioe = null;
        if (launchProfile != null) {
            try {
                downloadLaunchProfile(launchProfile);
            } catch (IOException ex) {
                ioe = ex;
            }
        }

        // download each missing asset
        for (Asset asset : assets) {
            try {
                asset.downloadContent(downloadUrl);
            } catch (IOException ex) {
                ioe = ex;
            }
        }

        // hide the download splash screen and return
        splashScreen.dispose();
        if (ioe != null)
            throw ioe;
    }

    private URLConnection openLaunchProfile() throws IOException {
        // if we have not been asked to download a launch profile, do nothing
        if (launchProfileJnlpUrl == null)
            return null;

        // look up the generic name to display for any launch profile
        launchProfileName = res.getString("Application_Name");

        // retrieve the JNLP file for the launch profile
        Element xml = JnlpFileRetriever.open(launchProfileJnlpUrl);

        // get the real name of this launch profile from the <title> tag
        NodeList nl = xml.getElementsByTagName("title");
        if (nl != null && nl.getLength() > 0)
            launchProfileName = nl.item(0).getTextContent();

        // find the <jar> tag, which will provide to the launch profile URL
        nl = xml.getElementsByTagName("jar");
        if (nl == null || nl.getLength() == 0)
            return null;
        String jarHref = ((Element) nl.item(0)).getAttribute("href");
        if (jarHref == null || jarHref.length() == 0)
            return null;

        // in silent mode, add a query arg to let the server know this is a
        // low-priority background process. The server might decline to respond
        // based on server load / available bandwidth
        if (silent)
            jarHref = HTMLUtils.appendQuery(jarHref, "bg");

        // open a connection to download the JAR, and return it
        URLConnection conn = new URL(jarHref).openConnection();
        conn.setUseCaches(false);
        return conn;
    }

    private void downloadLaunchProfile(URLConnection launchProfile)
            throws IOException {
        // display a message to the user that we are downloading the app
        splashScreen.setActivity(
            res.format("Download_Asset_FMT", launchProfileName));

        // create a temporary file to write the data to
        File tmpFile = File.createTempFile("pdashLaunchProfile", ".tmp");
        tmpFile.deleteOnExit();

        // construct streams for downloading and saving the data
        InputStream in = launchProfile.getInputStream();
        OutputStream out = new BufferedOutputStream(
                new FileOutputStream(tmpFile));

        // download the launch profile and save in the temp file
        byte[] buf = new byte[8192];
        int numRead;
        while ((numRead = in.read(buf)) != -1) {
            out.write(buf, 0, numRead);
            splashScreen.addProgress(numRead);
        }

        // close the streams we were using
        FileUtils.safelyClose(in);
        FileUtils.safelyClose(out);

        // extract and verify the launch profile data
        splashScreen.setActivity(
            res.format("Verifying_FMT", launchProfileName));
        launchProfileDir = DistributionManager.extractDistribution(tmpFile);
        splashScreen.addProgress(VERIFICATION_EFFORT);
    }

    private void displayDownloadErrorMessage(IOException e) {
        String title = res.getString("Download_Error.Title");

        List<String> message = new ArrayList<String>();
        message.addAll(Arrays.asList(res.getStrings("Download_Error.Header")));
        if (launchProfileDir == null && launchProfileJnlpUrl != null)
            message.add("  \u2022 " + launchProfileName);
        for (Asset asset : assets) {
            if (asset.contentDir == null)
                message.add("  \u2022 " + asset.displayName);
        }
        message.add(" ");
        message.addAll(Arrays.asList(res.getStrings("Network_Error_Footer")));

        ExceptionDialog.showWithSubdialog(null, title, message, e);
        System.exit(1);
    }


    private class Asset {

        String packageId, packageVersion, uri, displayName;

        int size;

        File baseDir, contentDir;

        Asset(String assetSpec) {
            String[] parts = assetSpec
                    .split(JnlpClientConstants.PROCESS_ASSET_DELIM);
            packageId = parts[0];
            packageVersion = parts[1];
            uri = parts[2];
            size = Integer.parseInt(parts[3]);
            displayName = parts[4];

            String baseDirName = makeFilenamePart(packageId) + "-" //
                    + makeFilenamePart(packageVersion) + "-" //
                    + makeFilenamePart(uri.substring(uri.length() - 8));
            baseDir = new File(distribDir, baseDirName);
            contentDir = findPreviouslyDownloadedContent();
        }

        private String makeFilenamePart(String s) {
            String result = FileUtils.makeSafeIdentifier(s);
            // comma characters are common in package IDs, and are safe
            result = StringUtils.findAndReplace(result, "%2C", ",");
            result = StringUtils.findAndReplace(result, "%2c", ",");
            return result;
        }

        /** Check to see if this asset has been downloaded in the past */
        private File findPreviouslyDownloadedContent() {
            // look under the target directory to find the randomly named
            // subdirectory which contains our content. We expect to find
            // exactly one subdir containing exactly one file, but we will
            // search through multiple files/dirs if they are present.
            for (File oneSubdir : listFiles(baseDir)) {
                for (File oneFile : listFiles(oneSubdir)) {
                    if (oneFile.length() == size)
                        return oneSubdir;
                }
            }

            // no previously downloaded content was found.
            return null;
        }

        private File[] listFiles(File dir) {
            File[] result = dir.listFiles();
            if (result == null)
                result = new File[0];
            return result;
        }

        private void downloadContent(String downloadBaseUrl)
                throws IOException {
            // if this file is already locally downloaded and cached, exit
            if (contentDir != null || size < 1)
                return;

            // display a message to the user that we are downloading this item
            splashScreen.setActivity(
                res.format("Download_Asset_FMT", displayName));

            // create a temporary file to write the data to
            File tmpFile = File.createTempFile("pdashProcessAsset", ".tmp");
            tmpFile.deleteOnExit();

            // connect to the URL for this process asset
            int nonce = Math.abs(random.nextInt());
            URL url = new URL(downloadBaseUrl + uri + "?"
                    + JnlpClientConstants.DOWNLOAD_NONCE_PARAM + "=" + nonce);
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.connect();

            // download the asset and save in the temp file
            String targetFilename = getTargetFilename(conn, nonce);
            int xorByte = random.nextInt(200) + 50;
            byte[] digest = downloadFile(conn, tmpFile, xorByte);

            // ensure that the downloaded asset meets expectations
            verifyContent(tmpFile, digest);

            // copy the asset to the appropriate location
            copyToDestination(tmpFile, xorByte, targetFilename);
        }

        /** Get the filename that should be used for this asset */
        private String getTargetFilename(URLConnection conn, int nonce)
                throws IOException {
            // the data stream from the server will contain an extra header
            // with the name of the target file and signature data. Determine
            // the length of that header
            int headerLen = conn.getContentLength() - size;
            if (headerLen < 1)
                throw new IOException("PDES download header was missing");

            // read the header into a buffer.
            InputStream in = conn.getInputStream();
            byte[] headerData = new byte[headerLen];
            for (int pos = 0; pos < headerLen;) {
                int numRead = in.read(headerData, pos, headerLen - pos);
                if (numRead < 1)
                    throw new IOException(
                            "Could not read PDES download header");
                else
                    pos += numRead;
            }

            // find the first zero byte, and interpret the data that precedes it
            // as the target filename.
            String targetFilename = null;
            int signPos = -1;
            for (int pos = 0; pos < headerLen; pos++) {
                if (headerData[pos] == 0) {
                    targetFilename = new String(headerData, 0, pos, "UTF-8");
                    signPos = pos + 1;
                    break;
                }
            }
            if (targetFilename == null)
                throw new IOException(
                        "Target filename was missing from download header");

            // The remainder of the header is a digital signature. Validate it
            // to prove we are communicating with an Enterprise Server.
            try {
                if (signature == null) {
                    signature = Signature.getInstance("SHA1withDSA");
                    signature.initVerify(JarVerifier.getTrustedDownloadCert());
                }
                String stringToVerify = packageId + PROCESS_ASSET_DELIM //
                        + packageVersion + PROCESS_ASSET_DELIM //
                        + uri + PROCESS_ASSET_DELIM //
                        + targetFilename + PROCESS_ASSET_DELIM //
                        + nonce;
                signature.update(stringToVerify.getBytes("UTF-8"));
                // If the signature verifies, return the target filename
                if (signature.verify(headerData, signPos, headerLen - signPos))
                    return targetFilename;

            } catch (Exception e) {
                throw new IOException(
                        "Problem validating PDES download signature", e);
            }

            // If the validations did not pass, abort and throw an exception.
            in.close();
            throw new IOException("Invalid PDES download signature");
        }

        /** Download the asset and save in a temporary file */
        protected byte[] downloadFile(URLConnection conn, File tmpFile,
                int xorByte) throws IOException {

            // construct a digest object for validating file contents
            MessageDigest m;
            try {
                m = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Cannot calculate asset hash", e);
            }

            InputStream in = null;
            OutputStream out = null;
            try {
                // construct streams for downloading and saving the data
                in = new DigestInputStream(
                        new BufferedInputStream(conn.getInputStream()), m);
                out = new XorOutputStream(
                        new BufferedOutputStream(new FileOutputStream(tmpFile)),
                        xorByte);

                // download the data and save in the temporary file
                byte[] buf = new byte[1024];
                int numRead;
                while ((numRead = in.read(buf)) != -1) {
                    out.write(buf, 0, numRead);
                    splashScreen.addProgress(numRead);
                }

            } finally {
                // close the streams we were using
                FileUtils.safelyClose(in);
                FileUtils.safelyClose(out);
            }

            // return the MD5 digest of the downloaded file
            return m.digest();
        }

        /** Verify that the downloaded file meets our expectations */
        protected void verifyContent(File f, byte[] digest) throws IOException {
            // ensure that the file has the correct size
            if (f.length() != size)
                throw new IOException("File size doesn't match expectations");

            // ensure that the MD5 hash of the file matches
            int c = uri.indexOf('-') + 1;
            for (int i = 0; i < digest.length; i++, c += 2) {
                if (digest[i] != getHashByte(c))
                    throw new IOException("Content hashes do not match");
            }
        }

        private int getHashByte(int pos) {
            if (pos + 2 > uri.length())
                return -999;
            else
                return (byte) Integer.parseInt(uri.substring(pos, pos + 2), 16);
        }

        /**
         * Copy the downloaded file to a permanent location under the distrib
         * directory, and save that location into the contentDir field.
         */
        private void copyToDestination(File tmpFile, int xorByte,
                String targetFilename) throws IOException {
            String randomName = Integer.toString(Math.abs(random.nextInt()),
                Character.MAX_RADIX);

            File tmpDir = new File(distribDir, randomName);
            tmpDir.mkdirs();

            OutputStream out = new XorOutputStream(new BufferedOutputStream(
                    new FileOutputStream(new File(tmpDir, targetFilename))),
                    xorByte);
            FileUtils.copyFile(tmpFile, out);
            out.close();

            baseDir.mkdir();
            contentDir = new File(baseDir, randomName);
            FileUtils.renameFile(tmpDir, contentDir);

            DistributionManager.touchDirectory(baseDir);
        }

    }

    private static final Resources res = JnlpDatasetLauncher.res;

    private static final String DATA_BRIDGE_URI_PATTERN = "/DataBridge/";

    private static final String PROCESS_ASSET_URI = "/pub/ProcessAsset/";

    private static final int VERIFICATION_EFFORT = 5000000;

}
