// Copyright (C) 2018-2023 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.XMLUtils;

class JnlpFileRetriever {

    /**
     * Retrieves the JNLP file at the given URL, parses it, and returns the
     * resulting XML document.
     * 
     * If the server is unreachable, this will check for a previously cached
     * version of the file.
     * 
     * @param url
     *            the URL of the JNLP document
     * @return an XML document, never null
     * @throws IOException
     *             if the file could not be retrieved, or if it is not valid XML
     */
    static Element open(String url) throws IOException {
        // try to download the JNLP file for the given URL
        File jnlpFile = tryToDownload(url);

        // if the download was successful, parse it and save it in the cache
        if (jnlpFile != null)
            return parseXmlAndCacheFile(jnlpFile);

        // if the download failed, try retrieving a cached version of the file
        jnlpFile = getCacheFile(url, false);
        if (jnlpFile == null)
            throw new FileNotFoundException(url);

        // parse the file we found
        return parseXml(jnlpFile);
    }

    /**
     * Contacts the server to see if a newer version of the file is available;
     * then parses the result
     * 
     * @param jnlpFile
     *            a JNLP file that has been downloaded and saved on the
     *            filesystem
     * @return an XML document, never null
     * @throws IOException
     *             if the named file does not exist, or if it is not valid XML
     */
    static Element open(File jnlpFile) throws IOException {
        // check to make sure the file exists
        if (!jnlpFile.isFile())
            throw new FileNotFoundException(jnlpFile.getPath());

        // if we were given an old version of the JNLP file, try refreshing it
        jnlpFile = maybeRedownload(jnlpFile);

        // parse the file and save it in the cache
        return parseXmlAndCacheFile(jnlpFile);
    }

    private static File maybeRedownload(File jnlpFile) throws IOException {
        // see if the file in question was saved fairly recently; if so, there
        // is no need to refresh it. (This would occur if a file was downloaded
        // by a web browser and immediately opened with this class.)
        long fileAge = System.currentTimeMillis() - jnlpFile.lastModified();
        if (fileAge < RECENT_DOWNLOAD_AGE)
            return jnlpFile;

        // parse the XML and retrieve the canonical href from the root tag
        Element xml = parseXml(jnlpFile);
        String href = xml.getAttribute("href");
        if (!XMLUtils.hasValue(href))
            return jnlpFile;

        // attempt to download a new copy of the file. If that fails, use
        // the original file instead
        File downloadedFile = tryToDownload(href);
        return (downloadedFile == null ? jnlpFile : downloadedFile);
    }

    private static final int RECENT_DOWNLOAD_AGE = 60000; // one minute

    private static File tryToDownload(String jnlpHref) {
        try {
            URL url = new URL(jnlpHref);
            URLConnection conn = HTTPUtils.getURLFollowRedirects(url);
            InputStream in = conn.getInputStream();
            File out = TempFileFactory.get().createTempFile("jnlp", ".tmp");
            out.deleteOnExit();
            FileUtils.copyFile(in, out);
            in.close();
            return out;
        } catch (IOException ioe) {
            return null;
        }
    }

    private static Element parseXmlAndCacheFile(File f) throws IOException {
        Element xml = parseXml(f);

        // if the XML file doesn't include a <shortcut> tag, don't cache it
        NodeList nl = xml.getElementsByTagName("shortcut");
        if (nl == null || nl.getLength() < 1)
            return xml;

        // get the URL of the JNLP file, and find the associated cache file
        String jnlpHref = xml.getAttribute("href");
        File cacheFile = getCacheFile(jnlpHref, true);

        // keep the existing cache file if it exists and is more recent than the
        // incoming file
        if (cacheFile.isFile() && cacheFile.lastModified() > f.lastModified())
            return xml;

        // copy the file into the cache
        try {
            FileUtils.copyFile(f, cacheFile);
        } catch (IOException ioe) {
            cacheFile.delete();
        }

        // return the XML data
        return xml;
    }

    private static Element parseXml(File xmlFile) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(new FileInputStream(xmlFile)).getDocumentElement();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new InvalidXmlException(xmlFile, e);
        }
    }

    public static class InvalidXmlException extends IOException {
        public InvalidXmlException(File f, Exception e) {
            super("Invalid XML file " + f.getPath(), e);
        }
    }

    private static File getCacheFile(String jnlpUrl, boolean create) {
        // compute the data bridge URL corresponding to this JNLP URL
        String dataUrl = getDataBridgeUrl(jnlpUrl);
        if (dataUrl == null)
            return null;

        // compute the path of the file where the JNLP file should be cached
        File cacheDir = DirectoryPreferences.getLocalCacheDir(dataUrl);
        File metadataDir = new File(cacheDir, "metadata");
        File cacheFile = new File(metadataDir, "launchData.txt");

        // if the file exists, return it
        if (cacheFile.isFile())
            return cacheFile;

        // If our caller isn't interested in creating the file, abort
        if (create == false)
            return null;

        // create the metadata directory if needed, and return the cache file
        if (!metadataDir.isDirectory())
            metadataDir.mkdirs();
        return cacheFile;
    }

    /**
     * Compute the data bridge URL corresponding to a given JNLP URL
     */
    static String getDataBridgeUrl(String jnlpUrl) {
        Matcher m = DATASET_JNLP_URL_PAT.matcher(jnlpUrl);
        if (m.find())
            return m.group(1) + "/DataBridge/" + m.group(2);
        else
            return null;
    }

    private static final Pattern DATASET_JNLP_URL_PAT = Pattern
            .compile("(http.+)/pub/LaunchDataset/.*(INST-[a-z0-9]+)_?");

}
